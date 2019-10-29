import unittest
import logging

from zorroa.zsdk.zps.process import ProcessorExecutor
from zorroa.zsdk.testing import TestEventEmitter
from zorroa.zsdk.processor import Reactor

logging.basicConfig(level=logging.DEBUG)


class ProcessorExecutorTests(unittest.TestCase):

    def setUp(self):
        self.emitter = TestEventEmitter()
        self.pe = ProcessorExecutor(Reactor(self.emitter))

    def test_get_processor_key(self):
        ref1 = {
            "className": "foo.bar",
            "args": {"pop": "tarts"}
        }
        ref2 = {
            "className": "foo.bar",
            "args": {"pop": "tart"}
        }
        key1 = self.pe.get_processor_key(ref1)
        key2 = self.pe.get_processor_key(ref2)
        assert key1 != key2

    def test_execute_processor(self):
        req = {
            "ref": {
                "className": "zorroa.zsdk.testing.TestProcessor",
                "args": {},
                "image": "plugins-py3-base"
            },
            "object": {
                "id": "1234"
            }
        }
        self.pe.execute_processor(req)
        assert self.emitter.event_count("object") == 1
        assert self.emitter.event_count("error") == 0
        assert self.emitter.event_total() == 1

    def test_teardown_processor(self):
        req = {
            "ref": {
                "className": "zorroa.zsdk.testing.TestProcessor",
                "args": {},
                "image": "plugins-py3-base"
            },
            "object": {
                "id": "1234"
            }
        }
        self.pe.execute_processor(req)
        self.pe.teardown_processor(req)
        stats = self.emitter.get_events("stats")
        assert len(stats) == 1

    def test_get_processor_wrapper(self):
        req = {
            "className": "zorroa.zsdk.testing.TestProcessor",
            "args": {},
            "image": "plugins-py3-base"
        }
        wrapper = self.pe.get_processor_wrapper(req)
        assert wrapper is not None
        assert wrapper.instance is not None
        assert wrapper.ref == req

    def test_get_processor_wrapper(self):
        ref = {
            "className": "zorroa.zsdk.testing.TestProcessor",
            "args": {},
            "image": "plugins-py3-base"
        }
        wrapper = self.pe.get_processor_wrapper(ref)
        assert wrapper is not None
        assert wrapper.instance is not None
        assert wrapper.ref == ref

        wrapper2 = self.pe.get_processor_wrapper(ref)
        assert wrapper2.ref == wrapper.ref

    def test_new_processor_instance(self):
        ref = {
            "className": "zorroa.zsdk.testing.TestProcessor",
            "args": {},
            "image": "plugins-py3-base"
        }
        instance = self.pe.new_processor_instance(ref)
        assert instance.__class__.__name__ == "TestProcessor"
