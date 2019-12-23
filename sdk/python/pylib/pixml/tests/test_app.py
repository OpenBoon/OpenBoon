import base64
import json
import logging
import os
import unittest

import pixml.app

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class PixmlAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.key_dict = {
            'projectId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'keyId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'sharedKey': 'test123test135'
        }
        self.key_str = base64.b64encode(json.dumps(self.key_dict).encode())

    def test_create_app_with_key_dict(self):
        app = pixml.app.PixmlApp(self.key_dict)
        assert app.client
        assert app.client.apikey
        assert app.client.headers()

    def test_create_app_with_key_str(self):
        app = pixml.app.PixmlApp(self.key_str)
        assert app.client
        assert app.client.apikey
        assert app.client.headers()

    def test_create_app_from_env(self):
        server = "https://localhost:9999"
        os.environ['PIXML_APIKEY'] = self.key_str.decode()
        os.environ['PIXML_SERVER'] = server
        try:
            app1 = pixml.app.app_from_env()
            # Assert we can sign a request
            assert app1.client.headers()
            assert app1.client.server == server
        finally:
            del os.environ['PIXML_APIKEY']
            del os.environ['PIXML_SERVER']

    def test_create_app_from_file_env(self):
        server = "https://localhost:9999"
        os.environ['PIXML_APIKEY_FILE'] = os.path.dirname(__file__) + "/test_key.json"
        os.environ['PIXML_SERVER'] = server
        try:
            app1 = pixml.app.app_from_env()
            # Assert we can sign a request
            assert app1.client.headers()
            assert app1.client.server == server
        finally:
            del os.environ['PIXML_APIKEY_FILE']
            del os.environ['PIXML_SERVER']
