import os
import unittest
import json

from zmlpsdk.testing import TestAsset, zorroa_test_data
from zmlp_core.office.oclient import OfficerClient


class OfficerPythonClientTests(unittest.TestCase):

    def setUp(self):
        self.path = zorroa_test_data('office/pdfTest.pdf')
        self.asset = TestAsset(str(self.path), id="abcdefg1234")
        self.local_test = None  # 'ws://localhost:7078'
        os.environ['ZMLP_JOB_ID'] = "abc123"
        os.environ['ZORROA_JOB_STORAGE_PATH'] = "/projects/foo"

    def tearDown(self):
        del os.environ['ZMLP_JOB_ID']
        del os.environ['ZORROA_JOB_STORAGE_PATH']

    def test_service_url(self):
        client = OfficerClient()
        assert client.url == 'ws://officer:7078'

    def test_render_url(self):
        client = OfficerClient()
        assert client.render_url == 'ws://officer:7078/render'

    def test_render(self):
        client = OfficerClient(self.local_test)
        result = client.render(self.asset, 1, False)
        assert result == '/projects/foo/officer/abcdefg1234'

    def test_get_render_request_body(self):
        client = OfficerClient(self.local_test)
        body = client._get_render_request_body(self.asset, None, True)
        assert body['file']
        assert body['body']

        assert "/office/pdfTest.pdf" in json.loads(body['body'])['fileName']
        assert "/projects/foo/officer/abcdefg1234" in json.loads(body['body'])["outputPath"]
        assert -1 == json.loads(body['body'])["page"]
        assert json.loads(body['body'])["disableImageRender"]

    def test_get_render_request_body_clip(self):
        client = OfficerClient(self.local_test)
        body = client._get_render_request_body(self.asset, 5, False)

        assert body['file']
        assert '/office/pdfTest.pdf' in json.loads(body['body'])['fileName']
        assert body['body']

        assert "/office/pdfTest.pdf" in json.loads(body['body'])['fileName']
        assert "/projects/foo/officer/abcdefg1234" in json.loads(body['body'])["outputPath"]
        assert 5 == json.loads(body['body'])["page"]
        assert not json.loads(body['body'])["disableImageRender"]

    def test_get_cache_location_true(self):
        client = OfficerClient(self.local_test)
        location = client.get_cache_location(self.asset, 1)
        assert location == '/projects/foo/officer/abcdefg1234'

    def test_get_cache_location_false(self):
        not_rendered_asset = zorroa_test_data('office/simple.pdf')
        client = OfficerClient(self.local_test)
        assert client.get_cache_location(not_rendered_asset, 1) is None
