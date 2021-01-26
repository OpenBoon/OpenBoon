import collections
import logging
import os
import tempfile
import threading
import time
import unittest
import uuid
from unittest.mock import patch, MagicMock

from requests import Response

from analyst import main
from analyst.executor import ZpsExecutor
from analyst.main import setup_routes
from analyst.service import ClusterClient, get_sdk_version, ServiceComponents

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

os.environ["ANALYST_DOCKER_PULL"] = "false"


def test_task(event_type=None, attrs=None, sleep=1):
    task = {
        "id": "71C54046-6452-4669-BD71-719E9D5C2BBF",
        "taskId": "71C54046-6452-4669-BD71-719E9D5C2BBF",
        "jobId": "71C54046-6452-4669-BD71-719E9D5C2BBF",
        "projectId": "81C54046-6452-4669-BD71-719E9D5C2BBF",
        "name": "process_me",
        "state": 1,
        "logFile": "file:///%s" % tempfile.mktemp("logfile"),
        "script": {
            "assets": [
                {
                    "id": "71C54046-6452-4669-BD71-719E9D5C2BBF",
                    "document": {
                        "foo": "bar",
                        "source": {
                            "path": "/something/something/bilbo.jpg"
                        }
                    }
                }
            ],
            "execute": [
                {
                    "className": "zmlpsdk.testing.TestProcessor",
                    "args": {
                        "send_event": event_type,
                        "attrs": attrs,
                        "sleep": sleep
                    },
                    "image": "zmlp/plugins-base:latest"
                }
            ]
        },
        "args": {"arg1": "arg_value"}
    }
    return task


class TestClusterClient(unittest.TestCase):
    def setUp(self):
        self.client = ClusterClient("http://localhost:8080", "12345", 5000)

    @patch("requests.post")
    def test_send_ping(self, mock_post):
        ping = {
            "freeRamMb": 1000,
            "totalRamMb": 5000,
            "load": 0.1,
            "state": "Up",
            "taskId": None,
            "version": "0.1"
        }

        mock_post.return_value = type('', (dict,), {'json': lambda it: it})(ping)
        result = self.client.ping(ping)

        self.assertEquals(0.1, result["load"])
        self.assertEquals(1000, result["freeRamMb"])
        self.assertEquals(5000, result["totalRamMb"])
        self.assertEquals("Up", result["state"])

    def test_get_next_task(self):
        assert not self.client.get_next_task()

    @patch("requests.post")
    def test_emit_event(self, mock_post):
        mock_post.return_value = MagicMock(spec=Response, status_code=404)

        status = self.client.emit_event(
            {"id": str(uuid.uuid4()), "jobId": str(uuid.uuid4())},
            "message", {"message": "burp!"})

        # The task and job are random so this event will return a 404
        self.assertEquals(404, status)
        mock_post.assert_called_once()
        arg, kwargs = mock_post.call_args
        self.assertEqual("burp!", kwargs["json"]["payload"]["message"])

    def test_headers(self):
        header = self.client._headers()
        assert header["Content-Type"] == "application/json"
        assert header["Authorization"].startswith("Bearer")


