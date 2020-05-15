import logging
import unittest
import tempfile
import os
import json
from unittest.mock import patch

from zmlp import ZmlpClient, ZmlpApp, DataSetType, DataSet
from zmlp.app import AssetApp, DataSetApp
from zmlp.app.dataset_app import DataSetDownloader

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


class ZmlpDataSetDownloader(unittest.TestCase):

    def setUp(self):
        self.app = ZmlpApp(key_dict)

    @patch.object(DataSetApp, 'get_dataset')
    @patch.object(ZmlpClient, 'get')
    def test_setup_label_detection_structure(self, get_patch, data_dataset_patch):
        data_dataset_patch.return_value = DataSet({"id": "12345", "type": "LabelDetection"})
        get_patch.return_value = {
            "goats": 100,
            "hobbits": 12,
            "wizards": 45,
            "dwarfs": 9
        }
        d = tempfile.mkdtemp()
        dsl = DataSetDownloader(self.app, "12345", d)
        dsl._setup_label_detection_structure()

        dirs = os.listdir(d)
        assert 'set_train' in dirs
        assert 'set_test' in dirs

        labels1 = os.listdir(d + "/set_train")
        assert 4 == len(labels1)
        assert ["dwarfs", "goats", "hobbits", "wizards"] == sorted(labels1)

        labels2 = os.listdir(d + "/set_test")
        assert 4 == len(labels2)
        assert ["dwarfs", "goats", "hobbits", "wizards"] == sorted(labels2)

    @patch.object(DataSetApp, 'get_dataset')
    @patch.object(AssetApp, 'download_file')
    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    @patch.object(ZmlpClient, 'get')
    def test_download_label_detection(
            self, get_patch, post_patch, del_patch, dl_patch, get_ds_patch):
        get_ds_patch.return_value = DataSet({"id": "12345", "type": "LABEL_DETECTION"})
        get_patch.return_value = {
            "goats": 100,
            "hobbits": 12,
            "wizards": 45,
            "dwarfs": 9
        }
        post_patch.side_effect = [mock_search_result_labels, {'hits': {'hits': []}}]
        del_patch.return_value = {}
        dl_patch.return_value = b'foo'

        d = tempfile.mkdtemp()
        dsl = DataSetDownloader(self.app, "12345", d)
        dsl.download()

    @patch.object(DataSetApp, 'get_dataset')
    @patch.object(AssetApp, 'download_file')
    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    @patch.object(ZmlpClient, 'get')
    def test_download_object_detection(
            self, get_patch, post_patch, del_patch, dl_patch, get_ds_patch):
        get_ds_patch.return_value = DataSet({"id": "12345", "type": "OBJECT_DETECTION"})

        post_patch.side_effect = [mock_search_result_objects, {'hits': {'hits': []}}]
        del_patch.return_value = {}
        dl_patch.return_value = b'foo'

        d = tempfile.mkdtemp()
        dsl = DataSetDownloader(self.app, "12345", d)
        dsl.download()
        with open(os.path.join(d, dsl.SET_TRAIN, "annotations.json")) as fp:
            train_annotations = json.load(fp)
        with open(os.path.join(d, dsl.SET_TEST, "annotations.json")) as fp:
            test_annotations = json.load(fp)

        assert 2 == len(train_annotations['images'])
        assert 1 == len(test_annotations['images'])

        assert 2 == len(train_annotations['categories'])
        assert 2 == len(test_annotations['categories'])

        assert 6 == len(train_annotations['annotations'])
        assert 2 == len(test_annotations['annotations'])


