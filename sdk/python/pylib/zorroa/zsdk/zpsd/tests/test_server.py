import zmq
import unittest
import logging


from zorroa.zsdk.zpsd.server import ZpsdServer
from zorroa.zsdk.testing import TestEventEmitter
from zorroa.zsdk.processor import Reactor

logging.basicConfig(level=logging.DEBUG)


class ZpsdServerTests(unittest.TestCase):

    def tearDown(self):
        self.zpsd.stop()

    def setUp(self):
        self.zpsd = ZpsdServer(9999, Reactor(TestEventEmitter()))

    def test_event_handler_process(self):
        event = {
            "type": "execute",
            "payload": {
                "ref": {
                    "className": "zorroa.zsdk.zpsd.tests.processors.TestSetAttrProcessor",
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

    def test_event_handler_failure(self):
        event = {
            "type": "execute",
            "payload": {
                "ref": {
                    "className": "foo.DoesNotExist",
                    "image": "plugins-py3-base",
                    "args": { }
                },
                "object": {}
            }
        }
        self.zpsd.handle_event(event)

    def test_receive_event(self):
        context = zmq.Context()
        socket = context.socket(zmq.PAIR)
        socket.connect("tcp://localhost:9999")

        event = {
            "type": "execute",
            "payload": {
                "ref": {
                    "className": "zorroa.zsdk.zpsd.tests.processors.TestSetAttrProcessor",
                    "image": "plugins-py3-base",
                    "args": {

                    }
                },
                "asset": { }
            }
        }
        socket.send_json(event)
        packet = self.zpsd.socket.recv_json()
        assert packet == event

