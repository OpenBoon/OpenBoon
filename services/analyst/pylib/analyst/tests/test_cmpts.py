import json
import logging
import os
import socket
import tempfile
import threading
import time
import unittest
import uuid
from threading import Lock


from mock import patch, MagicMock
from requests import Response

from analyst import main
from analyst.components import ClusterClient, \
    get_sdk_version, Executor
from analyst.containerized import ContainerizedZpsExecutor
from analyst.main import setup_routes

logging.basicConfig(level=logging.INFO)

logger = logging.getLogger(__name__)

def read_build_version_file():
    with open(os.environ["ZORROA_BUILD_FILE"]) as fp:
        expected = fp.read().strip()
    return expected


def test_task(event_type=None, attrs=None):
    this_dir = os.path.dirname(__file__)
    task = {
        "id": "71C54046-6452-4669-BD71-719E9D5C2BBF",
        "jobId": "71C54046-6452-4669-BD71-719E9D5C2BBF",
        "name": "process_me",
        "state": 1,
        "logFile": "file:///%s" % tempfile.mktemp("logfile"),
        "script": {
            "assets": [
                {
                    "id": "71C54046-6452-4669-BD71-719E9D5C2BBF",
                    "document": {"foo": "bar"}
                }
            ],
            "execute": [
                {
                    "className": "pixml.testing.TestProcessor",
                    "args": {
                        "send_event": event_type,
                        "attrs": attrs
                    },
                    "image": "zmlp-plugins-base"
                }
            ]
        },
        "args": {"arg1": "arg_value"}
    }
    return task


class MockArchivistClient:
    def __init__(self):
        self.lock = Lock()
        self.pings = []
        self.events = []
        self.remote_url = "https://127.0.0.1:8066"

    def ping(self, ping):
        self.pings.append(ping)

    def get_next_task(self):
        return test_task()

    def emit_event(self, task, etype, payload):
        logger.info("ANALYST EVENT: {} {}".format(etype, payload))
        self.events.append((etype, task, payload))

    def event_count(self, event_type):
        return len(self.get_events(event_type))

    def get_events(self, event_type):
        return [e for e in self.events if e[0] == event_type]

    def event_types(self):
        return {e[0] for e in self.events}


class ApiComponents(object):
    def __init__(self, ping_timer=0, poll_timer=0):
        self.client = MockArchivistClient()
        self.executor = Executor(self.client, ping_timer, poll_timer)


class TestClusterClient(unittest.TestCase):
    def setUp(self):
        self.client = ClusterClient("http://localhost:8080", 5000)

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

    def test_check_headers(self):
        header = self.client.headers
        assert header["Content-Type"] == "application/json"
        assert header["X-Analyst-Port"] == "5000"
        assert header["X-Analyst-Host"] == socket.gethostname()


class ApiUnitTestCases(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        setup_routes(None)
        cls.test_client = main.app.test_client()

    def setUp(self):
        os.environ["ZORROA_CORE_PLUGIN_PATH"] = os.path.dirname(__file__) + "/../../zplugins"
        os.environ["ZORROA_BUILD_FILE"] = os.path.dirname(__file__) + "/BUILD"

    def test_root(self):
        response = self.test_client.get('/')
        assert response.data == b'Zorroa Analyst'

    def test_info(self):
        response = self.test_client.get('/info')
        assert read_build_version_file() == response.json['version']


class TestFunctions(unittest.TestCase):
    def setUp(self):
        os.environ["ZORROA_BUILD_FILE"] = os.path.dirname(__file__) + "/BUILD"

    def test_read_sdk_version(self):
        expected = read_build_version_file()
        assert get_sdk_version() == expected


class TestExecutor(unittest.TestCase):
    def setUp(self):
        self.api = ApiComponents()
        self.api.client = MockArchivistClient()

    def test_send_ping(self):
        api = self.api
        ping = api.executor.send_ping()
        assert("freeRamMb" in ping)
        assert("totalRamMb" in ping)
        assert("freeDiskMb" in ping)
        assert("load" in ping)
        assert("taskId" not in ping)

        api.executor.current_task = ContainerizedZpsExecutor({
            "id": "71C54046-6452-4669-BD71-719E9D5C2BBF",
            "jobId": "71C54046-6452-4669-BD71-719E9D5C2BBF",
            "organizationId": "71C54046-6452-4669-BD71-719E9D5C2BBF",
            "script": {}
        }, api.client)

        ping = api.executor.send_ping()
        assert("taskId" in ping)

    def test_emit_error(self):
        api = self.api
        result = api.executor.run_task(test_task("error"))
        assert(result["exit_status"] == 0)
        assert(result["error_events"] == 1)
        assert(api.executor.current_task is None)

    def test_emit_expand(self):
        api = self.api
        result = api.executor.run_task(test_task("expand"))
        assert(result["exit_status"] == 0)
        assert(result["expand_events"] == 1)
        assert(api.executor.current_task is None)

    def test_queue_next_task(self):
        api = self.api
        assert(api.executor.queue_next_task())

    def test_kill_no_task(self):
        api = self.api
        assert(api.executor.kill_task("ABC123", None, "test kill") is False)

    def ignore_kill_sleep_task(self):
        api = self.api
        arg = test_task("--sleep 60")
        thread = threading.Thread(target=api.executor.run_task, args=(arg,))
        thread.daemon = True
        thread.start()
        while True:
            time.sleep(1)
            if api.executor.current_task is not None:
                if api.executor.current_task.pid != -1:
                    killed = api.executor.kill_task("71C54046-6452-4669-BD71-719E9D5C2BBF",
                                                    "skipped", "test kill")
                    break
        thread.join(5)
        time.sleep(2)
        assert killed
        final_event = api.executor.client.events[-1][2]
        print(final_event)
        assert final_event["manualKill"]
        assert final_event["exitStatus"] == -9
        assert final_event["newState"] == "skipped"


class TestContainerizedZpsExecutor(unittest.TestCase):

    def test_run(self):
        task = test_task("--expand")

        temp_dir = tempfile.mkdtemp("zorroa_temp")
        script = os.path.join(temp_dir, "script.zps")

        with open(script, "w") as fp:
            fp.write(json.dumps(task["script"]))

        wrapper = ContainerizedZpsExecutor(test_task("--sleep 1"), MockArchivistClient())
        wrapper.run()

