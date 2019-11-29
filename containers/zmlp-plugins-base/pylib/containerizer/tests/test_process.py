import logging
import unittest

from containerizer.process import ProcessorExecutor, AssetConsumer, is_file_type_allowed

from pixml.analysis import Reactor
from pixml.analysis.testing import TestEventEmitter, TestAsset

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
                "className": "pixml.analysis.testing.TestProcessor",
                "args": {},
                "image": "plugins-py3-base"
            },
            "asset": {
                "id": "1234"
            }
        }
        self.pe.execute_processor(req)
        assert self.emitter.event_count("asset") == 1
        assert self.emitter.event_count("error") == 0
        assert self.emitter.event_count("finished") == 1
        assert self.emitter.event_total() == 2

    def test_teardown_processor(self):
        req = {
            "ref": {
                "className": "pixml.analysis.testing.TestProcessor",
                "args": {},
                "image": "plugins-py3-base"
            },
            "asset": {
                "id": "1234"
            }
        }
        self.pe.execute_processor(req)
        self.pe.teardown_processor(req)
        stats = self.emitter.get_events("stats")
        assert len(stats) == 1

    def test_get_processor_wrapper(self):
        ref = {
            "className": "pixml.analysis.testing.TestProcessor",
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
            "className": "pixml.analysis.testing.TestProcessor",
            "args": {},
            "image": "plugins-py3-base"
        }
        instance = self.pe.new_processor_instance(ref)
        assert instance.__class__.__name__ == "TestProcessor"


class TestAssetConsumer(unittest.TestCase):

    def setUp(self):
        self.emitter = TestEventEmitter()
        self.reactor = Reactor(self.emitter)
        self.consumer = AssetConsumer(self.reactor, ["jpg", "mp4"])

    def testAccept(self):
        asset1 = TestAsset("gs://foo/bar/bing.jpg")
        assert self.consumer.accept(asset1)

        asset2 = TestAsset("gs://foo/bar/car.exr")
        assert not self.consumer.accept(asset2)

    def testExpand(self):
        self.reactor.batch_size = 2
        asset1 = TestAsset("gs://foo/bar/bing.jpg")
        assert self.consumer.accept(asset1)
        assert len(self.consumer.expand) == 1
        assert self.consumer.accept(asset1)
        assert len(self.consumer.expand) == 0


class TypeFilterTests(unittest.TestCase):

    def test_is_file_type_allowed(self):
        asset = TestAsset("gs://foo/bar/bing.jpg")
        assert is_file_type_allowed(asset, frozenset(["jpg"]))
        assert not is_file_type_allowed(asset, frozenset(["png"]))