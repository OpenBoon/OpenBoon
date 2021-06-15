import logging
import unittest
from unittest.mock import patch

from boonsdk import BoonClient, ModelType, Model, Dataset
from .util import get_boon_app

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class DatasetAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.app = get_boon_app()

        self.ds_data = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'type': 'Classification'
        }

    def test_make_label(self):
        ds = Dataset({'id': '12345'})
        label = ds.make_label('dog', bbox=[0.1, 0.1, 0.5, 0.5], simhash='ABC1234')
        assert 'dog' == label.label
        assert [0.1, 0.1, 0.5, 0.5] == label.bbox
        assert 'ABC1234' == label.simhash

    @patch.object(BoonClient, 'post')
    def test_find_one_dataset(self, post_patch):
        post_patch.return_value = self.ds_data
        model = self.app.datasets.find_one_dataset(id="12345")
        self.assert_ds(model)

    @patch.object(BoonClient, 'post')
    def test_find_dataset(self, post_patch):
        post_patch.return_value = {"list": [self.ds_data]}
        models = list(self.app.datasets.find_datasets(id="12345", limit=1))
        self.assert_ds(models[0])

    @patch.object(BoonClient, 'post')
    def test_create_dataset(self, post_patch):
        post_patch.return_value = self.ds_data
        model = self.app.datasets.create_dataset('test', ModelType.TF_CLASSIFIER)
        self.assert_ds(model)

    @patch.object(BoonClient, 'get')
    def test_get_label_counts_from_model(self, get_patch):
        value = {
            'dog': 1,
            'cat': 2
        }
        get_patch.return_value = value
        rsp = self.app.datasets.get_label_counts(Model({'id': 'foo', 'datasetId': '123'}))
        assert value == rsp

    @patch.object(BoonClient, 'get')
    def test_get_label_counts(self, get_patch):
        value = {
            'dog': 1,
            'cat': 2
        }
        get_patch.return_value = value
        rsp = self.app.datasets.get_label_counts(Dataset(self.ds_data))
        assert value == rsp

    @patch.object(BoonClient, 'put')
    def test_rename_label(self, put_patch):
        value = {
            'updated': 1
        }
        put_patch.return_value = value
        rsp = self.app.datasets.rename_label(Dataset({'id': 'foo'}), 'dog', 'cat')
        assert value == rsp

    @patch.object(BoonClient, 'delete')
    def test_delete_label(self, put_patch):
        value = {
            'updated': 1
        }
        put_patch.return_value = value
        rsp = self.app.datasets.delete_label(Dataset({'id': 'foo'}), 'dog')
        assert value == rsp

    def assert_ds(self, ds):
        assert self.ds_data['id'] == ds.id
        assert self.ds_data['name'] == ds.name
        assert self.ds_data['type'] == ds.type.name
