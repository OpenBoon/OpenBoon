import pytest
from django.urls import reverse
from rest_framework import status
from unittest.mock import patch, Mock
from zmlp import ZmlpClient
from zmlp.app import ModelApp, AssetApp
from zmlp.entity import LabelScope

from models.views import ModelViewSet
from wallet.tests.utils import check_response

pytestmark = pytest.mark.django_db


@pytest.fixture
def model_fields():
    return ['id', 'name', 'type', 'moduleName', 'fileId', 'trainingJobName',
            'ready', 'deploySearch', 'timeCreated', 'timeModified', 'actorCreated',
            'actorModified', 'url', 'runningJobId']


class TestModelViewSetList:

    def test_list(self, login, project, api_client, monkeypatch, model_fields):

        def mock_response(*args, **kwargs):
            return {'list': [{'id': 'b9c52abf-9914-1020-b9f0-0242ac12000a', 'projectId': '00000000-0000-0000-0000-000000000000', 'type': 'ZVI_LABEL_DETECTION', 'name': 'Labeller', 'moduleName': 'zvi-labeller-label-detection', 'fileId': 'models/b9c52abf-9914-1020-b9f0-0242ac12000a/zvi-labeller-label-detection/zvi-labeller-label-detection.zip', 'trainingJobName': 'Train Labeller / zvi-labeller-label-detection', 'ready': False, 'deploySearch': {'query': {'match_all': {}}}, 'timeCreated': 1594678625043, 'timeModified': 1594678625043, 'actorCreated': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000'}, {'id': 'b9c52abe-9914-1020-b9f0-0242ac12000a', 'projectId': '00000000-0000-0000-0000-000000000000', 'type': 'ZVI_KNN_CLASSIFIER', 'name': 'MyClassifier', 'moduleName': 'zvi-myclassifier-cluster', 'fileId': 'models/b9c52abe-9914-1020-b9f0-0242ac12000a/zvi-myclassifier-cluster/zvi-myclassifier-cluster.zip', 'trainingJobName': 'Train MyClassifier / zvi-myclassifier-cluster', 'ready': False, 'deploySearch': {'query': {'match_all': {}}}, 'timeCreated': 1594676501554, 'timeModified': 1594676501554, 'actorCreated': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000'}], 'page': {'from': 0, 'size': 50, 'disabled': False, 'totalCount': 2}}  # noqa

        def job_response(*args, **kwargs):
            return []

        path = reverse('model-list', kwargs={'project_pk': project.id})
        monkeypatch.setattr(ZmlpClient, 'post', mock_response)
        monkeypatch.setattr(ZmlpClient, 'iter_paged_results', job_response)
        response = api_client.get(path)
        content = check_response(response)
        results = content['results']
        assert len(results) == 2
        assert results[0]['name'] == 'Labeller'
        assert results[0]['type'] == 'ZVI_LABEL_DETECTION'
        assert results[0]['runningJobId'] == ''
        assert set(model_fields) == set(results[0].keys())


