import logging
import unittest
from unittest.mock import patch

from boonai import ZmlpClient, BoonAiApp, DataSource

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class ZmlpDataSourceAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.key_dict = {
            'projectId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'keyId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'sharedKey': 'test123test135'
        }
        self.app = BoonAiApp(self.key_dict)

    @patch.object(ZmlpClient, 'post')
    def test_create_datasource(self, post_patch):
        value = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'uri': 'gs://test/test',
            'file_types': ['jpg'],
            'modules': ['google-ocr']
        }
        post_patch.return_value = value
        ds = self.app.datasource.create_datasource('test', 'gs://test/test')
        assert value['id'] == ds.id
        assert value['name'] == ds.name
        assert value['uri'] == ds.uri
        assert ds.file_types == ['jpg']
        assert ds.modules == ['google-ocr']

    @patch.object(ZmlpClient, 'post')
    def test_get_datasource(self, post_patch):
        value = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'uri': 'gs://test/test'
        }
        post_patch.return_value = value
        ds = self.app.datasource.get_datasource('test')
        assert value['id'] == ds.id
        assert value['name'] == ds.name
        assert value['uri'] == ds.uri

    @patch.object(ZmlpClient, 'post')
    def test_import_files(self, post_patch):
        value = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'Import DataSource'
        }
        post_patch.return_value = value
        job = self.app.datasource.import_files(DataSource({'id': '123'}))
        assert value['id'] == job.id
        assert value['name'] == job.name

    @patch.object(ZmlpClient, 'delete')
    def test_delete_ds(self, post_patch):
        value = {'type': 'DataSource',
                 'id': 'bfdb2d1c-597f-16a9-b0a9-0242ac15000a',
                 'op': 'delete', 'success': True}
        post_patch.return_value = value
        rsp = self.app.datasource.delete_datasource(DataSource({'id': '123'}))
        assert value['id'] == rsp['id']
        assert value['type'] == 'DataSource'
