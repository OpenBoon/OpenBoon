import logging
import unittest
from unittest.mock import patch

from boonsdk import BoonClient, ModelType, Model
from .util import get_boon_app

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class DataSetAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.app = get_boon_app()

        self.ds_data = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'type': 'Classification'
        }

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
    def test_get_label_counts(self, get_patch):
        value = {
            'dog': 1,
            'cat': 2
        }
        get_patch.return_value = value
        rsp = self.app.datasets.get_label_counts(Model({'id': 'foo'}))
        assert value == rsp

    @patch.object(BoonClient, 'put')
    def test_rename_label(self, put_patch):
        value = {
            'updated': 1
        }
        put_patch.return_value = value
        rsp = self.app.datasets.rename_label(Model({'id': 'foo'}), 'dog', 'cat')
        assert value == rsp

    @patch.object(BoonClient, 'delete')
    def test_delete_label(self, put_patch):
        value = {
            'updated': 1
        }
        put_patch.return_value = value
        rsp = self.app.datasets.delete_label(Model({'id': 'foo'}), 'dog')
        assert value == rsp

    def assert_ds(self, ds):
        assert self.ds_data['id'] == ds.id
        assert self.ds_data['name'] == ds.name
        assert self.ds_data['type'] == ds.type.name
