import json
import logging
import os
import tempfile
import threading
import time
import unittest

from analyst.executor import ZpsExecutor, DockerContainerWrapper
from .test_service import test_task

logging.basicConfig(level=logging.DEBUG)

os.environ["ANALYST_DOCKER_PULL"] = "false"


class MockClusterClient:
    """
    A pretend ClusterClient which simply counts events types.
    """

    def __init__(self):
        self.pings = []
        self.events = []
        self.remote_url = "https://127.0.0.1:8066"

    def emit_event(self, task, etype, payload):
        self.events.append((etype, task, payload))

    def event_count(self, event_type):
        return len(self.get_events(event_type))

    def get_events(self, event_type):
        return [e for e in self.events if e[0] == event_type]

    def event_types(self):
        return {e[0] for e in self.events}


class TestZpsExecutor(unittest.TestCase):

    def setUp(self):
        self.wrapper = None
        self.client = MockClusterClient()
        self.gen_task = {
            "id": "71C54046-6452-4669-BD71-719E9D5C2BBF",
            "jobId": "71C54046-6452-4669-BD71-719E9D5C2BBF",
            "name": "process_me",
            "logFile": "file:///%s" % tempfile.mktemp("logfile"),
            "env": {
                "CAT": "DOG"
            },
            "script": {
                "settings": {
                    "fileTypes": ["jpg"]
                },
                "generate": [
                    {
                        "className": "zmlpsdk.testing.TestGenerator",
                        "args": {
                            "files": ["/test-data/images/set01/toucan.jpg"]
                        },
                        "image": "zmlp/plugins-base:latest"
                    }
                ]
            }
        }

    def tearDown(self):
        if self.wrapper:
            self.wrapper.stop_container("finished test")

    def test_kill(self):
        task = test_task(sleep=30)

        self.wrapper = ZpsExecutor(task, self.client)

        def threaded_function():
            time.sleep(8)
            self.wrapper.kill(task["id"], "skipped", "manual kill")

        thread = threading.Thread(target=threaded_function)
        thread.start()
        self.wrapper.run()
        thread.join()

    def test_process(self):
        task = test_task()
        self.wrapper = ZpsExecutor(task, self.client)
        self.wrapper.run()

    def test_process_invalid_processor(self):
        task = test_task()
        task["script"]["execute"][0]["className"] = "FOO.analysis.testing.FOOProcessor"

        self.wrapper = ZpsExecutor(task, self.client)
        result = self.wrapper.run()
        assert result["hardfailure_events"] == 1
        assert result["error_events"] == 1
        assert result["exit_status"] == 9

    def test_generate(self):
        self.wrapper = ZpsExecutor(self.gen_task, self.client)
        self.wrapper.run()

        assert self.client.event_count("started") == 1
        assert self.client.event_count("stopped") == 1
        assert self.client.event_count("expand") == 1
        assert self.client.event_count("stats") == 1
        assert self.client.event_count("index") == 0

    def test_generate_invalid_processor(self):
        self.gen_task["script"]["generate"][0]["className"] = "foo.analysis.testing.FOO"
        self.wrapper = ZpsExecutor(self.gen_task, self.client)
        result = self.wrapper.run()

        assert result.get("hardfailure_events") == 1
        assert result.get("exit_status") == 9

    def test_get_exit_status(self):
        zexec = ZpsExecutor(self.gen_task, self.client)
        zexec.event_counts["hardfailure_events"] = 1
        assert 9 == zexec.get_exit_status()

        zexec = ZpsExecutor(self.gen_task, self.client)
        zexec.event_counts["error_events"] = 1
        assert 8 == zexec.get_exit_status()

        zexec = ZpsExecutor(self.gen_task, self.client)
        assert 0 == zexec.get_exit_status()


class TestDockerContainerWrapper(unittest.TestCase):

    def setUp(self):
        self.client = MockClusterClient()
        task = test_task()
        wrapper = ZpsExecutor(task, self.client)
        self.container = DockerContainerWrapper(
            wrapper, task, "zmlp/plugins-base:latest",
            os.path.realpath(tempfile.mkdtemp()))

    def tearDown(self):
        self.container.stop()

    def test_get_network_id(self):
        # Running locally this is false, running in CI/CD it's true
        self.container.get_network_id()

    def test_wait_for_container(self):
        self.container.wait_for_container()
        assert self.container.event_counts["ok_events"] == 1

    def test_log_event(self):
        event = {"type": "ok", "payload": {}}
        self.container.log_event(event)
        assert self.container.event_counts["ok_events"] == 1

    def test_docker_login(self):
        # Fake creds result in failure to logs
        test_creds = {"auths": {"https://index.docker.io/v1/": {
            "auth": "em9ycm9hYWRtaW46SFBYN2djUlo0Vm04VDRU",
            "email": "software@zorroa.com", "password": "12345",
            "username": "zorroaadmin"}}
        }

        tfp, tpath = tempfile.mkstemp(".json")
        with open(tpath, "w") as fp:
            json.dump(test_creds, fp)

        os.environ["ANALYST_DOCKER_CREDS_FILE"] = tpath
        try:
            self.container._docker_login()
        finally:
            del os.environ["ANALYST_DOCKER_CREDS_FILE"]

    def test_receive_event_with_timeout(self):
        event = self.container.receive_event(250)
        assert event["type"] == "timeout"
        assert event["payload"]["timeout"] == 250
