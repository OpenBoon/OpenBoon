import unittest
from unittest.mock import patch

from pathlib2 import Path

from pixml.analysis.storage import file_cache
from pixml.analysis.testing import TestAsset, MockRequestsResponse, zorroa_test_data
from pixml_core.office.oclient import OfficerClient


class OfficerPythonClientTests(unittest.TestCase):

    def setUp(self):
        self.path = Path('/tmp/path/file.pdf')
        self.asset = TestAsset(str(self.path), id="abcdefg1234")

    def test_service_url(self):
        client = OfficerClient()
        assert client.url == 'http://officer:7078'

    def test_render_url(self):
        client = OfficerClient()
        assert client.render_url == 'http://officer:7078/render'

    @patch('requests.post')
    @patch.object(file_cache, 'localize_remote_file')
    def test_render(self, file_cache_patch, post_patch):
        post_patch.return_value = MockRequestsResponse(
            {"output": "pixml://ml-storage/foo/bar"}, 200)
        file_cache_patch.return_value = zorroa_test_data('office/pdfTest.pdf', False)
        client = OfficerClient()
        result = client.render(self.asset, 1)
        assert result == "pixml://ml-storage/foo/bar"

    @patch('requests.post')
    @patch.object(file_cache, 'localize_remote_file')
    def test_get_render_request_body(self, file_cache_patch, post_patch):
        post_patch.return_value = MockRequestsResponse(
            {"output": "pixml://ml-storage/foo/bar"}, 200)
        file_cache_patch.return_value = zorroa_test_data('office/pdfTest.pdf', False)
        client = OfficerClient()
        body = client._get_render_request_body(self.asset, None)

        assert body[0][0] == 'file'
        assert body[0][1][0] == '/tmp/path/file.pdf'
        assert body[1][0] == 'body'
        assert body[1][1][0] is None

        assert '"fileName": "/tmp/path/file.pdf"' in body[1][1][1]
        assert '"outputDir": "abcdefg1234"' in body[1][1][1]
        assert '"page": -1' in body[1][1][1]

    @patch('requests.post')
    @patch.object(file_cache, 'localize_remote_file')
    def test_get_render_request_body_clip(self, file_cache_patch, post_patch):
        post_patch.return_value = MockRequestsResponse(
            {"output": "pixml://ml-storage/foo/bar"}, 200)
        file_cache_patch.return_value = zorroa_test_data('office/pdfTest.pdf', False)
        client = OfficerClient()
        body = client._get_render_request_body(self.asset, 5)

        assert body[0][0] == 'file'
        assert body[0][1][0] == '/tmp/path/file.pdf'
        assert body[1][0] == 'body'
        assert body[1][1][0] is None

        assert '"fileName": "/tmp/path/file.pdf"' in body[1][1][1]
        assert '"outputDir": "abcdefg1234"' in body[1][1][1]
        assert '"page": 5' in body[1][1][1]

    @patch('requests.post')
    def test_exists_true(self, post_patch):
        post_patch.return_value = MockRequestsResponse("", 200)
        client = OfficerClient()
        assert client.exists(self.asset, 1) is True
        args = post_patch.call_args_list[0][1]
        assert args['json']['outputDir'] == 'abcdefg1234'
        assert args['json']['page'] == 1
        assert args['headers']['Content-Type'] == 'application/json'

    @patch('requests.post')
    def test_exists_false(self, post_patch):
        post_patch.return_value = MockRequestsResponse("", 404)
        client = OfficerClient()
        assert client.exists(self.asset, 1) is False
        args = post_patch.call_args_list[0][1]
        assert args['json']['outputDir'] == 'abcdefg1234'
        assert args['json']['page'] == 1
        assert args['headers']['Content-Type'] == 'application/json'
