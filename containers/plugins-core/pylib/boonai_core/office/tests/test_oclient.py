import os
import unittest
import json
from unittest.mock import patch

from boonflow.testing import TestAsset, test_data
from boonai_core.office.oclient import OfficerClient


class OfficerPythonClientTests(unittest.TestCase):

    def setUp(self):
        self.path = test_data('office/pdfTest.pdf'.format(__file__))
        self.asset = TestAsset(str(self.path), id="abcdefg1234")
        self.local_test = None  # 'ws://localhost:7078'
        os.environ['BOONAI_JOB_ID'] = "abc123"
        os.environ['BOONAI_JOB_STORAGE_PATH'] = "/projects/foo"

    def tearDown(self):
        del os.environ['BOONAI_JOB_ID']
        del os.environ['BOONAI_JOB_STORAGE_PATH']

    def test_service_url(self):
        client = OfficerClient(self.local_test)
        assert client.url == 'ws://officer:7078'

    def test_render_url(self):
        client = OfficerClient(self.local_test)
        assert client.render_url == 'ws://officer:7078/render'

    @patch.object(OfficerClient, 'render')
    def test_render(self, client):
        client.render.return_value = '/projects/foo/officer/abcdefg1234'

        result = client.render(self.asset, 1, False)
        assert result == '/projects/foo/officer/abcdefg1234'

    @patch.object(OfficerClient, '_get_render_request_body')
    def test_get_render_request_body_clip(self, client):
        client._get_render_request_body.return_value = self._response_body()
        body = client._get_render_request_body(self.asset, None, True)
        assert body['file']
        assert body['body']

        assert "/office/pdfTest.pdf" in json.loads(body['body'])['fileName']
        assert "/projects/foo/officer/abcdefg1234" in json.loads(body['body'])["outputPath"]
        assert -1 == json.loads(body['body'])["page"]
        assert json.loads(body['body'])["disableImageRender"]

    @patch.object(OfficerClient, '_get_render_request_body')
    def test_get_cache_location_true(self, client):
        client.get_cache_location.return_value = '/projects/foo/officer/abcdefg1234'

        location = client.get_cache_location(self.asset, 1)
        assert location == '/projects/foo/officer/abcdefg1234'

    @patch.object(OfficerClient, '_get_render_request_body')
    def test_get_cache_location_false(self, client):
        client.get_cache_location.return_value = None

        not_rendered_asset = test_data('office/simple.pdf')
        assert client.get_cache_location(not_rendered_asset, 1) is None

    def _response_body(self):
        return {
            'file': 'base64VeryBigEncodedFileblablabla',
            'body': '{"fileName": '
                    '"/Users/ironaraujo/Projects/zorroa/zmlp/test-data/office/pdfTest.pdf", '
                    '"outputPath": '
                    '"/projects/foo/officer/abcdefg1234", "page": -1, "disableImageRender": true, '
                    '"dpi": 150} '
        }
