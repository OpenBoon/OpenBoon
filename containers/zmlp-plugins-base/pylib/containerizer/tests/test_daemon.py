import logging
import time
import unittest

import zmq

from containerizer.daemon import PixmlContainerDaemon
from pixml.analysis import Reactor
from pixml.analysis.testing import TestEventEmitter

logging.basicConfig(level=logging.DEBUG)


class PixmlContainerDaemonTests(unittest.TestCase):

    def tearDown(self):
        self.zpsd.stop()

    def setUp(self):
        self.emitter = TestEventEmitter()
        self.zpsd = PixmlContainerDaemon(9999, Reactor(self.emitter))

    def test_event_handler_generate(self):
        event = {
            "type": "generate",
            "payload": {
                "ref": {
                    "className": "pixml.analysis.testing.TestGenerator",
                    "image": "zmlp-plugins-base",
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
                    "className": "pixml.analysis.testing.TestProcessor",
                    "image": "zmlp-plugins-base",
                    "args": {

                    }
                },
                "asset": {
                    "id": "1234",
                    "document": {
                        "kirk": "spock"
                    }
                }
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
                    "image": "zmlp-plugins-base",
                    "args": {}
                },
                "asset": {
                    "id": "abc123"
                }
            }
        }
        self.zpsd.handle_event(event)
        assert self.emitter.event_count("error") == 1
