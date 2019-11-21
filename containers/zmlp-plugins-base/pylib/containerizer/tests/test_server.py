import logging
import unittest

import zmq

from pixml.processor import Reactor
from pixml.testing import TestEventEmitter
from containerizer.server import PixmlContainerDaemon

logging.basicConfig(level=logging.DEBUG)


class ZpsdServerTests(unittest.TestCase):

    def tearDown(self):
        self.zpsd.stop()

    def setUp(self):
        self.emitter = TestEventEmitter()
        self.zpsd = PixmlContainerDaemon(Reactor(self.emitter))

    def test_event_handler_generate(self):
        event = {
            "type": "generate",
            "payload": {
                "ref": {
                    "className": "zorroa.zsdk.testing.TestGenerator",
                    "image": "plugins-py3-base",
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
                    "className": "zorroa.zsdk.testing.TestProcessor",
                    "image": "plugins-py3-base",
                    "args": {

                    }
                },
                "object": {
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
        assert self.emitter.event_count("object") == 2
        assert self.emitter.event_count("error") == 0

    def test_event_handler_failure(self):
        event = {
            "type": "execute",
            "payload": {
                "ref": {
                    "className": "foo.DoesNotExist",
                    "image": "plugins-py3-base",
                    "args": {}
                },
                "object": {}
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
                    "className": "zorroa.zsdk.testing.TestProcessor",
                    "image": "plugins-py3-base",
                    "args": {

                    }
                },
                "asset": {}
            }
        }
        socket.send_json(event)
        packet = self.zpsd.socket.recv_json()
        assert packet == event
