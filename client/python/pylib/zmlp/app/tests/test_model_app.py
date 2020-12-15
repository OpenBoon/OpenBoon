import logging
import unittest
import tempfile
from unittest.mock import patch

from zmlp import ZmlpClient, ModelType, Model
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
            'type': 'ZVI_LABEL_DETECTION',
            'fileId': '/abc/123/345/foo.zip'
        }

    @patch.object(ZmlpClient, 'get')
    def test_get_model(self, get_patch):
        get_patch.return_value = self.model_data
        model = self.app.models.get_model('12345')
        self.assert_model(model)

    @patch.object(ZmlpClient, 'send_file')
    def test_upload_custom_model(self, post_patch):
        post_patch.return_value = {'category': 'model'}

        tmp_dir = tempfile.mkdtemp()
        model_file = self.app.models.upload_custom_model('12345', tmp_dir,
                                                         ["dog", "cat"])
        assert model_file.category == 'model'

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
        model = self.app.models.create_model('test', ModelType.ZVI_LABEL_DETECTION)
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

    @patch.object(ZmlpClient, 'get')
    def test_get_label_counts(self, get_patch):
        value = {
            'dog': 1,
            'cat': 2
        }
        get_patch.return_value = value
        rsp = self.app.models.get_label_counts(Model({'id': 'foo'}))
        assert value == rsp

    @patch.object(ZmlpClient, 'put')
    def test_rename_label(self, put_patch):
        value = {
            'updated': 1
        }
        put_patch.return_value = value
        rsp = self.app.models.rename_label(Model({'id': 'foo'}), 'dog', 'cat')
        assert value == rsp

    @patch.object(ZmlpClient, 'delete')
    def test_delete_label(self, put_patch):
        value = {
            'updated': 1
        }
        put_patch.return_value = value
        rsp = self.app.models.delete_label(Model({'id': 'foo'}), 'dog')
        assert value == rsp

    @patch.object(ZmlpClient, 'get')
    def test_download_labeled_images(self, get_patch):
        raw = {'id': '12345', 'type': 'ZVI_LABEL_DETECTION'}
        model = Model(raw)
        get_patch.return_value = raw
        dl = self.app.models.download_labeled_images(model, 'objects_coco', '/tmp/dstest')
        assert '/tmp/dstest' == dl.dst_dir
        assert '12345' == dl.model.id

    @patch.object(ZmlpClient, 'get')
    def test_get_model_type_info(self, get_patch):
        raw = {
            'name': 'ZVI_LABEL_DETECTION',
            'description': 'a description',
            'objective': 'label detection',
            'provider': 'zorroa',
            'minConcepts': 1,
            'minExamples': 1
        }
        get_patch.return_value = raw

        props = self.app.models.get_model_type_info(ModelType.ZVI_LABEL_DETECTION)
        assert props.name == "ZVI_LABEL_DETECTION"
        assert props.description == 'a description'
        assert props.objective == 'label detection'
        assert props.provider == 'zorroa'
        assert props.min_concepts == 1
        assert props.min_examples == 1

    @patch.object(ZmlpClient, 'get')
    def test_get_all_model_type_info(self, get_patch):
        raw = {
            'name': 'ZVI_LABEL_DETECTION',
            'description': 'a description',
            'objective': 'label detection',
            'provider': 'zorroa',
            'minConcepts': 1,
            'minExamples': 1
        }
        get_patch.return_value = [raw]

        props = self.app.models.get_all_model_type_info()[0]
        assert props.name == "ZVI_LABEL_DETECTION"
        assert props.description == 'a description'
        assert props.objective == 'label detection'
        assert props.provider == 'zorroa'
        assert props.min_concepts == 1
        assert props.min_examples == 1
