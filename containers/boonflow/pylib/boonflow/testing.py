import json
import logging
import os
import shutil
import tempfile
import time
import unittest
import uuid
from urllib.parse import urlparse

import requests

from boonsdk import FileImport, Asset, StoredFile
from boonflow.base import Context, AssetProcessor, Generator, Argument, \
    FatalProcessorException, ProcessorException

logger = logging.getLogger(__name__)


class TestProcessor(AssetProcessor):
    """
    A processor for running simple execution tests.  Having a test
    processor in the SDK allows us to use the core image for
    testing the execution module.
    """

    version = 2

    namespace = "test"

    def __init__(self):
        super(TestProcessor, self).__init__()
        self.add_arg(Argument('sleep', 'int', default=1))
        self.add_arg(Argument('attrs', 'struct', default=None))
        self.add_arg(Argument('send_event', 'str', default=None))
        self.add_arg(Argument('raise_fatal', 'bool', default=False,
                              toolTip='Raise a BoonSdkFatalProcessorException'))
        self.add_arg(Argument('raise', 'bool', default=False,
                              toolTip='Raise a ProcessorException'))
        self.add_arg(Argument('raise_on_init', 'bool', default=False,
                              toolTip='Raise a ProcessorException on init'))

        self.preprocess_ran = False

    def init(self):
        if self.arg_value('raise_on_init'):
            raise ProcessorException('Failed to initialize!')

    def preprocess(self, assets):
        for asset in assets:
            print("Asset: {}".format(asset.id))
        self.preprocess_ran = True

    def process(self, frame):
        self.logger.info('Running TestProcessor process()')
        if self.arg_value('raise_fatal'):
            raise FatalProcessorException('Fatal exception raised')

        if self.arg_value('raise'):
            raise ProcessorException('Warning exception raised')

        attrs = self.arg_value('attrs')
        if attrs:
            self.logger.info('setting attrs: {}'.format(attrs))
            for k, v in attrs.items():
                frame.asset.set_attr(k, v)

        event_type = self.arg_value('send_event')
        if event_type:
            self.logger.info('emitting event: {}'.format(event_type))
            self.reactor.emitter.write({'type': event_type, 'payload': {'ding': 'dong'}})

        self.logger.info("Sleeping {}".format(self.arg_value('sleep')))
        time.sleep(self.arg_value('sleep'))

    def teardown(self):
        self.logger.info('Running TestProcessor teardown()')


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
            spec = FileImport(file)
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
        reactor = TestReactor(TestEventEmitter())
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


class TestAsset(Asset):
    """
    A TestAsset is used for testing processors with local files.
    """
    mimetype_lookup = {
        "jpg": "image/jpeg",
        "mp4": "video/mp4",
        "png": "image/png"
    }

    def __init__(self, path=None, attrs=None, id=None, clip=None):
        """
        Construct a test Asset.

        Args:
            path (str): A URL to a local file.
            attrs(dict): Additional attributes in key/value pair form Eg {"a.b.c": 123})
        """
        super(TestAsset, self).__init__({"id": id or str(uuid.uuid4())})
        self.set_attr("source.path", path)

        if path:
            parsed_uri = urlparse(path)
            ext = os.path.basename(parsed_uri.path).split(".")[-1]

            self.set_attr("source.extension", ext)
            self.set_attr("source.mimetype",
                          self.mimetype_lookup.get(ext, "application/octet-stream"))

        if attrs:
            for k, v in attrs.items():
                self.set_attr(k, v)


class TestReactor(object):
    """
    A TestReactor class for testing expand frame generation.
    """

    def __init__(self, emitter):
        self.emitter = emitter
        self.expand_frames = []

    def add_expand_frame(self, parent_frame, expand_frame, batch_size=None, force=False):
        self.expand_frames.append((parent_frame, expand_frame))

    def check_expand(self, *args, **kwargs):
        pass

    def expand(self, *args):
        pass

    def error(self, *args, **kwargs):
        print(f"Emit Error: {args} {kwargs}")

    def performance_report(self, *args):
        pass

    def emit_status(self, text):
        print(f"Emit Status: '{text}'")

    def progress(self, progress):
        print(f"Emit Progress: '{progress}'")

    def write_event(self, event, payload):
        print(f"Emit event: '{event}'  payload: '{payload}'")


def test_path(rel_path=""):
    """
    Return the absolute path to the given test file.

    Args:
        rel_path (str): A path relative to the zorroa-test-data local sub module.

    Returns:
        str: The path to the test data file
    """
    return test_data(rel_path, False)


def test_data(rel_path="", uri=True):
    """
    Return the absolute path to the given test file.

    Args:
        rel_path (str): A path relative to the zorroa-test-data local sub module.
        uri (bool): return a file:// URI rather than a path.
    Returns:
        str: the absolute path to the test file.

    """
    if os.path.isdir('/test-data'):
        path = os.path.join("/test-data", rel_path)
    else:
        path = os.path.join(os.path.join(os.path.dirname(__file__)),
                            "../../../../test-data", rel_path)

    full_path = os.path.abspath(os.path.normpath(path))
    if uri:
        return "file://{}".format(full_path)
    else:
        return full_path


class MockRequestsResponse:
    """
    A Mock HTTP request response object used for mocking responses from
    the popular python 'requests' library.

    Examples:
        post_patch.return_value = MockRequestsResponse(
            {"output": "boonai://ml-storage/foo/bar"}, 200)

    See Also:
        https://requests.readthedocs.io/en/master/

    """

    def __init__(self, json_data, status_code):
        self.json_data = json_data
        self.status_code = status_code

    def json(self):
        return self.json_data

    def raise_for_status(self):
        if self.status_code > 299:
            raise requests.RequestException("Failed with status {}".format(self.status_code))


def get_prediction_labels(analysis):
    """
    Takes a raw predictions list and returns an array of labels.

    Args:
        analysis (dict): An analysis namespace with a predictions property.

    Returns:
        list[str] A list of label values.

    """
    return [p["label"] for p in analysis['predictions']]


def get_prediction_map(analysis):
    """
    Takes a raw predictions list and returns an dictionary of
    labels and confidence scores.

    Args:
        analysis (dict):

    Returns:

    """
    return dict([(p["label"], p['score']) for p in analysis['predictions']])


def get_mock_stored_file(category="proxy", mimetype="image/jpeg"):
    """
    A convenience methods for returning mock stored file with a
    random name.  This ensures the the local file cache is always
    seeing a new file.

    Args:
        category (str): An optional category name.

    Returns:
        StoredFile: A stored file with cache-buster values.
    """
    return StoredFile({
        'id': 'assets/{}/{}/bar.jpg'.format(uuid.uuid4(), category),
        'name': 'bar.jpg',
        'attrs': {
            'width': 100,
            'height': 100
        },
        'mimetype': mimetype,
        'size': 1000,
        "category": 'proxy'
    })