mock_search_result_objects = {
    'took': 4,
    'timed_out': False,
    '_scroll_id': "12345",
    'hits': {
        'total': {'value': 2},
        'max_score': 0.2876821,
        'hits': [
            {
                '_index': 'litvqrkus86sna2w',
                '_type': 'asset',
                '_id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                '_score': 0.2876821,
                '_source': {
                    'source': {
                        'path': 'https://i.imgur.com/SSN26nN.jpg'
                    },
                    "files": [
                        {
                            "id": "assets/123/proxy/proxy_400x400.jpg",
                            "category": "proxy",
                            "name": "proxy_400x400.jpg",
                            "mimetype": "image/jpeg",
                            "attrs": {
                                "width": 400,
                                "height": 400
                            }
                        }
                    ],
                    "labels": [
                        {
                            "dataSetId": "12345",
                            "label": "wizard",
                            "bbox": [0.5, 0.5, 0.6, 0.6]
                        },
                        {
                            "dataSetId": "12345",
                            "label": "dwarf",
                            "bbox": [0.0, 0.0, 0.3, 0.3]
                        },
                        {
                            "dataSetId": "12345",
                            "label": "wizard",
                            "bbox": [0.2, 0.5, 0.2, 0.6]
                        },
                        {
                            "dataSetId": "12345",
                            "label": "dwarf",
                            "bbox": [0.1, 0.3, 0.3, 0.4]
                        }
                    ]
                }
            },
            {
                '_index': 'litvqrkus86sna2w',
                '_type': 'asset',
                '_id': 'aabbccddec48n1q1fginVMV5yllhRRGx2WKyKLjDphg',
                '_score': 0.2876821,
                '_source': {
                    'source': {
                        'path': 'https://i.imgur.com/foo.jpg'
                    },
                    "files": [
                        {
                            "id": "assets/456/proxy/proxy_400x400.jpg",
                            "category": "proxy",
                            "name": "proxy_400x400.jpg",
                            "mimetype": "image/jpeg",
                            "attrs": {
                                "width": 400,
                                "height": 400
                            }
                        }
                    ],
                    "labels": [
                        {
                            "dataSetId": "12345",
                            "label": "wizard",
                            "bbox": [0.5, 0.5, 0.6, 0.6]
                        },
                        {
                            "dataSetId": "12345",
                            "label": "dwarf",
                            "bbox": [0.0, 0.0, 0.3, 0.3]
                        },
                        {
                            "dataSetId": "12345",
                            "label": "wizard",
                            "bbox": [0.2, 0.5, 0.2, 0.6]
                        },
                        {
                            "dataSetId": "12345",
                            "label": "dwarf",
                            "bbox": [0.1, 0.3, 0.3, 0.4]
                        }
                    ]
                }
            }
        ]
    }
}

mock_search_result_labels = {
    'took': 4,
    'timed_out': False,
    '_scroll_id': "12345",
    'hits': {
        'total': {'value': 2},
        'max_score': 0.2876821,
        'hits': [
            {
                '_index': 'litvqrkus86sna2w',
                '_type': 'asset',
                '_id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                '_score': 0.2876821,
                '_source': {
                    'source': {
                        'path': 'https://i.imgur.com/SSN26nN.jpg'
                    },
                    "files": [
                        {
                            "id": "assets/123/proxy/proxy_400x400.jpg",
                            "category": "proxy",
                            "name": "proxy_400x400.jpg",
                            "mimetype": "image/jpeg",
                            "attrs": {
                                "width": 400,
                                "height": 400
                            }
                        }
                    ],
                    "labels": [
                        {
                            "dataSetId": "12345",
                            "label": "wizard"
                        }
                    ]
                }
            },
            {
                '_index': 'litvqrkus86sna2w',
                '_type': 'asset',
                '_id': 'aabbccddec48n1q1fginVMV5yllhRRGx2WKyKLjDphg',
                '_score': 0.2876821,
                '_source': {
                    'source': {
                        'path': 'https://i.imgur.com/foo.jpg'
                    },
                    "files": [
                        {
                            "id": "assets/123/proxy/proxy_400x400.jpg",
                            "category": "proxy",
                            "name": "proxy_400x400.jpg",
                            "mimetype": "image/jpeg",
                            "attrs": {
                                "width": 400,
                                "height": 400
                            }
                        }
                    ],
                    "labels": [
                        {
                            "dataSetId": "12345",
                            "label": "hobbit"
                        }
                    ]
                }
            }
        ]
    }
}
