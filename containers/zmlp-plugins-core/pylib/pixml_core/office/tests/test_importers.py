import os
import json
import pytest
from pathlib2 import Path
from unittest.mock import patch

from pixml.analysis.storage import file_cache
from pixml.analysis import Frame, PixmlUnrecoverableProcessorException
from pixml.analysis.testing import PluginUnitTestCase, TestAsset, zorroa_test_data

from pixml_core.office.importers import OfficeImporter, _content_sanitizer
from pixml_core.office.oclient import OfficerClient


class OfficeImporterUnitTestCase(PluginUnitTestCase):

    def setUp(self):
        self.path = Path('/tmp/path/file.pdf')
        self.asset = TestAsset(str(self.path))

    def test_bad_extension(self):
        path = Path('/tmp/file.bad')
        try:
            path.touch()
            frame = Frame(TestAsset(str(path)))
            processor = self.init_processor(OfficeImporter(), {})
            with pytest.raises(PixmlUnrecoverableProcessorException) as error:
                processor.process(frame)
            assert 'Storage failure Invalid URI' in error.value.args[0]
            processor.teardown()
        finally:
            if path.exists():
                path.unlink()

    def test_content_sanitizer(self):
        has_nulls = r'This string has embedded \u0000 null.'
        null_removed = 'This string has embedded   null.'

        metadata = json.loads('{"content": "%s"}' % has_nulls, object_hook=_content_sanitizer)
        self.assertDictEqual({'content': null_removed}, metadata)

    @patch.object(OfficerClient, 'exists')
    def test_needs_rerender(self, patch_exists):
        patch_exists.return_value = False
        asset = TestAsset('some_file.docx')
        processor = self.init_processor(OfficeImporter(), {})
        assert processor._needs_rerender(asset, 1) is True

    @patch.object(OfficerClient, 'exists')
    def test_needs_rerender_all_exist(self, patch_exists):
        patch_exists.return_value = True
        processor = self.init_processor(OfficeImporter(), {})
        assert processor._needs_rerender(self.asset, 1) is False

    @patch.object(OfficeImporter, 'get_metadata',
                  return_value={'author': 'Zach', 'content': 'temp'})
    @patch.object(OfficerClient, 'render', return_value='/fake')
    def test_process_loads_metadata_to_asset(self, _, __):
        processor = self.init_processor(OfficeImporter(), {'extract_pages': False})
        processor.process(Frame(self.asset))
        assert self.asset.get_attr('media.author') == 'Zach'
        assert self.asset.get_attr('media.content') == 'temp'

    @patch.object(OfficeImporter, 'get_metadata',
                  return_value={'author': 'Zach'})
    @patch.object(OfficerClient, 'render', return_value='/fake')
    def test_process_loads_metadata(self, _, __):
        processor = self.init_processor(OfficeImporter(), {'extract_pages': False})
        processor.process(Frame(self.asset))
        assert self.asset.get_attr('media.author') == 'Zach'
        assert self.asset.get_attr('media.content') is None
        assert self.asset.get_attr('tmp.proxy_source_image').endswith("proxy.0.jpg")

    @patch.object(OfficeImporter, 'expand')
    @patch.object(OfficeImporter, 'get_metadata', return_value={'length': 3})
    @patch.object(OfficerClient, 'render', return_value='/fake')
    def test_process_expands_children(self, _, __, expand_patch):
        processor = self.init_processor(OfficeImporter(), {'extract_pages': True})
        processor.process(Frame(self.asset))
        assert expand_patch.call_count == 3

    @patch.object(file_cache, 'localize_uri')
    def test_get_metadata(self, cache_patch):
        path = os.path.dirname(__file__) + '/test_metadata.json'
        cache_patch.return_value = path
        processor = self.init_processor(OfficeImporter(), {'extract_pages': True})
        md = processor.get_metadata(self.asset, 1)
        assert md['title'] == 'PhD Thesis on \'Die Hard\''
        assert md['type'] == 'document'
        assert md['length'] == 6

    def test_get_image_uri(self):
        processor = self.init_processor(OfficeImporter(), {'extract_pages': True})
        md = processor.get_image_uri("pixml://ml-storage/tmp-files/officer/foo/bar", 1)
        assert md.startswith("pixml://")
        assert md.endswith('proxy.1.jpg')

    @patch.object(OfficerClient, '_get_render_request_body', return_value={})
    @patch.object(OfficerClient, 'render')
    def test_render_outputs_exception(self, _, __):
        processor = self.init_processor(OfficeImporter(), {})
        with pytest.raises(PixmlUnrecoverableProcessorException):
            processor.process(Frame(self.asset))