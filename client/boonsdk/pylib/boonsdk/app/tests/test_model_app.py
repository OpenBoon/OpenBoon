import logging
import tempfile
import unittest
import uuid
from unittest.mock import patch

import pytest

from boonsdk import BoonClient, ModelType, Model
from boonsdk.app import ModelApp
from .util import get_boon_app

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class ModelAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.app = get_boon_app()

        self.model_data = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'type': 'TF_SAVED_MODEL',
            'fileId': '/abc/123/345/foo.zip'
        }

        self.arg_schema = {
            "args": {
                "n_clusters": {
                    "type": "integer",
                    "defaultValue": 15,
                    "description": "Cluster groups"
                }
            }
        }

    @patch.object(BoonClient, 'get')
    def test_get_model(self, get_patch):
        get_patch.return_value = self.model_data
        model = self.app.models.get_model(str(uuid.uuid4()))
        self.assert_model(model)

    @patch.object(BoonClient, 'post')
    def test_get_model_by_name(self, get_patch):
        get_patch.return_value = self.model_data
        model = self.app.models.get_model('12345')
        self.assert_model(model)

    @patch.object(ModelApp, 'find_one_model')
    def test_upload_trained_model_wrong_type(self, model_patch):
        model_patch.return_value = Model({
            'id': '12345',
            'type': 'TF_CLASSIFIER',
            'name': 'foo'
        })

        tmp_dir = tempfile.mkdtemp()
        pytest.raises(ValueError,
                      self.app.models.upload_trained_model,
                      '12345', tmp_dir, ["dog", "cat"])

    @patch.object(ModelApp, 'find_one_model')
    @patch.object(BoonClient, 'send_file')
    def test_upload_trained_model_existing_labels(self, upload_patch, model_patch):
        upload_patch.return_value = {'category': 'LabelDetection'}
        model_patch.return_value = Model({
            'id': '12345',
            'type': 'TF_SAVED_MODEL',
            'name': 'foo'
        })

        tmp_dir = tempfile.mkdtemp()
        with open(f'{tmp_dir}/labels.txt', "w") as fp:
            fp.write("cat\n")
            fp.write("dog\n")

        module = self.app.models.upload_trained_model('12345', tmp_dir, None)
        assert module.category == 'LabelDetection'

    @patch.object(ModelApp, 'find_one_model')
    @patch.object(BoonClient, 'send_file')
    def test_upload_trained_model(self, upload_patch, model_patch):
        upload_patch.return_value = {'category': 'LabelDetection'}
        model_patch.return_value = Model({
            'id': '12345',
            'type': 'TF_SAVED_MODEL',
            'name': 'foo'
        })

        tmp_dir = tempfile.mkdtemp()
        module = self.app.models.upload_trained_model('12345', tmp_dir, ["dog", "cat"])
        assert module.category == 'LabelDetection'

    @patch.object(ModelApp, 'find_one_model')
    @patch.object(BoonClient, 'send_file')
    def test_upload_trained_model_directory_tf(self, post_patch, model_patch):
        post_patch.return_value = {'category': 'LabelDetection'}
        model_patch.return_value = Model({
            'id': '12345',
            'type': 'TF_SAVED_MODEL',
            'name': 'foo'
        })

        tmp_dir = tempfile.mkdtemp()
        module = self.app.models.upload_trained_model("12345", tmp_dir, ["dog", "cat"])
        assert module.category == 'LabelDetection'

    @patch.object(ModelApp, 'find_one_model')
    @patch.object(BoonClient, 'send_file')
    def test_upload_trained_model_directory_pth(self, post_patch, model_patch):
        post_patch.return_value = {'category': 'LabelDetection'}
        model_patch.return_value = Model({
            'id': '12345',
            'type': 'TORCH_MAR_CLASSIFIER',
            'name': 'foo'
        })

        tmp_dir = tempfile.mkdtemp()
        module = self.app.models.upload_trained_model("12345", tmp_dir, ["dog", "cat"])
        assert module.category == 'LabelDetection'

    @patch.object(BoonClient, 'post')
    def test_find_one_model(self, post_patch):
        post_patch.return_value = self.model_data
        model = self.app.models.find_one_model(id="12345")
        self.assert_model(model)

    @patch.object(BoonClient, 'post')
    def test_find_models(self, post_patch):
        post_patch.return_value = {"list": [self.model_data]}
        models = list(self.app.models.find_models(id="12345", limit=1))
        self.assert_model(models[0])

    @patch.object(BoonClient, 'post')
    def test_create_model(self, post_patch):
        post_patch.return_value = self.model_data
        model = self.app.models.create_model('test', ModelType.TF_CLASSIFIER)
        self.assert_model(model)

    @patch.object(BoonClient, 'post')
    def test_train_model(self, post_patch):
        job_data = {
            'id': '12345',
            'name': 'Train model'
        }
        post_patch.return_value = job_data
        model = Model(self.model_data)
        job = self.app.models.train_model(model)
        assert job_data['id'] == job.id
        assert job_data['name'] == job.name

    @patch.object(BoonClient, 'get')
    def test_get_model_version_tags(self, get_patch):
        get_patch.return_value = ['model', 'latest']
        model = Model(self.model_data)
        tags = self.app.models.get_model_version_tags(model)
        assert tags == ['model', 'latest']

    @patch.object(BoonClient, 'post')
    def test_apply_model(self, post_patch):
        job_data = {
            'id': '12345',
            'name': 'job-foo-bar'
        }
        post_patch.return_value = job_data
        model = Model(self.model_data)
        mod = self.app.models.apply_model(model)
        assert job_data['id'] == mod.id
        assert job_data['name'] == mod.name

    @patch.object(BoonClient, 'post')
    def test_test_model(self, post_patch):
        job_data = {
            'id': '12345',
            'name': 'job-foo-bar'
        }
        post_patch.return_value = job_data
        model = Model(self.model_data)
        mod = self.app.models.test_model(model)
        assert job_data['id'] == mod.id
        assert job_data['name'] == mod.name

    @patch.object(BoonClient, 'get')
    def test_get_model_type_info(self, get_patch):
        raw = {
            'name': 'TF_CLASSIFIER',
            'description': 'a description',
            'objective': 'label detection',
            'provider': 'boonai',
            'minConcepts': 1,
            'minExamples': 1
        }
        get_patch.return_value = raw

        props = self.app.models.get_model_type_info(ModelType.TF_CLASSIFIER)
        assert props.name == "TF_CLASSIFIER"
        assert props.description == 'a description'
        assert props.objective == 'label detection'
        assert props.provider == 'boonai'
        assert props.min_concepts == 1
        assert props.min_examples == 1

    @patch.object(BoonClient, 'get')
    def test_get_all_model_type_info(self, get_patch):
        raw = {
            'name': 'TF_CLASSIFIER',
            'description': 'a description',
            'objective': 'label detection',
            'provider': 'boonai',
            'minConcepts': 1,
            'minExamples': 1
        }
        get_patch.return_value = [raw]

        props = self.app.models.get_all_model_type_info()[0]
        assert props.name == "TF_CLASSIFIER"
        assert props.description == 'a description'
        assert props.objective == 'label detection'
        assert props.provider == 'boonai'
        assert props.min_concepts == 1
        assert props.min_examples == 1

    @patch.object(BoonClient, 'get')
    def test_export_trained_model(self, get_patch):
        data = b'some_data'
        mockresponse = unittest.mock.Mock()
        mockresponse.content = data
        get_patch.return_value = mockresponse

        model = Model(self.model_data)
        size = self.app.models.export_trained_model(model, '/tmp/model.zip')
        assert size == 9

    @patch.object(BoonClient, 'post')
    def test_approve_model(self, post_patch):
        post_patch.return_value = {'success': True}
        raw = {'id': '12345', 'type': 'TF_CLASSIFIER'}
        model = Model(raw)

        rsp = self.app.models.approve_model(model)
        assert rsp['success']

    @patch.object(BoonClient, 'get')
    def test_get_model_type_training_args(self, get_patch):
        get_patch.return_value = self.arg_schema
        rsp = self.app.models.get_model_type_training_args(ModelType.KNN_CLASSIFIER)
        assert rsp == self.arg_schema

    @patch.object(BoonClient, 'get')
    def test_get_model_training_args(self, get_patch):
        get_patch.return_value = {"n_clusters": 5}
        model = Model({'id': '12345', 'type': 'TF_CLASSIFIER'})
        rsp = self.app.models.get_training_args(model)
        assert rsp == {"n_clusters": 5}

    @patch.object(BoonClient, 'patch')
    def test_set_training_arg(self, get_patch):
        get_patch.return_value = {"n_clusters": 3}
        model = Model({'id': '12345', 'type': 'TF_CLASSIFIER'})
        rsp = self.app.models.set_training_arg(model, 'n_clusters', 3)
        assert rsp == {"n_clusters": 3}

    @patch.object(BoonClient, 'put')
    def test_set_training_args(self, put_patch):
        put_patch.return_value = {"n_clusters": 5}
        model = Model({'id': '12345', 'type': 'TF_CLASSIFIER'})
        rsp = self.app.models.set_training_args(model, {"n_clusters": 5})
        assert rsp == {"n_clusters": 5}

    def assert_model(self, model):
        assert self.model_data['id'] == model.id
        assert self.model_data['name'] == model.name
        assert self.model_data['type'] == model.type.name
        assert self.model_data['fileId'] == model.file_id
