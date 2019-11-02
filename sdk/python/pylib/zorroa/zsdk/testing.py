import logging
import os
import shutil
import tempfile
import unittest
import time
import json

from filecmp import cmp

from pathlib2 import Path

from .processor import Reactor, Context, DocumentProcessor, Argument
from .ofs.core import AbstractObjectFileSystem, AbstractObjectFile
from .ofs import set_ofs

logger = logging.getLogger(__name__)


class TestProcessor(DocumentProcessor):
    """
    A processor for running simple execution tests.  Having a test
    processor in the SDK allows us to use the core image for
    testing the execution module.
    """
    def __init__(self):
        super(TestProcessor, self).__init__()
        self.add_arg(Argument('attrs', 'struct', default=None))
        self.add_arg(Argument('send_event', 'str', default=None))

    def _process(self, frame):
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


class TestObjectFile(AbstractObjectFile):
    """An ObjectFile represents a single file with an ObjectFileSystem.

    This class is a base class that should be inherited from when creating
    any concrete ObjectFile classes.
    """
    __test__ = False

    def __init__(self, path, system):
        super(TestObjectFile, self).__init__()
        self._path = path
        self.system = system

    @property
    def path(self):
        return self._path

    def store(self, rfp):
        if isinstance(rfp, str):
            rfp = open(rfp, "rb")
        elif isinstance(rfp, Path):
            rfp = rfp.open('rb')

        parent_path = os.path.dirname(self.path)
        if not os.path.exists(parent_path):
            os.makedirs(parent_path, 0o755)

        with (open(self.path, "wb")) as wfp:
            wfp.write(rfp.read())
        return self

    def open(self, mode="r"):
        return open(self.path, mode)

    def exists(self):
        return os.path.exists(self.path)

    def mkdirs(self):
        Path(self.path).parent.mkdir(exist_ok=True, parents=True)

    def stat(self):
        pass

    @property
    def size(self):
        return os.path.getsize(self.path)

    @property
    def id(self):
        return self.path

    @property
    def storage_type(self):
        return "file"

    def sync_local(self):
        return self.path

    def sync_remote(self):
        pass

    def __str__(self):
        return self.path

    def __repr__(self):
        return "<TestObjectFile id=%s path=%s>" % (self.id, self.path)

    def __eq__(self, other):
        return self.id == other.id

    def __cmp__(self, other):
        return cmp(other.path, self.path)

    def __hash__(self):
        return hash(self.id)


class TestObjectFileSystem(AbstractObjectFileSystem):
    __test__ = False

    def __init__(self, root):
        super(TestObjectFileSystem, self).__init__()
        os.makedirs(root)
        self.root = root

    def init(self):
        """Does any initialization required by the file system."""
        pass

    def prepare(self, parent_type, parent_id, name):
        path = os.path.join(self.root, "%s_%s/%s" %
                            (parent_type, self._extract_id(parent_id), name))
        logger.info("OFS PREPARE: %s" % path)
        return TestObjectFile(path, self)

    def stat(self, parent_type, parent_id, name):
        path = os.path.join(self.root, "%s_%s/%s" %
                            (parent_type, self._extract_id(parent_id), name))
        logger.info("OFS STAT: %s" % path)
        return TestObjectFile(path, self)

    def get(self, id):
        logger.info("OFS GET: %s" % id)
        return TestObjectFile(id, self)

    def _extract_id(self, obj):
        """Returns the 'id' property of a given object, or it's string rep
        if that fails.

        Args:
            obj (mixed): Typically a zorroa class, or anything with an 'id'
                property.

        Returns (str): the id of the obj

        """
        try:
            return obj.id
        except AttributeError:
            return str(obj)


class TestExecutor:
    """A mock executor we can use to check the status of reactor events.
    """
    __test__ = False

    def __init__(self, proc):
        self.proc = proc
        self.proc.executor = self
        self.script = None

    def write(self, script):
        self.script = script


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
        cls.ofs = TestObjectFileSystem(cls.tmp_dir + "/ofs")
        set_ofs(cls.ofs)

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
        reactor = Reactor(TestExecutor(processor), None)
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
