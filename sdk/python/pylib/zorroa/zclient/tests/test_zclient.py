# flake8: noqa
import os
import unittest
from unittest.mock import patch, Mock

import pytest
import requests

import zorroa.zclient
from zorroa import ZmlpClient


class ZmlpClientTestsTests(unittest.TestCase):
    base64_key = 'ewogICAgImtleUlkIjogIjViODZjNDk0LTQ3NGYtNGFiYS05NTM2LTgzMmQxNjE3ZThiYiIsCiAgICAic2hhcmVkS2V5IjogImNhOTAyOWE0YTU5ZDVhODZmY2E0MDU0NWI3MGI3MTQ1Yzc4Mjg3YzdkYWU5ZDdkMjMzZWM1NzJlYjllYjM5ZjBhYmZiZjczOTIyZmVlYmJiMDU2YWE1Mzk0ZTE0YjhkYjgyMWNhMjlkMGY1N2IxZjBlZjJkMjE0YTlhOWQ1NTkzIgp9Cg=='
    dict_key = {
        'keyId': '5b86c494-474f-4aba-9536-832d1617e8bb',
        'sharedKey': 'ca9029a4a59d5a86fca40545b70b7145c78287c7dae9d7d233ec572eb9eb39f0abfbf73922feebbb056aa5394e14b8db821ca29d0f57b1f0ef2d214a9a9d5593'
    }

    def test_init_with_env_base64_apikey(self):
        os.environ['ZMLP_APIKEY'] = self.base64_key
        os.environ['ZMLP_SERVER'] = "http://foo/bar"
        try:
            client = zorroa.zclient.from_env()
            assert client.headers()['Authorization'].startswith(
                'Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9')
            assert client.server == "http://foo/bar"
        finally:
            del os.environ['ZMLP_APIKEY']
            del os.environ['ZMLP_SERVER']

    def test_init_from_env_file_api_key(self):
        os.environ['ZMLP_APIKEY_FILE'] = "test-key.json"
        try:
            client = zorroa.zclient.from_env()
            assert client.headers()['Authorization'].startswith(
                'Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9')
        finally:
            del os.environ['ZMLP_APIKEY_FILE']

    def test_init_with_base64_apikey(self):
        client = ZmlpClient(self.base64_key)
        assert client.headers()['Authorization'].startswith(
            'Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9')

    def test_init_with_dict_api_key(self):
        client = ZmlpClient(self.dict_key)
        assert client.headers()['Authorization'].startswith(
            'Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9')

    def test_init_with_file_api_key(self):
        client = ZmlpClient(open('test-key.json'))
        assert client.headers()['Authorization'].startswith(
            'Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9')

    def mock_rsp(self, status, body={}):
        mock_rsp = Mock()
        mock_rsp.status_code = status
        mock_rsp.content = body
        mock_rsp.json = lambda: body
        return mock_rsp

    @patch.object(requests, 'get')
    def test_get(self, mock_req):
        body = {"a": "b"}
        mock_req.return_value = self.mock_rsp(200, {"a": "b"})
        client = ZmlpClient(self.base64_key)
        assert body == client.get("/any")

    @patch.object(requests, 'post')
    def test_post(self, mock_req):
        body = {"a": "b"}
        mock_req.return_value = self.mock_rsp(200, {"a": "b"})
        client = ZmlpClient(self.base64_key)
        assert body == client.post("/any")

    @patch.object(requests, 'put')
    def test_put(self, mock_req):
        body = {"a": "b"}
        mock_req.return_value = self.mock_rsp(200, {"a": "b"})
        client = ZmlpClient(self.base64_key)
        assert body == client.put("/any")

    @patch.object(requests, 'delete')
    def test_delete(self, mock_req):
        body = {"a": "b"}
        mock_req.return_value = self.mock_rsp(200, {"a": "b"})
        client = ZmlpClient(self.base64_key)
        assert body == client.delete("/any")

    @patch.object(requests, 'get')
    def test_translate_404(self, mock_get):
        mock_get.return_value = self.mock_rsp(404)
        client = ZmlpClient(self.base64_key)
        with pytest.raises(zorroa.zclient.ZmlpNotFoundException):
            client.get("/foo/bar")

    @patch.object(requests, 'get')
    def test_translate_409(self, mock_get):
        mock_get.return_value = self.mock_rsp(409)
        client = ZmlpClient(self.base64_key)
        with pytest.raises(zorroa.zclient.ZmlpDuplicateException):
            client.get("/foo/bar")

    @patch.object(requests, 'get')
    def test_translate_500(self, mock_get):
        mock_get.return_value = self.mock_rsp(500)
        client = ZmlpClient(self.base64_key)
        with pytest.raises(zorroa.zclient.ZmlpInvalidRequestException):
            client.get("/foo/bar")

    @patch.object(requests, 'get')
    def test_translate_400(self, mock_get):
        mock_get.return_value = self.mock_rsp(400)
        client = ZmlpClient(self.base64_key)
        with pytest.raises(zorroa.zclient.ZmlpInvalidRequestException):
            client.get("/foo/bar")

    @patch.object(requests, 'get')
    def test_translate_401(self, mock_get):
        mock_get.return_value = self.mock_rsp(401)
        client = ZmlpClient(self.base64_key)
        with pytest.raises(zorroa.zclient.ZmlpSecurityException):
            client.get("/foo/bar")

    @patch.object(requests, 'get')
    def test_translate_403(self, mock_get):
        mock_get.return_value = self.mock_rsp(403)
        client = ZmlpClient(self.base64_key)
        with pytest.raises(zorroa.zclient.ZmlpSecurityException):
            client.get("/foo/bar")
