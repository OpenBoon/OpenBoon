import json
import logging
import sys

import zmq

from containerizer.process import ProcessorExecutor
from pixml.analysis import Reactor
from pixml.rest import PixmlJsonEncoder

logger = logging.getLogger(__name__)


class PixmlContainerDaemon(object):
    """Starts a ZMQ server which allows us to run the processors contained
    in the image on asset metadata.
    """
    def __init__(self, port, reactor=None):
        self.port = port
        self.socket = self.__setup_zmq_socket(port)
        self.reactor = reactor

        if not reactor:
            self.reactor = Reactor(ZmqEventEmitter(self.socket))
        self.executor = ProcessorExecutor(self.reactor)

    def __setup_zmq_socket(self, port):
        ctx = zmq.Context()
        socket = ctx.socket(zmq.DEALER)
        socket.bind("tcp://*:%s" % port)
        return socket

    def start(self):
        logger.info("Analyst container server listening on port: %d" % self.port)
        while True:
            event = self.socket.recv_json()
            try:
                self.handle_event(event)
            except Exception as e:
                logger.exception("Failed to handle event '{}'".format(event))
                self.socket.send_json({"type": "hardfailure", "payload": {"message": str(e)}})
                break

    def stop(self):
        self.socket.close()

    def handle_event(self, event):
        etype = event["type"]
        logger.info("handling event: {}".format(etype))
        if etype == "ready":
            self.reactor.emitter.write({"type": "ok", "payload": {}})
        elif etype == "execute":
            self.executor.execute_processor(event["payload"])
        elif etype == "generate":
            self.executor.execute_generator(event["payload"])
        elif etype == "teardown":
            self.executor.teardown_processor(event["payload"])
        elif etype == "stop":
            logger.info("Exiting ZPSD via stop event")
            sys.exit(event["payload"].get("status", 0))


class ZmqEventEmitter(object):
    """
    An event emitter that emits to a ZMQ socket.
    """
    def __init__(self, socket):
        """
        Initialize a new ZmqEventEmitter.

        Args:
            socket (zmq.socket): A ZMQ socket.
        """
        self.socket = socket

    def write(self, event):
        """
        Write the given event payload to ZMQ.

        Args:
            event (dict): the event dict
        """
        sanitized = json.dumps(event, cls=PixmlJsonEncoder)
        self.socket.send_string(sanitized)