class TestModelViewSetRetrieve:

    def test_retrieve(self, login, project, api_client, monkeypatch, model_fields):

        def mock_response(*args, **kwrags):
            return {'id': 'b9c52abf-9914-1020-b9f0-0242ac12000a', 'projectId': '00000000-0000-0000-0000-000000000000', 'type': 'ZVI_LABEL_DETECTION', 'name': 'Labeller', 'moduleName': 'zvi-labeller-label-detection', 'fileId': 'models/b9c52abf-9914-1020-b9f0-0242ac12000a/zvi-labeller-label-detection/zvi-labeller-label-detection.zip', 'trainingJobName': 'Train Labeller / zvi-labeller-label-detection', 'ready': False, 'deploySearch': {'query': {'match_all': {}}}, 'timeCreated': 1594678625043, 'timeModified': 1594678625043, 'actorCreated': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000'}  # noqa

        def job_response(*args, **kwargs):
            return []

        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        path = reverse('model-detail', kwargs={'project_pk': project.id,
                                               'pk': model_id})
        monkeypatch.setattr(ZmlpClient, 'get', mock_response)
        monkeypatch.setattr(ZmlpClient, 'iter_paged_results', job_response)
        response = api_client.get(path)
        content = check_response(response)
        assert content['id'] == model_id
        model_fields.remove('url')
        assert set(model_fields) == set(content.keys())


class TestModelViewSetDestroy:

    def test_destroy(self, login, project, api_client, monkeypatch, model_fields):

        def mock_response(*args, **kwrags):
            return {'type': 'model', 'id': 'b9c52abf-9914-1020-b9f0-0242ac12000a', 'op': 'delete', 'success': True}  # noqa

        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        path = reverse('model-detail', kwargs={'project_pk': project.id,
                                               'pk': model_id})
        monkeypatch.setattr(ZmlpClient, 'delete', mock_response)
        response = api_client.delete(path)
        content = check_response(response)
        assert content['id'] == model_id
        assert content['success']


class TestModelViewSetCreate:

    def test_create(self, login, project, api_client, monkeypatch, model_fields):

        def mock_response(*args, **kwargs):
            return {'id': '78c536eb-6c26-160c-9389-0242ac12000a', 'projectId': '00000000-0000-0000-0000-000000000000', 'type': 'ZVI_KNN_CLASSIFIER', 'name': 'Test Model', 'moduleName': 'zvi-test-model-cluster', 'fileId': 'models/78c536eb-6c26-160c-9389-0242ac12000a/zvi-test-model-cluster/zvi-test-model-cluster.zip', 'trainingJobName': 'Train Test Model / zvi-test-model-cluster', 'ready': False, 'deploySearch': {'query': {'match_all': {}}}, 'timeCreated': 1594770525976, 'timeModified': 1594770525976, 'actorCreated': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000'}  # noqa

        body = {'name': 'Test Model',
                'type': 'ZVI_KNN_CLASSIFIER'}
        path = reverse('model-list', kwargs={'project_pk': project.id})
        monkeypatch.setattr(ZmlpClient, 'post', mock_response)
        response = api_client.post(path, body)
        content = check_response(response, status.HTTP_201_CREATED)
        results = content['results']
        assert results['name'] == 'Test Model'
        assert results['type'] == 'ZVI_KNN_CLASSIFIER'
        fields = ['id', 'projectId', 'type', 'name', 'moduleName', 'fileId', 'trainingJobName',
                  'ready', 'deploySearch', 'timeCreated', 'timeModified', 'actorCreated',
                  'actorModified']
        assert set(fields) == set(results.keys())


class TestModelViewSetActions:

    def test_model_types_empty(self, login, project, api_client, monkeypatch):

        def mock_response(*args, **kwargs):
            return []

        path = reverse('model-model-types', kwargs={'project_pk': project.id})
        monkeypatch.setattr(ZmlpClient, 'get', mock_response)
        response = api_client.get(path)
        content = check_response(response)
        assert content['results'] == []

    def test_model_types(self, login, project, api_client, monkeypatch):

        def mock_response(*args, **kwargs):
            return [{'name': 'ZVI_KNN_CLASSIFIER', 'description': 'Classify images or documents using a KNN classifier.  This type of model generates a single prediction which can be used to quickly organize assets into general groups.The KNN classifier works with just a single image and label.', 'objective': 'Label Detection', 'provider': 'Zorroa', 'deployOnTrainingSet': True, 'minConcepts': 1, 'minExamples': 1}, {'name': 'ZVI_LABEL_DETECTION', 'description': 'Classify images or documents using a custom strained CNN deep learning algorithm.  This type of modelgenerates multiple predictions and can be trained to identify very specific features. The label detection classifier requires at least 2 concepts with 10 labeled images each. ', 'objective': 'Label Detection', 'provider': 'Zorroa', 'deployOnTrainingSet': False, 'minConcepts': 2, 'minExamples': 10}, {'name': 'ZVI_FACE_RECOGNITION', 'description': 'Relabel existing ZVI faces using a KNN Face Recognition model.', 'objective': 'Face Recognition', 'provider': 'Zorroa', 'deployOnTrainingSet': True, 'minConcepts': 1, 'minExamples': 1}, {'name': 'GCP_LABEL_DETECTION', 'description': 'Utilize Google AutoML to train an image classifier.', 'objective': 'Label Detection', 'provider': 'Google', 'deployOnTrainingSet': True, 'minConcepts': 2, 'minExamples': 10}]  # noqa

        path = reverse('model-model-types', kwargs={'project_pk': project.id})
        monkeypatch.setattr(ZmlpClient, 'get', mock_response)
        response = api_client.get(path)
        content = check_response(response)
        results = content['results']
        assert len(results) == 4
        assert results[0]['name'] == 'ZVI_KNN_CLASSIFIER'

    def test_train(self, login, project, api_client):
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        path = reverse('model-train', kwargs={'project_pk': project.id,
                                              'pk': model_id})
        with patch.object(ModelApp, 'train_model') as train:
            with patch.object(ModelViewSet, '_get_model') as get_model:
                model = Mock()
                get_model.return_value = model
                train.return_value = Mock(_data={})
                response = api_client.post(path)
        check_response(response)
        train.assert_called_once_with(model, deploy=False)
        get_model.assert_called_once()

    def test_train_deploy(self, login, project, api_client):
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        path = reverse('model-train', kwargs={'project_pk': project.id,
                                              'pk': model_id})
        with patch.object(ModelApp, 'train_model') as train:
            with patch.object(ModelViewSet, '_get_model') as get_model:
                model = Mock()
                get_model.return_value = model
                train.return_value = Mock(_data={})
                response = api_client.post(path, {'deploy': True})
        check_response(response)
        train.assert_called_once_with(model, deploy=True)
        get_model.assert_called_once()

    def test_get_labels(self, login, project, api_client, monkeypatch):

        def mock_response(*args, **kwargs):
            return {'Mountains': 8}

        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        path = reverse('model-get-labels', kwargs={'project_pk': project.id,
                                                   'pk': model_id})
        monkeypatch.setattr(ZmlpClient, 'get', mock_response)
        response = api_client.get(path)
        content = check_response(response)
        assert content == {'count': 1,
                           'results': [{'label': 'Mountains', 'count': 8}]}

    def test_rename_label(self, login, project, api_client, monkeypatch):
        def mock_response(*args, **kwargs):
            return {'updated': 26}

        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        monkeypatch.setattr(ZmlpClient, 'put', mock_response)
        path = reverse('model-rename-label', kwargs={'project_pk': project.id,
                                                     'pk': model_id})
        response = api_client.put(path, {'label': 'Dog', 'newLabel': 'Cat'})
        content = check_response(response)
        assert content == {'updated': 26}

    def test_destroy_label(self, login, project, api_client, monkeypatch):
        def mock_response(*args, **kwargs):
            return {'updated': 1}

        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        monkeypatch.setattr(ZmlpClient, 'delete', mock_response)
        path = reverse('model-destroy-label', kwargs={'project_pk': project.id,
                                                      'pk': model_id})
        response = api_client.delete(path, {'label': 'Dog'})
        content = check_response(response)
        assert content == {'updated': 1}


class TestLabelingEndpoints:

    @pytest.fixture
    def add_body(self):
        return {
            "addLabels": [
                {"assetId": "eicS1V9d1hBpOGFC0Zo1TB1OSt0Yrrtl",
                 "label": "Mountains",
                 "scope": "TRAIN"},
                {"assetId": "vKZkTwYjd0zSPpPipAVS5BkLakSeOjzH",
                 "label": "Mountains",
                 "scope": "TEST"}
            ]
        }

    @pytest.fixture
    def remove_body(self):
        return {
            "removeLabels": [
                {"assetId": "eicS1V9d1hBpOGFC0Zo1TB1OSt0Yrrtl",
                 "label": "Mountains",
                 "scope": "TRAIN"
                 },
                {"assetId": "vKZkTwYjd0zSPpPipAVS5BkLakSeOjzH",
                 "label": "Mountains",
                 "scope": "TEST"
                 }
            ]
        }

    def test_add_labels(self, login, project, api_client, add_body):
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        make_label_mock = Mock(return_value=Mock())
        model = Mock(make_label=make_label_mock)
        path = reverse('model-add-labels', kwargs={'project_pk': project.id,
                                                   'pk': model_id})
        with patch.object(ModelViewSet, '_get_model', return_value=model) as get_model:
            with patch.object(AssetApp, 'get_asset'):
                with patch.object(AssetApp, 'update_labels') as update_labels:
                    response = api_client.post(path, add_body)
                    check_response(response, status=status.HTTP_201_CREATED)
        get_model.assert_called_once()
        assert update_labels.call_count == 2
        model.make_label.assert_called_with('Mountains', bbox=None,
                                            scope=LabelScope.TEST, simhash=None)

    def test_update_labels(self, login, project, api_client, add_body, remove_body):
        add_body.update(remove_body)
        body = add_body
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        make_label_mock = Mock(return_value=Mock())
        model = Mock(make_label=make_label_mock)
        path = reverse('model-update-labels', kwargs={'project_pk': project.id,
                                                      'pk': model_id})
        with patch.object(ModelViewSet, '_get_model', return_value=model) as get_model:
            with patch.object(AssetApp, 'get_asset'):
                with patch.object(AssetApp, 'update_labels') as update_labels:
                    response = api_client.post(path, body)
                    check_response(response, status=status.HTTP_200_OK)
        get_model.assert_called_once()
        # This is due to the mocked calls being condensed down to 1 "asset"
        assert update_labels.call_count == 1
        model.make_label.assert_called_with('Mountains', bbox=None,
                                            scope=LabelScope.TEST, simhash=None)

    def test_delete_labels(self, login, project, api_client, remove_body):
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        make_label_mock = Mock(return_value=Mock())
        model = Mock(make_label=make_label_mock)
        path = reverse('model-delete-labels', kwargs={'project_pk': project.id,
                                                      'pk': model_id})
        with patch.object(ModelViewSet, '_get_model', return_value=model) as get_model:
            with patch.object(AssetApp, 'get_asset'):
                with patch.object(AssetApp, 'update_labels') as update_labels:
                    response = api_client.delete(path, remove_body)
                    check_response(response)
        get_model.assert_called_once()
        assert update_labels.call_count == 2
        model.make_label.assert_called_with('Mountains', bbox=None,
                                            scope=LabelScope.TEST, simhash=None)
