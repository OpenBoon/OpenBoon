import logging
import unittest
import tempfile
from types import SimpleNamespace
from unittest.mock import patch

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
            'type': 'TF_UPLOADED_CLASSIFIER',
            'fileId': '/abc/123/345/foo.zip'
        }

    @patch.object(BoonClient, 'get')
    def test_get_model(self, get_patch):
        get_patch.return_value = self.model_data
        model = self.app.models.get_model('12345')
        self.assert_model(model)

    @patch.object(ModelApp, 'find_one_model')
    @patch.object(ModelApp, 'upload_trained_model_tf')
    def test_upload_trained_model_directory(self, upload_patch, model_patch):
        upload_patch.return_value = SimpleNamespace(**{'category': 'LabelDetection'})
        model_patch.return_value = SimpleNamespace(**self.model_data)

        tmp_dir = tempfile.mkdtemp()
        module = self.app.models.upload_trained_model('12345', tmp_dir, ["dog", "cat"])
        assert module.category == 'LabelDetection'

    @patch.object(BoonClient, 'send_file')
    def test_upload_trained_model_directory_tf(self, post_patch):
        post_patch.return_value = {'category': 'LabelDetection'}

        tmp_dir = tempfile.mkdtemp()
        module = self.app.models.upload_trained_model_tf('12345', tmp_dir, ["dog", "cat"])
        assert module.category == 'LabelDetection'

    @patch.object(BoonClient, 'send_file')
    def test_upload_trained_model_directory_pth(self, post_patch):
        post_patch.return_value = {'category': 'LabelDetection'}

        tmp_dir = tempfile.mkdtemp()
        module = self.app.models.upload_trained_model_tf('12345', tmp_dir, ["dog", "cat"])
        assert module.category == 'LabelDetection'

    @patch.object(BoonClient, 'send_file')
    def test_upload_trained_model_keras_inst(self, post_patch):
        class MockKerasModel:
            def save(self, path):
                pass

        post_patch.return_value = {'category': 'LabelDetection'}
        module = self.app.models.upload_trained_model_tf('12345', MockKerasModel(), ["dog", "cat"])
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
            "id": "12345",
            "name": "Train model"
        }
        post_patch.return_value = job_data
        model = Model(self.model_data)
        job = self.app.models.train_model(model, foo='bar')
        assert job_data['id'] == job.id
        assert job_data['name'] == job.name

    @patch.object(BoonClient, 'post')
    def test_deploy_model(self, post_patch):
        job_data = {
            "id": "12345",
            "name": "job-foo-bar"
        }
        post_patch.return_value = job_data
        model = Model(self.model_data)
        mod = self.app.models.deploy_model(model)
        assert job_data['id'] == mod.id
        assert job_data['name'] == mod.name

    def assert_model(self, model):
        assert self.model_data['id'] == model.id
        assert self.model_data['name'] == model.name
        assert self.model_data['type'] == model.type.name
        assert self.model_data['fileId'] == model.file_id

    @patch.object(BoonClient, 'get')
    def test_get_label_counts(self, get_patch):
        value = {
            'dog': 1,
            'cat': 2
        }
        get_patch.return_value = value
        rsp = self.app.models.get_label_counts(Model({'id': 'foo'}))
        assert value == rsp

    @patch.object(BoonClient, 'put')
    def test_rename_label(self, put_patch):
        value = {
            'updated': 1
        }
        put_patch.return_value = value
        rsp = self.app.models.rename_label(Model({'id': 'foo'}), 'dog', 'cat')
        assert value == rsp

    @patch.object(BoonClient, 'delete')
    def test_delete_label(self, put_patch):
        value = {
            'updated': 1
        }
        put_patch.return_value = value
        rsp = self.app.models.delete_label(Model({'id': 'foo'}), 'dog')
        assert value == rsp

    @patch.object(BoonClient, 'get')
    def test_download_labeled_images(self, get_patch):
        raw = {'id': '12345', 'type': 'TF_CLASSIFIER'}
        model = Model(raw)
        get_patch.return_value = raw
        dl = self.app.models.download_labeled_images(model, 'objects_coco', '/tmp/dstest')
        assert '/tmp/dstest' == dl.dst_dir
        assert '12345' == dl.model.id

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
