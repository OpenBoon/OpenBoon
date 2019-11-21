import logging
import os
import shutil
import tempfile
import unittest
import time
import json

from pixml.asset import AssetSpec
from pixml.analysis.base import Reactor, Context, AssetBuilder, Generator, Argument

logger = logging.getLogger(__name__)


class TestProcessor(AssetBuilder):
    """
    A processor for running simple execution tests.  Having a test
    processor in the SDK allows us to use the core image for
    testing the execution module.
    """
    def __init__(self):
        super(TestProcessor, self).__init__()
        self.add_arg(Argument('attrs', 'struct', default=None))
        self.add_arg(Argument('send_event', 'str', default=None))

    def process(self, frame):
        self.logger.info("Running TestProcessor process()")
        attrs = self.arg_value("attrs")
        if attrs:
            self.logger.info("setting attrs: {}".format(attrs))
            for k, v in attrs.items():
                frame.asset.set_attr(k, v)

        event_type = self.arg_value("send_event")
        if event_type:
            self.logger.info("emitting event: {}".format(event_type))
            self.reactor.emitter.write({"type": event_type, "payload": {"ding": "dong"}})
        time.sleep(1)

    def teardown(self):
        self.logger.info("Running TestProcessor teardown()")


class TestGenerator(Generator):
    """
    A test generator which generates frames from a supplied array
    of files paths.
    """
    def __init__(self):
        super(TestGenerator, self).__init__()
        self.add_arg(Argument('files', 'list', default=[]))

    def generate(self, consumer):
        for file in self.arg_value('files'):
            spec = AssetSpec(file)
            consumer.accept(spec)

    def teardown(self):
        self.logger.info("Running TestGenerator teardown()")


class PluginUnitTestCase(unittest.TestCase):
    """
    A base class for unit-testing Processor Plugins.

    """
    @classmethod
    def setUpClass(cls):
        """
        Setup the a Test OFS implementation.

        """
        cls.tmp_dir = tempfile.mkdtemp("ofs", "zorroa")

    @classmethod
    def tearDownClass(cls):
        """
        Remove temp files associated with the test OFS.

        """
        shutil.rmtree(cls.tmp_dir)

    def init_processor(self, processor, args=None, global_args=None):
        """
        A convenience method for initializing a processor.

        Args:
            processor (:obj:`Processor`) The processor instance.
            args (:obj:`dict`): The processor args.
            global_args: (:obj:`dict`): Any globals

        Returns:
           (:obj:`Processor`): The configured processor.

        """
        reactor = Reactor(TestEventEmitter())
        processor.set_context(Context(reactor, args, global_args or {}))
        processor.init()
        return processor


class TestConsumer:
    """
    A Consumer implementation that allows the user to inspect the frames
    that have been consumed.

    """
    __test__ = False

    def __init__(self):
        self.consumed = []
        self.count = 0

    def accept(self, frame):
        self.consumed.append(frame)
        self.count += 1


class TestEventEmitter(object):
    """
    This is an emitter class used for dumping Processor execution
    events to stdout.
    """
    def __init__(self):
        self.events = []

    def write(self, event):
        """
        Write an event
        Args:
            event (dict): The event to write.
        """
        self.events.append(event)
        logger.debug("Event type='%s'" % event.get("type"))
        logger.debug(json.dumps(event, sort_keys=True, indent=4))

    def clear(self):
        """
        Clear the event cache.
        """
        self.events = []

    def event_count(self, etype):
        """
        Return the number of events of the given type this
        emitter has seen.

        Args:
            etype (str): The type of event.

        Returns:
            (int): The number of events.

        """
        count = 0
        for event in self.events:
            if event["type"] == etype:
                count += 1
        return count

    def event_total(self):
        """
        Return the total number of events.

        Returns:
            (int): Total number of events.

        """
        return len(self.events)

    def get_events(self, etype):
        """
        Return a list of all events of a given type.
        Args:
            etype (str): the type of event

        Returns:
            (list): A list of events.

        """
        result = []
        for event in self.events:
            if event["type"] == etype:
                result.append(event)
        return result


def zorroa_test_data(rel_path=""):
    """
    Return the absolute path to the given test file.

    Args:
        rel_path (str): A path relative to the zorroa-test-data local sub module.

    Returns:
        str: the absolute path to the test file.

    """
    if os.path.isdir('/test-data'):
        path = os.path.join("/test-data", rel_path)
    else:
        path = os.path.join(os.path.join(os.path.dirname(__file__)),
                            "../../../../../test-data", rel_path)
    return os.path.abspath(os.path.normpath(path))
