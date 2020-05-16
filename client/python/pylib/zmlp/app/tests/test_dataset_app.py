import logging
import unittest
from unittest.mock import patch

from zmlp import ZmlpClient, ZmlpApp, DataSetType, DataSet

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

key_dict = {
    'projectId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
    'keyId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
    'accessKey': 'test123test135',
    'secretKey': 'test123test135'
}


class ZmlpDataSetAppTests(unittest.TestCase):

    def setUp(self):
        self.app = ZmlpApp(key_dict)

    @patch.object(ZmlpClient, 'post')
    def test_create_dataset(self, post_patch):
        value = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'type': 'LABEL_DETECTION'
        }
        post_patch.return_value = value
        ds = self.app.datasets.create_dataset('test', DataSetType.LABEL_DETECTION)
        assert value['id'] == ds.id
        assert value['name'] == ds.name
        assert DataSetType.LABEL_DETECTION == ds.type

    @patch.object(ZmlpClient, 'get')
    def test_get_dataset(self, get_patch):
        value = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'type': 'LABEL_DETECTION'
        }
        get_patch.return_value = value
        ds = self.app.datasets.get_dataset('A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80')
        assert value['id'] == ds.id
        assert value['name'] == ds.name
        assert DataSetType.LABEL_DETECTION == ds.type

    @patch.object(ZmlpClient, 'post')
    def test_find_one_dataset(self, post_patch):
        value = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'type': 'LABEL_DETECTION'
        }
        post_patch.return_value = value
        ds = self.app.datasets.find_one_dataset(id='A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80')
        assert value['id'] == ds.id
        assert value['name'] == ds.name
        assert DataSetType.LABEL_DETECTION == ds.type

    @patch.object(ZmlpClient, 'post')
    def test_find_datasets(self, post_patch):
        value = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'type': 'LABEL_DETECTION'
        }
        post_patch.return_value = {"list": [value]}
        ds = list(self.app.datasets.find_datasets(
            id='A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80', limit=1))
        assert value['id'] == ds[0].id
        assert value['name'] == ds[0].name
        assert DataSetType.LABEL_DETECTION == ds[0].type

    @patch.object(ZmlpClient, 'get')
    def test_get_label_counts(self, get_patch):
        value = {
            "dog": 1,
            "cat": 2
        }
        get_patch.return_value = value
        rsp = self.app.datasets.get_label_counts(DataSet({"id": "foo"}))
        assert value == rsp

    @patch.object(ZmlpClient, 'get')
    def test_get_dataset_downloader(self, get_patch):
        ds_raw = {"id": "12345", "type": "LABEL_DETECTION"}
        ds = DataSet(ds_raw)
        get_patch.return_value = ds_raw
        dl = self.app.datasets.get_dataset_downloader(ds, "objects_coco", "/tmp/dstest")
        assert "/tmp/dstest" == dl.dst_dir
        assert "12345" == dl.dataset.id
