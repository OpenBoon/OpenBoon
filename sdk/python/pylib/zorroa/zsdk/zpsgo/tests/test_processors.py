import os
import logging

from zorroa.zsdk.processor import DocumentProcessor, Generator, Collector, Argument, Frame
from zorroa.zsdk.document.asset import Asset

logger = logging.getLogger(__name__)


class GroupProcessor(DocumentProcessor):
    """
    A GroupProcessor is for holding sub processors. By itself, GroupProcessor is a no-op.
    """
    def __init__(self):
        super(GroupProcessor, self).__init__()

    def _process(self, frame):
        self.logger.info("Processing %s" % frame.asset.id)


class TestCollector(Collector):
    """
    A Processor that sets a simple value for testing expressions
    """
    __test__ = False

    def __init__(self):
        super(TestCollector, self).__init__()


class TestExceptionProcessor(DocumentProcessor):
    """
    A Processor that sets a simple value for testing expressions
    """
    __test__ = False

    def _process(self, frame):
        raise Exception("process failed!")

    def __init__(self):
        super(TestExceptionProcessor, self).__init__()


class TestSetValueProcessor(DocumentProcessor):
    """
    A Processor that sets a simple value for testing expressions
    """
    __test__ = False

    def __init__(self):
        super(TestSetValueProcessor, self).__init__()
        self.add_arg(Argument("value", "string", required=True))

    def _process(self, frame):
        frame.asset.set_attr("test.value", self.arg_value("value"))


class TestEnvironmentProcessor(DocumentProcessor):
    """
    A Processor that sets a simple value for testing expressions
    """
    __test__ = False

    def __init__(self):
        super(TestEnvironmentProcessor, self).__init__()
        self.add_arg(Argument("key", "string", required=True))
        self.add_arg(Argument("value", "string", required=False))

    def _process(self, frame):
        key = self.arg_value("key")
        value = self.arg_value("value")
        os_value = os.environ.get(key)

        logger.info("Asserting that {} {}=={}"
                    .format(key, value, os_value))

        if value != os_value:
            logger.info("Raising exception")
            raise RuntimeError("ENV {} {} != {}".format(key, value, os_value))


class OfsTestProcessor(DocumentProcessor):
    """
    A processor to test OFS
    """
    def __init__(self):
        super(OfsTestProcessor, self).__init__()
        self.add_arg(Argument("random_value", "string", required=True))

    def _process(self, frame):
        p = self.ofs.prepare("unittest", frame.asset, "test.txt")
        with p.open(mode="w") as fp:
            fp.write(self.arg_value("random_value"))


class FileGenerator(Generator):
    def __init__(self, **kwargs):
        super(FileGenerator, self).__init__(**kwargs)
        self.add_arg(Argument("paths", "list", default=[], required=True))

    def generate(self, consumer):
        for path in self.arg_value("paths"):
            logger.info("Generating %s" % path)
            consumer.accept(Frame(Asset(os.path.realpath(path))))


class SkipProcessor(DocumentProcessor):
    """Dummy processor that just sets every frame to skip."""
    def _process(self, frame):
        frame.skip = True
