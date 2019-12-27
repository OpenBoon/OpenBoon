import logging
import os
import tempfile
import threading
import time
import unittest

from analyst.executor import ZpsExecutor, DockerContainerWrapper
from .test_service import test_task

logging.basicConfig(level=logging.DEBUG)


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
                "generate": [
                    {
                        "className": "zmlp.analysis.testing.TestGenerator",
                        "args": {
                            "files": ["/test-data/images/set01/toucan.jpg"]
                        },
                        "image": "zmlp/plugins-base:latest"
                    }
                ]
            }
        }

    def test_kill(self):
        task = test_task(sleep=30)

        wrapper = ZpsExecutor(task, self.client)

        def threaded_function():
            time.sleep(8)
            wrapper.kill(task["id"], "skipped", "manual kill")

        thread = threading.Thread(target=threaded_function)
        thread.start()
        wrapper.run()
        thread.join()

    def test_process(self):
        task = test_task()
        wrapper = ZpsExecutor(task, self.client)
        wrapper.run()

        assert self.client.event_count("started") == 1
        assert self.client.event_count("stopped") == 1
        assert self.client.event_count("expand") == 0
        assert self.client.event_count("index") == 1
        assert self.client.event_count("stats") == 1

    def test_process_invalid_processor(self):
        task = test_task()
        task["script"]["execute"][0]["className"] = "FOO.analysis.testing.FOOProcessor"

        wrapper = ZpsExecutor(task, self.client)
        result = wrapper.run()
        assert result["hardfailure_events"] == 1
        assert result["error_events"] == 1
        assert result["exit_status"] == 2

    def test_generate(self):
        wrapper = ZpsExecutor(self.gen_task, self.client)
        wrapper.run()

        assert self.client.event_count("started") == 1
        assert self.client.event_count("stopped") == 1
        assert self.client.event_count("expand") == 1
        assert self.client.event_count("stats") == 1
        assert self.client.event_count("index") == 0

    def test_generate_invalid_processor(self):
        self.gen_task["script"]["generate"][0]["className"] = "foo.analysis.testing.FOO"
        wrapper = ZpsExecutor(self.gen_task, self.client)
        result = wrapper.run()

        assert result.get("hardfailure_events") == 1
        assert result.get("exit_status") == 1


class TestDockerContainerWrapper(unittest.TestCase):

    def setUp(self):
        self.client = MockClusterClient()
        task = test_task()
        wrapper = ZpsExecutor(task, self.client)
        self.container = DockerContainerWrapper(wrapper, task, "zmlp/plugins-base")

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

    def test_docker_pull(self):
        image = self.container._pull_image()
        assert "zmlp/plugins-base:development" == image

    def test_docker_pull_no_repo(self):
        os.environ["ANALYST_DOCKER_PULL"] = "false"
        try:
            image = self.container._pull_image()
            assert "zmlp/plugins-base" == image
        finally:
            del os.environ["ANALYST_DOCKER_PULL"]

    def test_receive_event_with_timeout(self):
        event = self.container.receive_event(250)
        assert event["type"] == "timeout"
        assert event["payload"]["timeout"] == 250
