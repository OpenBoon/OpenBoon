import os
import json
import tempfile
import pytest
import requests
from pathlib2 import Path
from mock import patch, Mock

from zplugins.office.importers import OfficeImporter, _content_sanitizer
from zsdk import Frame, Asset
from zsdk.exception import UnrecoverableProcessorException
from zsdk.testing import PluginUnitTestCase


class OfficeImporterUnitTestCase(PluginUnitTestCase):

    def setUp(self):
        self.path = Path('/tmp/path/file.pdf')
        self.asset = Asset(str(self.path))

    def test_bad_extension(self):
        path = Path('/tmp/file.bad')
        try:
            path.touch()
            frame = Frame(Asset(str(path)))
            processor = self.init_processor(OfficeImporter(), {})
            with pytest.raises(UnrecoverableProcessorException) as error:
                processor.process(frame)
            assert error.value.args[0] == ('An exception was returned while '
                                           'communicating with the Officer service')
            processor.teardown()
        finally:
            if path.exists():
                path.unlink()

    def test_content_sanitizer(self):
        has_nulls = r"This string has embedded \u0000 null."
        null_removed = u"This string has embedded   null."

        metadata = json.loads(u'{"content": "%s"}' % has_nulls, object_hook=_content_sanitizer)
        self.assertDictEqual({"content": null_removed}, metadata)

    def test_service_url(self):
        processor = self.init_processor(OfficeImporter(), {})
        assert processor.service_url == 'http://officer:7081'

    def test_extract_url(self):
        processor = self.init_processor(OfficeImporter(), {})
        assert processor.extract_url == 'http://officer:7081/extract'

    def test_is_content_extractable(self):
        cases = {'file.pdf': True,
                 'file.doc': True,
                 'file.docx': True,
                 'file.ppt': True,
                 'file.pptx': True,
                 'file.xls': False,
                 'file.xlsx': False}
        processor = self.init_processor(OfficeImporter(), {})
        for path, result in cases.iteritems():
            assert processor._is_content_extractable(path) == result

    def test_needs_rerender_catches_missing_proxy(self):
        metadata_file = tempfile.NamedTemporaryFile(prefix='metadata.', suffix='.json')
        path, filename = os.path.split(metadata_file.name)
        clip_start = filename.split('.')
        asset = Asset(metadata_file.name)
        asset.set_attr('media.clip.start', clip_start)
        asset.set_attr('tmp.office_output_dir', path)

        processor = self.init_processor(OfficeImporter(), {})
        assert processor._needs_rerender(asset) == True

    def test_needs_rerender_catches_missing_metadata(self):
        metadata_file = tempfile.NamedTemporaryFile(prefix='proxy.', suffix='.json')
        path, filename = os.path.split(metadata_file.name)
        clip_start = filename.split('.')
        asset = Asset(metadata_file.name)
        asset.set_attr('media.clip.start', clip_start)
        asset.set_attr('tmp.office_output_dir', path)

        processor = self.init_processor(OfficeImporter(), {})
        assert processor._needs_rerender(asset) is True

    @patch('os.path.exists', return_value=True)
    def test_needs_rerender_all_exist(self, _patch):
        self.asset.set_attr('media.clip.start', 1)
        self.asset.set_attr('tmp.office_output_dir', '/fake')
        processor = self.init_processor(OfficeImporter(), {})
        assert processor._needs_rerender(self.asset) == False

    @patch.object(OfficeImporter, '_is_content_extractable', return_value=True)
    def test_get_request_body_no_content_no_page(self, _):
        processor = self.init_processor(OfficeImporter(), {'extract_content': False})
        body = processor._get_request_body(self.asset)
        assert 'content' not in body
        assert 'page' not in body
        assert body['input_file'] == '/tmp/path/file.pdf'
        assert body['output_dir'] == self.asset.id
        assert body['dpi'] == 75

    @patch.object(OfficeImporter, '_is_content_extractable', return_value=True)
    def test_get_request_body_child_asset(self, _):
        processor = self.init_processor(OfficeImporter(), {'extract_content': False})
        self.asset.set_attr("media.clip.parent", "foo")
        body = processor._get_request_body(self.asset)
        assert 'content' not in body
        assert 'page' not in body
        assert body['input_file'] == '/tmp/path/file.pdf'
        assert body['output_dir'] == "foo"
        assert body['dpi'] == 75

    @patch.object(Asset, 'get_local_source_path', return_value='/fake')
    @patch.object(OfficeImporter, '_is_content_extractable', return_value=True)
    def test_get_request_body_with_content_page_dpi(self, _, __):
        self.asset.set_attr('media.clip.start', 12)
        processor = self.init_processor(OfficeImporter(), {'extract_content': True,
                                                           'proxy_dpi': 150})
        body = processor._get_request_body(self.asset)
        assert body['content'] == 'true'
        assert body['page'] == 12
        assert body['dpi'] == 150

    @patch.object(OfficeImporter, '_get_request_body', return_value={})
    @patch.object(OfficeImporter, '_post_to_service')
    def test_render_outputs(self, post_mock, __):
        response_mock = Mock()
        response_mock.json = Mock(return_value={'output': '/fake'})
        post_mock.return_value = response_mock
        processor = self.init_processor(OfficeImporter(), {})
        output = processor._render_outputs(self.asset)
        assert output == '/fake'

    @patch.object(OfficeImporter, '_get_request_body', return_value={})
    @patch.object(OfficeImporter, '_post_to_service',
                  side_effect=requests.exceptions.HTTPError)
    def test_render_outputs_exception(self, _, __):
        processor = self.init_processor(OfficeImporter(), {})
        with pytest.raises(UnrecoverableProcessorException):
            processor._render_outputs(self.asset)

    @patch.object(OfficeImporter, '_load_metadata',
                  return_value={'author': 'Zach', 'content': 'temp'})
    @patch.object(OfficeImporter, '_render_outputs', return_value='/fake')
    def test_process_loads_metadata_to_asset(self, _, __):
        processor = self.init_processor(OfficeImporter(), {'extract_pages': False})
        processor._process(Frame(self.asset))
        assert self.asset.get_attr('media.author') == 'Zach'
        assert self.asset.get_attr('media.content') == 'temp'

    @patch.object(OfficeImporter, '_load_metadata',
                  return_value={'author': 'Zach', 'content': 'temp'})
    @patch.object(OfficeImporter, '_render_outputs', return_value='/fake')
    def test_process_saves_previous_media_info(self, _, __):
        self.asset.set_attr('media.clip.test', 'test')
        processor = self.init_processor(OfficeImporter(), {'extract_pages': False})
        processor._process(Frame(self.asset))
        assert self.asset.get_attr('media.author') == 'Zach'
        assert self.asset.get_attr('media.content') == 'temp'
        assert self.asset.get_attr('media.clip.test') == 'test'

    @patch.object(OfficeImporter, '_load_metadata',
                  return_value={'author': 'Zach', 'content': 'temp'})
    @patch.object(OfficeImporter, '_render_outputs', return_value='/fake')
    def test_process_loads_metadata_but_no_content(self, _, __):
        processor = self.init_processor(OfficeImporter(), {'extract_pages': False,
                                                           'extract_content': False})
        processor._process(Frame(self.asset))
        assert self.asset.get_attr('media.author') == 'Zach'
        assert self.asset.get_attr('media.content') is None

    @patch.object(OfficeImporter, 'expand')
    @patch.object(OfficeImporter, '_load_metadata', return_value={'pages': 3})
    @patch.object(OfficeImporter, '_render_outputs', return_value='/fake')
    def test_process_expands_children(self, _, __, expand_patch):
        processor = self.init_processor(OfficeImporter(), {'extract_pages': True})
        processor._process(Frame(self.asset))
        assert expand_patch.call_count == 3

