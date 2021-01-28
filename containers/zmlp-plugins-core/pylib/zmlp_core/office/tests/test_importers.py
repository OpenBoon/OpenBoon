import json
import os
import tempfile
from unittest.mock import patch

import pytest
from pathlib import Path

from zmlpsdk import Frame, ZmlpFatalProcessorException
from zmlpsdk.storage import file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path
from zmlp_core.office.importers import OfficeImporter, _content_sanitizer
from zmlp_core.office.oclient import OfficerClient
from zmlp_core.util.media import media_size


class OfficeImporterUnitTestCase(PluginUnitTestCase):

    def setUp(self):
        self.path = Path(zorroa_test_path('office/test_document.docx'))
        self.asset = TestAsset(str(self.path), id="qwerty1")
        os.environ['ZMLP_JOB_ID'] = "abc123"

    def tearDown(self):
        del os.environ['ZMLP_JOB_ID']

    @patch.object(OfficerClient, 'get_cache_location', return_value=None)
    def test_bad_extension(self, _):
        path = Path('/tmp/file.bad')
        try:
            path.touch()
            frame = Frame(TestAsset(str(path)))
            processor = self.init_processor(OfficeImporter(), {})
            with pytest.raises(ZmlpFatalProcessorException) as error:
                processor.process(frame)
            assert 'Unable to determine page cache location' in error.value.args[0]
            processor.teardown()
        finally:
            if path.exists():
                path.unlink()

    def test_content_sanitizer(self):
        has_nulls = r'This string has embedded \u0000 null.'
        null_removed = 'This string has embedded   null.'

        metadata = json.loads('{"content": "%s"}' % has_nulls, object_hook=_content_sanitizer)
        self.assertDictEqual({'content': null_removed}, metadata)

    @patch('zmlp_core.office.importers.OfficeImporter.get_metadata', return_value={})
    @patch.object(OfficerClient, 'render', return_value='/fake')
    @patch.object(OfficerClient, 'get_cache_location', return_value=None)
    @patch.object(OfficerClient, 'wait_for_rendering', return_value=None)
    def test_render_pagesno_clip_page_1(self, _, __, ___, ____):
        processor = self.init_processor(OfficeImporter(), {})
        processor.render_pages(self.asset, 1, False)
        assert self.asset.get_attr('tmp.proxy_source_image') \
               == 'zmlp://job/abc123/officer/qwerty1_proxy.1.jpg'

    @patch('zmlp_core.office.importers.OfficeImporter.get_metadata', return_value={})
    @patch.object(OfficerClient, 'get_cache_location', return_value=None)
    @patch.object(OfficerClient, 'render', return_value='/fake')
    @patch.object(OfficerClient, 'wait_for_rendering', return_value=None)
    def test_render_page_clip_page_2_not_exist(self, _, __, ___, ____):
        processor = self.init_processor(OfficeImporter(), {})
        processor.render_pages(self.asset, 2, False)
        assert self.asset.get_attr('tmp.proxy_source_image') \
               == 'zmlp://job/abc123/officer/qwerty1_proxy.2.jpg'

    @patch('zmlp_core.office.importers.OfficeImporter.get_metadata', return_value={})
    @patch.object(OfficerClient, 'get_cache_location', return_value="/cached")
    @patch.object(OfficerClient, 'wait_for_rendering', return_value=None)
    def test_render_page_clip_cached(self, _, __, ___):
        processor = self.init_processor(OfficeImporter(), {})
        processor.render_pages(self.asset, 3, False)
        assert self.asset.get_attr('tmp.proxy_source_image') == \
               'zmlp://job/abc123/officer/qwerty1_proxy.3.jpg'

    @patch.object(OfficeImporter, 'get_metadata',
                  return_value={'author': 'Zach', 'content': 'temp'})
    @patch.object(OfficerClient, 'render', return_value='/fake')
    @patch.object(OfficerClient, 'get_cache_location', return_value=None)
    @patch.object(OfficerClient, 'wait_for_rendering', return_value=None)
    def test_process_loads_metadata_to_asset(self, _, __, ___, ____):
        processor = self.init_processor(OfficeImporter())
        processor.process(Frame(self.asset))
        assert self.asset.get_attr('media.author') == 'Zach'
        assert self.asset.get_attr('media.content') == 'temp'

    @patch.object(OfficeImporter, 'get_metadata',
                  return_value={'author': 'Zach'})
    @patch.object(OfficerClient, 'render', return_value='/fake')
    @patch.object(OfficerClient, 'get_cache_location', return_value=None)
    @patch.object(OfficerClient, 'wait_for_rendering', return_value=None)
    def test_process_loads_metadata(self, _, __, ___, ____):
        processor = self.init_processor(OfficeImporter())
        processor.process(Frame(self.asset))
        assert self.asset.get_attr('media.author') == 'Zach'
        assert self.asset.get_attr('media.content') is None
        assert self.asset.get_attr('tmp.proxy_source_image').endswith("proxy.1.jpg")

    @patch.object(OfficerClient, 'get_cache_location', return_value=None)
    @patch.object(OfficeImporter, 'expand')
    @patch.object(OfficeImporter, 'get_metadata', return_value={'length': 3})
    @patch.object(OfficerClient, 'render', return_value='/fake')
    @patch.object(OfficerClient, 'wait_for_rendering', return_value=None)
    def test_process_expands_children(self, _, __, ___, expand_patch, ____):
        processor = self.init_processor(OfficeImporter(), {"extract_doc_pages": True})
        processor.process(Frame(self.asset))
        assert expand_patch.call_count == 2

    @patch.object(file_storage, 'localize_file')
    def test_get_metadata(self, cache_patch):
        path = os.path.dirname(__file__) + '/test_metadata.json'
        cache_patch.return_value = path
        processor = self.init_processor(OfficeImporter())
        md = processor.get_metadata(self.asset, 1)
        assert md['title'] == 'PhD Thesis on \'Die Hard\''
        assert md['type'] == 'document'
        assert md['length'] == 6

    @patch.object(OfficerClient, '_get_render_request_body', return_value={})
    @patch.object(OfficerClient, 'render')
    @patch.object(OfficerClient, 'get_cache_location', return_value=None)
    @patch.object(OfficerClient, 'wait_for_rendering', return_value=None)
    def test_render_outputs_exception(self, _, __, ___, ____):
        processor = self.init_processor(OfficeImporter(), {})
        with pytest.raises(ZmlpFatalProcessorException):
            processor.process(Frame(self.asset))

    @patch('zmlp_core.office.importers.OfficeImporter.get_metadata', return_value={})
    @patch.object(OfficerClient, '_get_render_request_body', return_value={})
    @patch.object(OfficerClient, 'render', return_value='zmlp://foo/bar')
    @patch.object(OfficerClient, 'get_cache_location', return_value=None)
    @patch.object(OfficerClient, 'wait_for_rendering', return_value=None)
    def test_render_pages(self, _, __, ___, ____, _____):
        processor = self.init_processor(OfficeImporter(), {})
        output_uri = processor.render_pages(self.asset, 1, True)
        assert 'zmlp://foo/bar' == output_uri
        assert self.asset["tmp.proxy_source_image"] == \
               'zmlp://job/abc123/officer/qwerty1_proxy.1.jpg'

    @patch.object(OfficerClient, 'get_cache_location', return_value=None)
    @patch.object(OfficeImporter, 'expand')
    @patch.object(OfficeImporter, 'get_metadata', return_value={'length': 3})
    @patch.object(OfficerClient, 'render', return_value='/fake')
    @patch.object(OfficerClient, 'wait_for_rendering', return_value=None)
    def test_render_pdf(self, _, __, ___, ____, _____):
        path = Path(zorroa_test_path('office/simple.pdf'))
        asset = TestAsset(str(path), id="12345")
        asset.set_attr('media.width', 612)
        asset.set_attr('media.height', 792)
        processor = self.init_processor(OfficeImporter(), {})
        processor.process(Frame(asset))
        assert asset['tmp.proxy_source_image'] == tempfile.gettempdir() + '/12345_pdf_proxy.jpg'

    @patch.object(OfficerClient, 'get_cache_location', return_value=None)
    @patch.object(OfficeImporter, 'expand')
    @patch.object(OfficeImporter, 'get_metadata', return_value={'length': 3})
    @patch.object(OfficerClient, 'render', return_value='/fake')
    @patch.object(OfficerClient, 'wait_for_rendering', return_value=None)
    def test_render_big_pdf(self, _, __, ___, ____, _____):
        path = Path(zorroa_test_path('office/big.pdf'))
        asset = TestAsset(str(path), id="12345")
        asset.set_attr('media.width', 6696)
        asset.set_attr('media.height', 2952)
        processor = self.init_processor(OfficeImporter(), {})
        processor.process(Frame(asset))
        assert asset['tmp.proxy_source_image'] == tempfile.gettempdir() + '/12345_pdf_proxy.jpg'
        size = media_size(asset['tmp.proxy_source_image'])
        print(asset['tmp.proxy_source_image'])
        assert size[0] == 10000
        assert size[1] == 4409
