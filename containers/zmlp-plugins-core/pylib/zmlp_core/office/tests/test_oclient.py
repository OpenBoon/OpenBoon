import os
import unittest
from unittest.mock import patch

from pathlib import Path

from zmlpsdk.storage import file_storage
from zmlpsdk.testing import TestAsset, MockRequestsResponse, zorroa_test_data
from zmlp_core.office.oclient import OfficerClient


class OfficerPythonClientTests(unittest.TestCase):

    def setUp(self):
        self.path = Path('/tmp/path/file.pdf')
        self.asset = TestAsset(str(self.path), id="abcdefg1234")
        os.environ['ZMLP_JOB_ID'] = "abc123"
        os.environ['ZORROA_JOB_STORAGE_PATH'] = "/projects/foo"

    def tearDown(self):
        del os.environ['ZMLP_JOB_ID']
        del os.environ['ZORROA_JOB_STORAGE_PATH']

    def test_service_url(self):
        client = OfficerClient()
        assert client.url == 'http://officer:7078'

    def test_render_url(self):
        client = OfficerClient()
        assert client.render_url == 'http://officer:7078/render'

    @patch('requests.post')
    @patch.object(file_storage, 'localize_file')
    def test_render(self, file_cache_patch, post_patch):
        post_patch.return_value = MockRequestsResponse(
            {"location": "zmlp://ml-storage/foo/bar"}, 200)
        file_cache_patch.return_value = zorroa_test_data('office/pdfTest.pdf', False)
        client = OfficerClient()
        result = client.render(self.asset, 1, False)
        assert result == "zmlp://ml-storage/foo/bar"

    @patch('requests.post')
    @patch.object(file_storage, 'localize_file')
    def test_get_render_request_body(self, file_cache_patch, post_patch):
        post_patch.return_value = MockRequestsResponse(
            {"output": "zmlp://ml-storage/foo/bar"}, 200)
        file_cache_patch.return_value = zorroa_test_data('office/pdfTest.pdf', False)
        client = OfficerClient()
        body = client._get_render_request_body(self.asset, None, True)
        assert body[0][0] == 'file'
        assert body[0][1][0] == '/tmp/path/file.pdf'
        assert body[1][0] == 'body'
        assert body[1][1][0] is None

        assert '"fileName": "/tmp/path/file.pdf"' in body[1][1][1]
        assert '"outputPath": "/projects/foo/officer/abcdefg1234"' in body[1][1][1]
        assert '"page": -1' in body[1][1][1]
        assert '"disableImageRender": true' in body[1][1][1]

    @patch('requests.post')
    @patch.object(file_storage, 'localize_file')
    def test_get_render_request_body_clip(self, file_cache_patch, post_patch):
        post_patch.return_value = MockRequestsResponse(
            {"location": "zmlp://ml-storage/foo/bar"}, 200)
        file_cache_patch.return_value = zorroa_test_data('office/pdfTest.pdf', False)
        client = OfficerClient()
        body = client._get_render_request_body(self.asset, 5, False)

        assert body[0][0] == 'file'
        assert body[0][1][0] == '/tmp/path/file.pdf'
        assert body[1][0] == 'body'
        assert body[1][1][0] is None

        assert '"fileName": "/tmp/path/file.pdf"' in body[1][1][1]
        assert '"outputPath": "/projects/foo/officer/abcdefg1234"' in body[1][1][1]
        assert '"page": 5' in body[1][1][1]
        assert '"disableImageRender": false' in body[1][1][1]

    @patch('requests.post')
    def test_get_cache_location_true(self, post_patch):
        post_patch.return_value = MockRequestsResponse({"location": "/foo"}, 200)
        client = OfficerClient()
        assert client.get_cache_location(self.asset, 1) == "/foo"
        args = post_patch.call_args_list[0][1]
        assert args['json']['outputPath'] == '/projects/foo/officer/abcdefg1234'
        assert args['json']['page'] == 1
        assert args['headers']['Content-Type'] == 'application/json'

    @patch('requests.post')
    def test_get_cache_location_false(self, post_patch):
        post_patch.return_value = MockRequestsResponse("", 404)
        client = OfficerClient()
        assert client.get_cache_location(self.asset, 1) is None
        args = post_patch.call_args_list[0][1]
        assert args['json']['outputPath'] == '/projects/foo/officer/abcdefg1234'
        assert args['json']['page'] == 1
        assert args['headers']['Content-Type'] == 'application/json'
