import logging
import unittest

import pytest

from zmlpcd.daemon import ZmlpContainerDaemon
from zmlpcd.reactor import Reactor
from zmlpsdk.testing import TestEventEmitter

logging.basicConfig(level=logging.DEBUG)

TEST_IMAGE = "zmlp/plugins-base:latest"


class ZmlpContainerDaemonTests(unittest.TestCase):

    def tearDown(self):
        self.zpsd.stop()

    def setUp(self):
        self.emitter = TestEventEmitter()
        self.zpsd = ZmlpContainerDaemon(9999, Reactor(self.emitter))

    def test_event_handler_generate(self):
        event = {
            "type": "generate",
            "payload": {
                "ref": {
                    "className": "zmlpsdk.testing.TestGenerator",
                    "image": TEST_IMAGE,
                    "args": {
                        "files": [
                            "/test-data/images/set01/toucan.jpg",
                            "/test-data/images/set01/faces.jpg"
                        ]
                    }
                }
            }
        }
        # Run twice
        self.zpsd.handle_event(event)
        assert self.emitter.event_count("error") == 0
        assert self.emitter.event_count("expand") == 1

    def test_event_handler_execute(self):
        event = {
            "type": "execute",
            "payload": {
                "ref": {
                    "className": "zmlpsdk.testing.TestProcessor",
                    "image": TEST_IMAGE,
                    "args": {},
                    "checksum": -1
                },
                "assets": [
                    {
                        "id": "1234",
                        "document": {
                            "kirk": "spock"
                        }
                    }
                ]
            }
        }
        # Run twice
        self.zpsd.handle_event(event)
        self.zpsd.handle_event(event)
        assert self.emitter.event_count("asset") == 2
        assert self.emitter.event_count("error") == 0

    def test_event_handler_failure(self):
        event = {
            "type": "execute",
            "payload": {
                "ref": {
                    "className": "foo.DoesNotExist",
                    "image": TEST_IMAGE,
                    "args": {}
                },
                "assets": [
                    {
                        "id": "abc123"
                    }
                ]
            }
        }
        self.zpsd.handle_event(event)
        assert self.emitter.event_count("error") == 1

    def test_handle_ready(self):
        event = {
            "type": "ready",
            "payload": {}
        }
        self.zpsd.handle_event(event)
        assert self.emitter.event_count("ok") == 1

    def test_handle_teardown_warning(self):
        event = {
            "type": "teardown",
            "payload": {}
        }
        self.zpsd.handle_event(event)
        assert self.emitter.event_count("warning") == 1

    def test_handle_teardown(self):
        ref = {
            "className": "zmlpsdk.testing.TestProcessor",
            "image": TEST_IMAGE,
            "args": {}
        }

        event = {
            "type": "execute",
            "payload": {
                "ref": ref,
                "assets": [{
                    "id": "1234",
                    "document": {
                        "kirk": "spock"
                    }
                }]
            }
        }
        self.zpsd.handle_event(event)

        event = {
            "type": "teardown",
            "payload": {
                "ref": ref
            }
        }
        self.zpsd.handle_event(event)
        assert self.emitter.event_count("warning") == 0

    def test_handle_stop(self):
        event = {
            "type": "stop",
            "payload": {}
        }
        with pytest.raises(SystemExit):
            self.zpsd.handle_event(event)
