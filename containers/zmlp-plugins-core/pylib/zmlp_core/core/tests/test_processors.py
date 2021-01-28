from unittest.mock import patch

import pytest

from zmlp import ZmlpClient
from zmlp.app import AssetApp
from zmlp_core.core.processors import SetAttributesProcessor, AssertAttributesProcessor, \
    PreCacheSourceFileProcessor, DeleteBySearchProcessor
from zmlpsdk import Frame, ZmlpFatalProcessorException
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_data, Context

MOCK_PERMISSION = {
    'authority': 'string',
    'description': 'string',
    'fullName': 'string',
    'id': 'string',
    'name': 'string',
    'type': 'string'
}


class SetAttributesProcessorTests(PluginUnitTestCase):

    def test_set_attributes_processor_set_attrs(self):
        frame = Frame(TestAsset("cat.jpg"))
        ih = self.init_processor(SetAttributesProcessor(), args={'attrs': {'foo.bar': 1}})
        ih.process(frame)
        self.assertEquals(1, frame.asset.get_attr("foo.bar"))

    def test_set_attributes_processor_remove_attrs(self):
        frame = Frame(TestAsset("cat.jpg", {"foo": "bar"}))
        self.assertEquals("bar", frame.asset.get_attr("foo"))
        ih = self.init_processor(SetAttributesProcessor(), args={'remove_attrs': ['foo']})
        ih.process(frame)
        self.assertIsNone(frame.asset.get_attr("foo"))


class AssertAttributesProcessorTests(PluginUnitTestCase):

    def test_assert_raises(self):
        frame = Frame(TestAsset("cat.jpg"))
        ih = self.init_processor(AssertAttributesProcessor(), args={'attrs': ['foo.bar']})
        with pytest.raises(ZmlpFatalProcessorException):
            ih.process(frame)

    def test_assert_success(self):
        frame = Frame(TestAsset("cat.jpg"))
        ih = self.init_processor(AssertAttributesProcessor(), args={'attrs': ['source.path']})
        ih.process(frame)


class PreCacheSourceFileProcessorTests(PluginUnitTestCase):

    TOUCAN = zorroa_test_data("images/set01/toucan.jpg")

    def test_assert_process_raises(self):
        frame = Frame(TestAsset("cat.jpg"))
        ih = self.init_processor(PreCacheSourceFileProcessor(), args={'attrs': ['foo.bar']})
        with pytest.raises(ZmlpFatalProcessorException):
            ih.process(frame)

    def test_process(self):
        frame = Frame(TestAsset(self.TOUCAN))
        ih = self.init_processor(PreCacheSourceFileProcessor())
        ih.process(frame)
        assert frame.asset.get_attr("source.filesize") == 97221
        assert frame.asset.get_attr("source.checksum") == 1582911032

    def test_process_skip_analyzed(self):
        frame = Frame(TestAsset(self.TOUCAN))
        frame.asset.set_attr('system.state', 'Analyzed')
        ih = self.init_processor(PreCacheSourceFileProcessor())
        ih.process(frame)
        assert frame.asset.get_attr("source.filesize") is None
        assert frame.asset.get_attr("source.checksum") is None


class DeleteBySearchProcessorTests(PluginUnitTestCase):

    @patch.object(ZmlpClient, 'delete')
    @patch.object(AssetApp, 'scroll_search')
    def test_generate(self, scroll_search_patch, del_patch):
        scroll_search_patch.return_value = {}
        del_patch.return_value = {}

        proc = DeleteBySearchProcessor()
        proc.set_context(Context(None,
                                 {'dataSourceId': 'c13cf8ae-a009-4ff8-b8f0-6db7eec96cf1'}, {}))
        proc.process(None)
