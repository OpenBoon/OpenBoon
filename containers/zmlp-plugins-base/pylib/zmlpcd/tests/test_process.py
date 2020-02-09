import logging
import unittest
from unittest.mock import patch

from zmlpcd.process import ProcessorExecutor, AssetConsumer, is_file_type_allowed
from zmlpcd.reactor import Reactor
from zmlpsdk.testing import TestEventEmitter, TestAsset
from zmlpsdk import Frame

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
                "className": "zmlpsdk.testing.TestProcessor",
                "args": {},
                "image": "plugins-py3-base:latest"
            },
            "asset": {
                "id": "1234"
            }
        }
        frame = self.pe.execute_processor(req)
        assert self.emitter.event_count("asset") == 1
        assert self.emitter.event_count("error") == 0
        assert self.emitter.event_total() == 1

        # Make sure we got metrics
        assert frame.asset["metrics"]["pipeline"]
        assert "zmlpsdk.testing.TestProcessor" == frame.asset["metrics"]["pipeline"][0]["processor"]

    def test_execute_processor_and_raise_fatal(self):
        req = {
            "ref": {
                "className": "zmlpsdk.testing.TestProcessor",
                "args": {"raise_fatal": True},
                "image": "plugins-py3-base:latest"
            },
            "asset": {
                "id": "1234",
                "document": {
                    "source": {
                        "path": "/foo/bing.jpg"
                    }
                }
            }
        }
        frame = self.pe.execute_processor(req)
        assert self.emitter.event_count("asset") == 1
        assert self.emitter.event_count("error") == 1
        assert self.emitter.event_total() == 2

        error = self.emitter.get_events("error")[0]
        assert error["payload"]["processor"] == "zmlpsdk.testing.TestProcessor"
        assert error["payload"]["fatal"] is True
        assert error["payload"]["phase"] == "execute"
        assert error["payload"]["path"] == "/foo/bing.jpg"

        assert frame.asset["metrics"]["pipeline"][0]["error"] == "fatal"

    def test_apply_metrics(self):
        ref = {
            "className": "zmlpsdk.testing.TestProcessor",
            "args": {},
            "image": "plugins-py3-base:latest"
        }
        frame = Frame(TestAsset())
        wrapper = self.pe.get_processor_wrapper(ref)
        wrapper.apply_metrics(frame.asset, 10, None)

        metrics = frame.asset["metrics"]["pipeline"][0]
        assert "zmlpsdk.testing.TestProcessor" == metrics['processor']
        assert None is metrics["module"]
        assert 2 == metrics["version"]
        assert 1 == metrics["executionCount"]
        assert 10 == metrics["executionTimeTotal"]
        assert None is not metrics["executionDateLast"]
        assert "test" == metrics["namespace"]

    @patch.object(Reactor, 'check_expand')
    def test_teardown_processor(self, react_patch):
        req = {
            "ref": {
                "className": "zmlpsdk.testing.TestProcessor",
                "args": {},
                "image": "plugins-py3-base:latest"
            },
            "asset": {
                "id": "1234"
            }
        }
        self.pe.execute_processor(req)
        self.pe.teardown_processor(req)
        stats = self.emitter.get_events("stats")
        assert len(stats) == 1

        react_patch.assert_called_with(force=True)

    def test_get_processor_wrapper(self):
        ref = {
            "className": "zmlpsdk.testing.TestProcessor",
            "args": {},
            "image": "plugins-py3-base:latest"
        }
        wrapper = self.pe.get_processor_wrapper(ref)
        assert wrapper is not None
        assert wrapper.instance is not None
        assert wrapper.ref == ref

        wrapper2 = self.pe.get_processor_wrapper(ref)
        assert wrapper2.ref == wrapper.ref

    def test_new_processor_instance(self):
        ref = {
            "className": "zmlpsdk.testing.TestProcessor",
            "args": {},
            "image": "plugins-py3-base:latest"
        }
        instance = self.pe.new_processor_instance(ref)
        assert instance.__class__.__name__ == "TestProcessor"

    def test_is_aleady_processed(self):
        ref = {
            "className": "zmlpsdk.testing.TestProcessor",
            "args": {},
            "image": "plugins-py3-base:latest"
        }
        frame = Frame(TestAsset())
        wrapper = self.pe.get_processor_wrapper(ref)

        assert not wrapper.is_already_processed(frame.asset)
        wrapper.apply_metrics(frame.asset, 10, None)
        assert wrapper.is_already_processed(frame.asset)

        # Now override with _force=true
        ref["args"]["_force"] = True
        assert not wrapper.is_already_processed(frame.asset)


class TestAssetConsumer(unittest.TestCase):

    def setUp(self):
        self.emitter = TestEventEmitter()
        self.reactor = Reactor(self.emitter)
        self.consumer = AssetConsumer(self.reactor, {"fileTypes": ["jpg", "mp4"]})

    def testAccept(self):
        asset1 = TestAsset("gs://foo/bar/bing.jpg")
        assert self.consumer.accept(asset1)

        asset2 = TestAsset("gs://foo/bar/car.exr")
        assert not self.consumer.accept(asset2)

    def testExpand(self):
        self.consumer.batch_size = 2
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
