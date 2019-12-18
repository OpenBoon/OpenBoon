import logging
import tempfile
import unittest

from analyst.containerized import ContainerizedZpsExecutor, DockerContainerProcess
from .test_cmpts import test_task, MockArchivistClient

logging.basicConfig(level=logging.DEBUG)


class TestContainerizedZpsExecutor(unittest.TestCase):

    def setUp(self):
        self.client = MockArchivistClient()

    def test_run(self):
        task = test_task()

        wrapper = ContainerizedZpsExecutor(task, self.client)
        wrapper.run()

        assert self.client.event_count("started") == 1
        assert self.client.event_count("stopped") == 1
        assert self.client.event_count("expand") == 0
        assert self.client.event_count("index") == 1
        assert self.client.event_count("stats") == 1

    def test_generate(self):
        task = {
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
                        "className": "pixml.analysis.testing.TestGenerator",
                        "args": {
                            "files": ["/test-data/images/set01/toucan.jpg"]
                        },
                        "image": "zmlp/plugins-base"
                    }
                ]
            }
        }

        wrapper = ContainerizedZpsExecutor(task, self.client)
        wrapper.run()

        assert self.client.event_count("started") == 1
        assert self.client.event_count("stopped") == 1
        assert self.client.event_count("expand") == 1
        assert self.client.event_count("stats") == 1
        assert self.client.event_count("index") == 0


class TestDockerContainerProcess(unittest.TestCase):

    def setUp(self):
        self.client = MockArchivistClient()
        task = test_task()
        wrapper = ContainerizedZpsExecutor(task, self.client)
        self.container = DockerContainerProcess(wrapper, task, "zmlp/plugins-base")

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
