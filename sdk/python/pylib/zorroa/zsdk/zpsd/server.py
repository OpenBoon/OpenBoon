import logging
import zmq
import sys

from zorroa.zsdk.processor import Reactor
from zorroa.zsdk.zps.process import ProcessorExecutor

logger = logging.getLogger(__name__)


class ZpsdServer(object):
    def __init__(self, port, reactor=None):
        self.port = port
        self.socket = self.__setup_zmq_socket()
        self.reactor = reactor

        if not reactor:
            self.reactor = Reactor(ZmqEventEmitter(self.socket))
        self.executor = ProcessorExecutor(reactor)

    def __setup_zmq_socket(self):
        ctx = zmq.Context()
        socket = ctx.socket(zmq.PAIR)
        socket.bind("tcp://*:{}".format(self.port))
        return socket

    def start(self):
        logging.info("Starting ZPSD on port {}".format(self.port))
        while True:
            event = self.socket.recv_json()
            try:
                self.handle_event(event)
            except Exception as e:
                logger.warning("Failed to handle event '{}', {}".format(event, e))
                self.socket.send_json({"type": "hardfailure", "payload": {"message": str(e)}})

    def stop(self):
        self.socket.close()

    def handle_event(self, event):
        etype = event["type"]
        if etype == "execute":
            obj = self.executor.execute_processor(event["payload"])
            self.reactor.emitter.write({"type": "object", "payload": obj})
        elif etype == "teardown":
            self.executor.teardown_processor(event["payload"])
        elif etype == "stop":
            logger.info("Exiting ZPSD via stop event")
            sys.exit(event["payload"].get("status", 0))


class ZmqEventEmitter(object):
    def __init__(self, socket):
        self.socket = socket

    def write(self, event):
        self.socket.send_json(event)


