import logging
import tempfile
import unittest

from analyst.containerized import ContainerizedZpsExecutor
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
                "CAT" : "DOG"
            },
            "script": {
                "generate": [
                    {
                        "className": "pixml.analysis.testing.TestGenerator",
                        "args": {
                            "files": ["/test-data/images/set01/toucan.jpg"]
                        },
                        "image": "zmlp-plugins-base"
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