class EndpointUnitTestCases(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        setup_routes(None)
        cls.test_client = main.app.test_client()

    def test_root(self):
        response = self.test_client.get('/')
        assert response.data == b'Zorroa Analyst'

    def test_info(self):
        response = self.test_client.get('/info')
        assert get_sdk_version() == response.json['version']


class TestExecutor(unittest.TestCase):
    def setUp(self):
        os.environ["ANALYST_DOCKER_PULL"] = "false"
        creds_file = os.path.join(os.path.dirname(__file__), "creds.txt")
        ArgTuple = collections.namedtuple('ArgTuple', 'credentials archivist ping poll grace port')
        args = ArgTuple(credentials=creds_file, archivist="https://localhost:8080",
                        ping=0, poll=0, grace=0, port=5000)

        self.api = ServiceComponents(args)

    @patch("requests.post")
    def test_send_ping(self, port_patch):
        api = self.api
        ping = api.executor.send_ping()
        assert ("freeRamMb" in ping)
        assert ("totalRamMb" in ping)
        assert ("freeDiskMb" in ping)
        assert ("load" in ping)
        assert ("taskId" not in ping)

        api.executor.current_task = ZpsExecutor({
            "id": "71C54046-6452-4669-BD71-719E9D5C2BBF",
            "jobId": "71C54046-6452-4669-BD71-719E9D5C2BBF",
            "organizationId": "71C54046-6452-4669-BD71-719E9D5C2BBF",
            "script": {}
        }, api.client)

        ping = api.executor.send_ping()
        assert ("taskId" in ping)

    @patch.object(ClusterClient, "emit_event")
    def test_emit_error(self, event_patch):
        event_patch.return_value = {}
        api = self.api
        result = api.executor.run_task(test_task("error"))
        assert (result["exit_status"] == 8)
        assert (result["error_events"] == 1)
        assert (api.executor.current_task is None)

    @patch("requests.post")
    def test_emit_expand(self, post_patch):
        api = self.api
        result = api.executor.run_task(test_task("expand"))
        assert (result["exit_status"] == 0)
        assert (result["expand_events"] == 1)
        assert (api.executor.current_task is None)

    @patch.object(ClusterClient, "get_next_task")
    def test_queue_next_task(self, put_patch):
        put_patch.return_value = test_task()
        api = self.api
        assert (api.executor.queue_next_task())

    @patch("requests.post")
    def test_kill_no_task(self, post_patch):
        api = self.api
        assert (api.executor.kill_task("ABC123", None, "test kill") is False)

    @patch("requests.post")
    def test_kill_sleep_task(self, post_patch):
        api = self.api
        arg = test_task(sleep=20)
        thread = threading.Thread(target=api.executor.run_task, args=(arg,))
        thread.daemon = True
        thread.start()

        while True:
            time.sleep(10)
            if api.executor.current_task is not None:
                logger.info("killing")
                killed = api.executor.kill_task("71C54046-6452-4669-BD71-719E9D5C2BBF",
                                                "skipped", "test kill")
                print(killed)
                if killed:
                    break
        thread.join(5)
        time.sleep(2)

    @patch("requests.post")
    @patch("requests.get")
    def test_kill_not_running_task(self, post_patch, get_patch):
        api = self.api
        arg = test_task(sleep=20)
        thread = threading.Thread(target=api.executor.run_task, args=(arg,))
        thread.daemon = True
        thread.start()

        task_id = "71C54046-6452-4669-BD71-719E9D5C2BBF"

        time.sleep(5)
        resp_exist = api.executor.send_ping()
        assert resp_exist['taskId'] == task_id
        status = api.executor.check_task_status(task_id)

        time.sleep(5)
        resp_not_exist = api.executor.send_ping()

        assert "taskId" not in resp_not_exist
        assert status is True
        thread.join()

    @patch("requests.post")
    def test_model_cache_clear(self, post_patch):
        task = test_task(sleep=1)
        self.api.executor.run_task(task)
        assert self.api.executor.previous_task["id"] == task["id"]

        model_cache_dir = "/tmp/model-cache/{}".format(task["projectId"])
        os.makedirs(model_cache_dir, exist_ok=True)

        task2 = test_task(sleep=1)
        task2["projectId"] = "AAAA4046-6452-4669-BD71-719E9D5C2BBF"
        self.api.executor.run_task(task2)

        assert not os.path.exists(model_cache_dir)

    @patch("requests.post")
    def test_model_cache_keep(self, post_patch):
        task = test_task(sleep=1)
        model_cache_dir = "/tmp/model-cache/{}".format(task["projectId"])
        os.makedirs(model_cache_dir, exist_ok=True)

        self.api.executor.previous_task = task
        self.api.executor.run_task(task)

        assert os.path.exists(model_cache_dir)

    @patch("requests.put")
    def test_start_shutdown_true(self, post_patch):
        post_patch.return_value = MockResponse(200)
        assert self.api.executor.start_shutdown()

    @patch("requests.post")
    def test_start_shutdown_false(self, post_patch):
        post_patch.return_value = MockResponse(200)
        task = test_task(sleep=5)
        thread = threading.Thread(target=self.api.executor.run_task, args=(task,))
        thread.daemon = True
        thread.start()
        time.sleep(1)
        assert not self.api.executor.start_shutdown()


class MockResponse:
    def __init__(self, status):
        self.status_code = status
