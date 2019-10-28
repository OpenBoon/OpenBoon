import json
import logging
import os
import socket
import sys
import tempfile
import threading
import time
import unittest
import uuid
from threading import Lock

import archivist
from mock import patch, MagicMock
from pathlib2 import Path
from requests import Response

from analyst import main
from analyst.components import ClusterClient, ProcessorScanner, ZpsGoProcessWrapper, \
    get_sdk_version, Executor
from analyst.main import setup_routes

logging.basicConfig()


def read_build_version_file():
    with open(os.environ["ZORROA_BUILD_FILE"]) as fp:
        expected = fp.read().strip()
    return expected


def test_task(args=""):
    this_dir = os.path.dirname(__file__)
    task = {
        "id": "71C54046-6452-4669-BD71-719E9D5C2BBF",
        "jobId": "71C54046-6452-4669-BD71-719E9D5C2BBF",
        "organizationId": "71C54046-6452-4669-BD71-719E9D5C2BBF",
        "name": "process_me",
        "state": 1,
        "logFile": "file:///%s" % tempfile.mktemp("logfile"),
        "script": {
            "over": [
                {
                    "id": "71C54046-6452-4669-BD71-719E9D5C2BBF",
                    "document": {"foo": "bar"}
                }
            ],
            "execute": [
                {
                    "className": "zplugins.core.document.PyGroupProcessor",
                    "args": {}
                }
            ]
        },
        "env": {"FOO": "BAR",
                "ZORROA_ZPSGO_PATH": this_dir + "/faux_go.py",
                "ZORROA_ZPSGO_ARGS": args},
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

    def emit_event(self, etype, task, payload):
        self.lock.acquire()
        try:
            self.events.append((etype, task, payload))
        finally:
            self.lock.release()


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

        status = self.client.emit_event("message",
                                        {"id": str(uuid.uuid4()), "jobId": str(uuid.uuid4())},
                                        {"message": "burp!"})

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

    @patch.object(ProcessorScanner, 'download_remote_processors')
    @patch('subprocess32.check_output')
    def test_processors(self, check_output, download_processors):
        check_output.return_value = json.dumps(
            [{u'className': u'zplugins.asset.generators.AssetSearchGenerator'}])
        response = self.test_client.get('/processors')
        assert download_processors.call_count == 1
        assert response.json == [{u'className': u'zplugins.asset.generators.AssetSearchGenerator'}]

    def test_root(self):
        response = self.test_client.get('/')
        assert response.data == 'Zorroa Analyst'

    def test_info(self):
        response = self.test_client.get('/info')
        assert read_build_version_file() == response.json['version']


class TestProcessorScanner(unittest.TestCase):
    def setUp(self):
        os.environ["ZORROA_CORE_PLUGIN_PATH"] = os.path.dirname(__file__) + "/../../zplugins"

    def test_get_processors(self):
        # Setup some env vars usually setup by docker
        processors = ProcessorScanner().get_processors()
        assert len(processors) > 50

    @patch('subprocess32.check_call')
    def test_download_remote_processors(self, subprocess_patch):
        os.environ['GCS_EXT_PLUGIN_BUCKET'] = 'gs://some-bucket'
        ext_path = tempfile.mkdtemp()
        os.environ['ZORROA_GCS_PLUGIN_PATH'] = ext_path
        tmp_plugin_path = Path(str(Path(ext_path).parent),
                               '.zorroa_gcs_plugin_tmp/fake_module/fake.py')
        tmp_plugin_path.parent.mkdir(exist_ok=True, parents=True)
        tmp_plugin_path.touch()
        dest_plugin_path = Path(ext_path, 'fake_module/fake.py')
        dest_plugin_path.parent.mkdir(exist_ok=True, parents=True)
        if dest_plugin_path.exists():
            dest_plugin_path.unlink()
        assert not dest_plugin_path.exists()
        ProcessorScanner().download_remote_processors()
        gsutil_command = subprocess_patch.call_args_list[0][0][0]
        assert gsutil_command[:5] == ['gsutil', '-m', 'cp', '-r', 'gs://some-bucket']
        assert dest_plugin_path.exists()

    @patch.object(ProcessorScanner, 'download_remote_processors')
    @patch.object(ProcessorScanner, 'get_processors')
    def test_scan_processors(self, processors_patch, download_patch):
        ProcessorScanner().scan_processors()
        assert processors_patch.call_count == 1
        assert download_patch.call_count == 1


class TestFunctions(unittest.TestCase):
    def setUp(self):
        os.environ["ZORROA_BUILD_FILE"] = os.path.dirname(__file__) + "/BUILD"

    def test_read_sdk_version(self):
        expected = read_build_version_file()
        assert get_sdk_version() == expected


class TestExecutor(unittest.TestCase):
    def setUp(self):
        self.api = ApiComponents()

    def test_send_ping(self):
        api = self.api
        ping = api.executor.send_ping()
        assert("freeRamMb" in ping)
        assert("totalRamMb" in ping)
        assert("freeDiskMb" in ping)
        assert("load" in ping)
        assert("taskId" not in ping)

        api.executor.current_task = ZpsGoProcessWrapper({
            "id": "71C54046-6452-4669-BD71-719E9D5C2BBF",
            "jobId": "71C54046-6452-4669-BD71-719E9D5C2BBF",
            "organizationId": "71C54046-6452-4669-BD71-719E9D5C2BBF",
        }, api.client)

        ping = api.executor.send_ping()
        assert("taskId" in ping)

    def test_emit_error(self):
        api = self.api
        result = api.executor.run_task(test_task("--error"))
        assert(result["exit_status"] == 0)
        assert(result["error_events"] == 1)
        assert(api.executor.current_task is None)

    def test_emit_expand(self):
        api = self.api
        result = api.executor.run_task(test_task("--expand"))
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
                                                    archivist.TaskState.Skipped, "test kill")
                    break
        thread.join(5)
        time.sleep(2)
        assert killed
        final_event = api.executor.client.events[-1][2]
        print(final_event)
        assert final_event["manualKill"]
        assert final_event["exitStatus"] == -9
        assert final_event["newState"] == archivist.TaskState.Skipped


class TestZpsGoProcessWrapper(unittest.TestCase):
    def test_open_log_file(self):
        t = tempfile.mktemp("log_file")
        task = {"logFile": "file:%s" % t, "env": {"bilbo": "baggins"}}
        wrapper = ZpsGoProcessWrapper(task, None)
        fp = wrapper.open_task_log_file()
        fp.close()

        lines = open(task["logFile"].split("file:")[1], "r").read()
        assert "ENV: bilbo=baggins" in lines

    def test_close_log_file(self):
        t = tempfile.mktemp("log_file")
        task = {"logFile": "file:%s" % t, "env": {"bilbo": "baggins"}}
        wrapper = ZpsGoProcessWrapper(task, None)
        fp = wrapper.open_task_log_file()
        wrapper.close_task_log_file(fp)

        lines = open(task["logFile"].split("file:")[1], "r").read()
        assert "Exit Status: 1" in lines

    def test_launch_process(self):
        task = test_task("--expand")

        temp_dir = tempfile.mkdtemp("zorroa_temp")
        script = os.path.join(temp_dir, "script.zps")

        with open(script, "w") as fp:
            fp.write(json.dumps(task["script"]))

        wrapper = ZpsGoProcessWrapper(test_task("--sleep 1"), ApiComponents().client)
        log_file = wrapper.open_task_log_file()
        p = wrapper.launch_process(script, temp_dir, log_file)
        assert p.wait() == 0
