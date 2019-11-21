import logging
import time
import unittest

import zmq

from containerizer.daemon import PixmlContainerDaemon
from pixml.processor import Reactor
from pixml.testing import TestEventEmitter

logging.basicConfig(level=logging.DEBUG)


class PixmlContainerDaemonTests(unittest.TestCase):

    def tearDown(self):
        self.server.close()
        self.zpsd.stop()

    def setUp(self):
        self.emitter = TestEventEmitter()
        self.zpsd = PixmlContainerDaemon(None, Reactor(self.emitter))

        context = zmq.Context()
        self.server = context.socket(zmq.PAIR)
        self.server.bind("tcp://*:9999")

    def test_event_handler_generate(self):
        event = {
            "type": "generate",
            "payload": {
                "ref": {
                    "className": "pixml.testing.TestGenerator",
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
                    "className": "pixml.testing.TestProcessor",
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

    def test_receive_event(self):

        context = zmq.Context()
        socket = context.socket(zmq.PAIR)
        socket.connect("tcp://localhost:9999")

        event = {
            "type": "execute",
            "payload": {
                "ref": {
                    "className": "pixml.testing.TestProcessor",
                    "image": "zmlp-plugins-base",
                    "args": {}
                },
                "asset": {
                    "id": "123"
                }
            }
        }
        self.server.send_json(event)
        tries = 0
        while True:
            tries += 1
            poll = socket.poll(timeout=1000)
            if poll == 1:
                tries = -1
                break
            else:
                time.sleep(0.25)
        assert tries == -1
