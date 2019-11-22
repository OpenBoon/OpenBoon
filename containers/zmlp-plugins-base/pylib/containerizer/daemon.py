import logging
import sys

import zmq

from containerizer.process import ProcessorExecutor
from pixml.analysis import Reactor

logger = logging.getLogger(__name__)


class PixmlContainerDaemon(object):
    def __init__(self, host, reactor=None):
        self.host = host
        self.socket = self.__setup_zmq_socket()
        self.reactor = reactor

        if not reactor:
            self.reactor = Reactor(ZmqEventEmitter(self.socket))
        self.executor = ProcessorExecutor(self.reactor)

    def __setup_zmq_socket(self):
        ctx = zmq.Context()
        socket = ctx.socket(zmq.PAIR)
        if self.host:
            logger.info("Connecting to {}".format(self.host))
            socket.connect(self.host)
            socket.send_json({"type": "ready", "payload": {}})
        else:
            logger.warning("No Analyst host specified, not connecting...")
        return socket

    def start(self):
        while True:
            logger.info("Waiting for events")
            event = self.socket.recv_json()
            try:
                self.handle_event(event)
            except Exception as e:
                logger.warning("Failed to handle event '{}', {}".format(event, e))
                self.socket.send_json({"type": "hardfailure", "payload": {"message": str(e)}})
                break

    def stop(self):
        self.socket.close()

    def handle_event(self, event):
        logger.info("handling event: %s" % event)
        etype = event["type"]
        if etype == "execute":
            self.executor.execute_processor(event["payload"])
        elif etype == "generate":
            self.executor.execute_generator(event["payload"])
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
