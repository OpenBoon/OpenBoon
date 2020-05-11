import logging
import unittest
from unittest.mock import patch

from zmlp import ZmlpClient, DataSet, ModelType, Model
from .util import get_zmlp_app

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class ModelAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.app = get_zmlp_app()

        self.model_data = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'dataSetId': 'abc123',
            'type': 'LABEL_DETECTION_MOBILENET2',
            'fileId': '/abc/123/345/foo.zip'
        }

    @patch.object(ZmlpClient, 'get')
    def test_get_model(self, get_patch):
        get_patch.return_value = self.model_data
        model = self.app.models.get_model('12345')
        self.assert_model(model)

    @patch.object(ZmlpClient, 'post')
    def test_find_one_model(self, post_patch):
        post_patch.return_value = self.model_data
        model = self.app.models.find_one_model(id="12345")
        self.assert_model(model)

    @patch.object(ZmlpClient, 'post')
    def test_find_models(self, post_patch):
        post_patch.return_value = {"list": [self.model_data]}
        models = list(self.app.models.find_models(id="12345", limit=1))
        self.assert_model(models[0])

    @patch.object(ZmlpClient, 'post')
    def test_create_model(self, post_patch):
        post_patch.return_value = self.model_data
        ds = DataSet({"id": "12345"})
        model = self.app.models.create_model(ds, ModelType.LABEL_DETECTION_MOBILENET2)
        self.assert_model(model)

    @patch.object(ZmlpClient, 'post')
    def test_train_model(self, post_patch):
        job_data = {
            "id": "12345",
            "name": "Train model"
        }
        post_patch.return_value = job_data
        model = Model(self.model_data)
        job = self.app.models.train_model(model, foo='bar')
        assert job_data['id'] == job.id
        assert job_data['name'] == job.name

    @patch.object(ZmlpClient, 'post')
    def test_publish_model(self, post_patch):
        mod_data = {
            "id": "12345",
            "name": "foo-bar"
        }
        post_patch.return_value = mod_data
        model = Model(self.model_data)
        mod = self.app.models.publish_model(model)
        assert mod_data['id'] == mod.id
        assert mod_data['name'] == mod.name

    def assert_model(self, model):
        assert self.model_data['id'] == model.id
        assert self.model_data['name'] == model.name
        assert self.model_data['dataSetId'] == model.dataset_id
        assert self.model_data['type'] == model.type.name
        assert self.model_data['fileId'] == model.file_id
