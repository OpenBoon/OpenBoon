from unittest.mock import patch, Mock

import pytest
from django.test import override_settings
from django.urls import reverse
from rest_framework import status

from models.utils import ConfusionMatrix
from models.views import ModelViewSet, get_model_type_restrictions
from wallet.tests.utils import check_response
from boonsdk import BoonClient, Model
from boonsdk.app import ModelApp, DatasetApp
from boonsdk.entity import PostTrainAction

pytestmark = pytest.mark.django_db


@pytest.fixture
def model_fields():
    return ['id', 'name', 'type', 'moduleName', 'fileId', 'trainingJobName',
            'unappliedChanges', 'applySearch', 'timeCreated', 'timeModified', 'timeLastTested',
            'timeLastTrained', 'actorCreated', 'actorLastTested', 'actorLastTrained',
            'actorModified', 'link', 'projectId', 'datasetId', 'dependencies', 'state',
            'trainingArgs', 'trainingJobName', 'uploadable', 'description']


class TestGetModelTypeRestrictions:

    def test_no_label_counts(self):
        label_counts = {}
        min_concepts = 5
        min_examples = 10
        result = get_model_type_restrictions(label_counts, min_concepts, min_examples)
        assert result['missingLabels'] == 5
        assert result['missingLabelsOnAssets'] == 50

    def test_some_labels(self):
        label_counts = {'one': 10,
                        'two': 2}
        min_concepts = 5
        min_examples = 10
        result = get_model_type_restrictions(label_counts, min_concepts, min_examples)
        assert result['missingLabels'] == 3
        assert result['missingLabelsOnAssets'] == 38

    def test_my_spoon_is_too_big(self):
        label_counts = {'one': 12,
                        'two': 2,
                        'three': 22,
                        'four': 10,
                        'five': 10,
                        'six': 2}
        min_concepts = 5
        min_examples = 10
        result = get_model_type_restrictions(label_counts, min_concepts, min_examples)
        assert result['missingLabels'] == 0
        assert result['missingLabelsOnAssets'] == 16


class TestModelViewSetList:

    def test_list(self, login, project, api_client, monkeypatch, model_fields):

        def mock_response(*args, **kwargs):
            return {'list': [{'id': '6fec6366-3289-10ca-8ee0-c2dc28c54abf', 'description': 'a model', 'datasetId': '12345', 'projectId': '6892bd17-8660-49f5-be2a-843d87c47bb3', 'state': 'Trained', 'type': 'KNN_CLASSIFIER', 'name': 'foo', 'moduleName': 'foo', 'fileId': 'models/6fec6366-3289-10ca-8ee0-c2dc28c54abf/__TAG__/model.zip', 'trainingJobName': 'Training model: foo - [Label Detection]', 'ready': False, 'applySearch': {'query': {'match_all': {}}}, 'trainingArgs': {}, 'dependencies': [], 'timeCreated': 1627407276005, 'timeModified': 1627407276005, 'actorCreated': '07ae05fc-b764-41f6-96cc-e7d764f6e0e8/Admin Console Generated Key - bdcd0930-a569-4f3d-bf8e-d8838160937c - jbuhler@zorroa.com_6892bd17-8660-49f5-be2a-843d87c47bb3', 'actorModified': '07ae05fc-b764-41f6-96cc-e7d764f6e0e8/Admin Console Generated Key - bdcd0930-a569-4f3d-bf8e-d8838160937c - jbuhler@zorroa.com_6892bd17-8660-49f5-be2a-843d87c47bb3', 'timeLastTrained': 0, 'timeLastApplied': 0, 'timeLastTested': 0, 'timeLastUploaded': 0, 'timeLastDeployed': 0, 'uploadable': False}, {'id': '313e28b1-9f0e-1595-a6c0-ea38f4c81474', 'projectId': '6892bd17-8660-49f5-be2a-843d87c47bb3', 'datasetId': '313e28b1-9f0e-1595-a6c0-ea38f4c81474', 'state': 'Trained', 'type': 'TF_CLASSIFIER', 'name': 'tensorflow-quality', 'moduleName': 'tensorflow-quality', 'fileId': 'models/313e28b1-9f0e-1595-a6c0-ea38f4c81474/__TAG__/model.zip', 'trainingJobName': 'Training model: tensorflow-quality - [Label Detection]', 'ready': False, 'applySearch': {'query': {'match_all': {}}}, 'trainingArgs': {}, 'dependencies': [], 'timeCreated': 1621012834907, 'timeModified': 1621012834907, 'actorCreated': 'e41a24b1-b45a-4e9e-b716-9a9d89b32839/Admin Console Generated Key - d9997529-8822-44a1-9850-64880338094d - wallet-project-key-6892bd17-8660-49f5-be2a-843d87c47bb3', 'actorModified': 'e41a24b1-b45a-4e9e-b716-9a9d89b32839/Admin Console Generated Key - d9997529-8822-44a1-9850-64880338094d - wallet-project-key-6892bd17-8660-49f5-be2a-843d87c47bb3', 'timeLastTrained': 0, 'timeLastApplied': 0, 'timeLastTested': 0, 'timeLastUploaded': 0, 'timeLastDeployed': 0, 'uploadable': False}, {'id': 'ef965880-4559-10f2-801c-4a8fc1a4e308', 'projectId': '6892bd17-8660-49f5-be2a-843d87c47bb3', 'datasetId': 'ef965880-4559-10f2-801c-4a8fc1a4e308', 'state': 'Trained', 'type': 'KNN_CLASSIFIER', 'name': 'knn-quality', 'moduleName': 'knn-quality', 'fileId': 'models/ef965880-4559-10f2-801c-4a8fc1a4e308/__TAG__/model.zip', 'trainingJobName': 'Training model: knn-quality - [Label Detection]', 'ready': True, 'applySearch': {'query': {'match_all': {}}}, 'trainingArgs': {}, 'dependencies': [], 'timeCreated': 1616783169869, 'timeModified': 1616783169869, 'actorCreated': '931fd6fc-6538-48d8-b7f5-cb45613d9503/Admin Console Generated Key - c4dfc976-2df2-47f4-8cd6-ad5b57f1d558 - jbuhler@zorroa.com_6892bd17-8660-49f5-be2a-843d87c47bb3', 'actorModified': '931fd6fc-6538-48d8-b7f5-cb45613d9503/Admin Console Generated Key - c4dfc976-2df2-47f4-8cd6-ad5b57f1d558 - jbuhler@zorroa.com_6892bd17-8660-49f5-be2a-843d87c47bb3', 'timeLastTrained': 0, 'timeLastApplied': 0, 'timeLastTested': 0, 'timeLastUploaded': 0, 'timeLastDeployed': 0, 'uploadable': False}], 'page': {'from': 0, 'size': 20, 'disabled': False, 'totalCount': 3}}  # noqa

        def job_response(*args, **kwargs):
            return []

        path = reverse('model-list', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        monkeypatch.setattr(BoonClient, 'iter_paged_results', job_response)
        response = api_client.get(path)
        content = check_response(response)
        results = content['results']
        assert len(results) == 3
        assert results[0]['name'] == 'foo'
        assert results[0]['type'] == 'KNN_CLASSIFIER'
        assert results[0]['description'] == 'a model'
        assert set(model_fields) == set(results[0].keys())


class TestModelViewSetListAll:

    @override_settings(REST_FRAMEWORK={'PAGE_SIZE': 1})
    def test_list_all(self, login, project, api_client, monkeypatch, model_fields):

        def mock_response(*args, **kwargs):
            return {'list': [{'id': '6fec6366-3289-10ca-8ee0-c2dc28c54abf', 'datasetId': '12345', 'projectId': '6892bd17-8660-49f5-be2a-843d87c47bb3', 'state': 'Trained', 'type': 'KNN_CLASSIFIER', 'name': 'foo', 'moduleName': 'foo', 'fileId': 'models/6fec6366-3289-10ca-8ee0-c2dc28c54abf/__TAG__/model.zip', 'trainingJobName': 'Training model: foo - [Label Detection]', 'ready': False, 'applySearch': {'query': {'match_all': {}}}, 'trainingArgs': {}, 'dependencies': [], 'timeCreated': 1627407276005, 'timeModified': 1627407276005, 'actorCreated': '07ae05fc-b764-41f6-96cc-e7d764f6e0e8/Admin Console Generated Key - bdcd0930-a569-4f3d-bf8e-d8838160937c - jbuhler@zorroa.com_6892bd17-8660-49f5-be2a-843d87c47bb3', 'actorModified': '07ae05fc-b764-41f6-96cc-e7d764f6e0e8/Admin Console Generated Key - bdcd0930-a569-4f3d-bf8e-d8838160937c - jbuhler@zorroa.com_6892bd17-8660-49f5-be2a-843d87c47bb3', 'timeLastTrained': 0, 'timeLastApplied': 0, 'timeLastTested': 0, 'timeLastUploaded': 0, 'timeLastDeployed': 0, 'uploadable': False}], 'page': {'from': 0, 'size': 1, 'disabled': False, 'totalCount': 3}}  # noqa

        def job_response(*args, **kwargs):
            return []

        path = reverse('model-all', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        monkeypatch.setattr(BoonClient, 'iter_paged_results', job_response)
        response = api_client.get(path)
        content = check_response(response)
        results = content['results']
        assert len(results) == 3
        assert results[0]['name'] == 'foo'
        assert results[0]['type'] == 'KNN_CLASSIFIER'
        assert set(model_fields) == set(results[0].keys())


class TestModelViewSetRetrieve:
    def test_retrieve(self, login, project, api_client, monkeypatch, model_fields):
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'

        model_response = {'id': model_id, 'projectId': '4c40d135-e1cd-4206-b496-94f10ff00f64', 'datasetId': 'ea787a54-e5a9-11eb-b8e0-c696422502ba', 'state': 'Ready', 'type': 'KNN_CLASSIFIER', 'name': 'sail-no-sail', 'moduleName': 'sail-no-sail', 'fileId': 'models/ea129371-b978-1242-bf29-f6107c2f812a/__TAG__/model.zip', 'trainingJobName': 'Training model: sail-no-sail - [Label Detection]', 'ready': False, 'applySearch': {'query': {'match_all': {}}}, 'trainingArgs': {}, 'dependencies': [], 'timeCreated': 1627578639958, 'timeModified': 1627578646452, 'actorCreated': 'e171ba12-7cfe-41e0-a5e6-34a9f2e829c8/Admin Console Generated Key - d0365a16-f7f3-4a4e-815d-188d0479aa3e - wallet-project-key-4c40d135-e1cd-4206-b496-94f10ff00f64', 'actorModified': 'e171ba12-7cfe-41e0-a5e6-34a9f2e829c8/Admin Console Generated Key - d0365a16-f7f3-4a4e-815d-188d0479aa3e - wallet-project-key-4c40d135-e1cd-4206-b496-94f10ff00f64', 'timeLastTrained': 1627578913212, 'actorLastTrained': 'e171ba12-7cfe-41e0-a5e6-34a9f2e829c8/Admin Console Generated Key - d0365a16-f7f3-4a4e-815d-188d0479aa3e - wallet-project-key-4c40d135-e1cd-4206-b496-94f10ff00f64', 'timeLastTested': 1627578922702, 'actorLastTested': '6d9c8605-97b9-4906-9b8d-95c146f6f3af/job-runner', 'uploadable': False}  # noqa
        model_type_response = [{'name': 'KNN_CLASSIFIER', 'description': 'Classify images, documents and video clips using a KNN classifier.  This type of model can work great with just a single labeled example.If no labels are provided, the model automatically generates numbered groups of similar assets. These groups can be renamed and edited in subsequent training passes.', 'objective': 'Label Detection', 'provider': 'Boon AI', 'deployOnTrainingSet': True, 'minConcepts': 0, 'minExamples': 0, 'dependencies': [], 'label': 'K-Nearest Neighbors Classifier', 'datasetType': 'Classification'}, {'name': 'TF_CLASSIFIER', 'description': 'Classify images or documents using a custom strained CNN deep learning algorithm.  This type of modelgenerates multiple predictions and can be trained to identify very specific features. The label detection classifier requires at least 2 concepts with 10 labeled images each. ', 'objective': 'Label Detection', 'provider': 'Boon AI', 'deployOnTrainingSet': False, 'minConcepts': 2, 'minExamples': 10, 'dependencies': [], 'label': 'Tensorflow Transfer Learning Classifier', 'datasetType': 'Classification'}, {'name': 'FACE_RECOGNITION', 'description': 'Label faces detected by the boonai-face-detection module, and classify them with a KNN model. ', 'objective': 'Face Recognition', 'provider': 'Boon AI', 'deployOnTrainingSet': True, 'minConcepts': 1, 'minExamples': 1, 'dependencies': ['boonai-face-detection'], 'label': 'Face Recognition', 'datasetType': 'FaceRecognition'}, {'name': 'TORCH_MAR_CLASSIFIER', 'description': 'Upload a pre-trained Pytorch Model Archive', 'objective': 'Label Detection', 'provider': 'Boon AI', 'deployOnTrainingSet': True, 'minConcepts': 0, 'minExamples': 0, 'dependencies': [], 'label': 'A Torch Model Archive using the image_classifier handler.', 'datasetType': 'Classification'}, {'name': 'TORCH_MAR_DETECTOR', 'description': 'Upload a pre-trained Pytorch Model Archive', 'objective': 'Label Detection', 'provider': 'Boon AI', 'deployOnTrainingSet': True, 'minConcepts': 0, 'minExamples': 0, 'dependencies': [], 'label': 'A Torch Model Archive using the object_detector handler.', 'datasetType': 'Detection'}]  # noqa
        mock_get_responses = [model_type_response, model_response]

        def mock_response(*args, **kwrags):
            return mock_get_responses.pop()

        def job_response(*args, **kwargs):
            return []

        def model_info_response(*args, **kwargs):
            return Mock(min_concepts=2, min_examples=3)

        def label_counts_response(*args, **kwargs):
            return {'test': 2, 'tester': 3}

        path = reverse('model-detail', kwargs={'project_pk': project.id,
                                               'pk': model_id})
        monkeypatch.setattr(BoonClient, 'get', mock_response)
        monkeypatch.setattr(BoonClient, 'iter_paged_results', job_response)
        monkeypatch.setattr(ModelApp, 'get_model_type_info', model_info_response)
        monkeypatch.setattr(DatasetApp, 'get_label_counts', label_counts_response)
        response = api_client.get(path)
        content = check_response(response)
        assert content['id'] == model_id
        model_fields.remove('link')
        model_fields.extend(['runningJobId', 'modelTypeRestrictions'])
        assert (set(model_fields + ['timeLastTrained', 'timeLastApplied', 'timeLastTested',
                                    'datasetType']) == set(content.keys()))
        restrictions = content['modelTypeRestrictions']
        assert restrictions['missingLabels'] == 0
        assert restrictions['missingLabelsOnAssets'] == 1
        assert content['datasetType'] == 'Classification'
        assert content['timeLastTrained'] == 1627578913212


class TestModelViewSetDestroy:

    def test_destroy(self, login, project, api_client, monkeypatch, model_fields):

        def mock_response(*args, **kwrags):
            return {'type': 'model', 'id': 'b9c52abf-9914-1020-b9f0-0242ac12000a', 'op': 'delete', 'success': True}  # noqa

        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        path = reverse('model-detail', kwargs={'project_pk': project.id,
                                               'pk': model_id})
        monkeypatch.setattr(BoonClient, 'delete', mock_response)
        response = api_client.delete(path)
        check_response(response)


class TestModelViewSetUpdate:

    def test_update(self, login, project, api_client, monkeypatch, model_fields):

        def mock_response(*args, **kwrags):
            return {'type': 'model', 'id': 'b9c52abf-9914-1020-b9f0-0242ac12000a', 'op': 'delete', 'success': True}  # noqa

        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        path = reverse('model-detail', kwargs={'project_pk': project.id,
                                               'pk': model_id})
        monkeypatch.setattr(BoonClient, 'put', mock_response)
        response = api_client.put(path, {'datasetId': None, 'name': 'changed',
                                         'dependencies': []})
        check_response(response)

        response = api_client.put(path, {'name': 'changed'})
        check_response(response, status=400)


class TestModelViewSetCreate:

    def test_create(self, login, project, api_client, monkeypatch, model_fields):

        def mock_response(*args, **kwargs):
            return {'id': 'ebf3e4a6-458f-15d4-a6b1-aab1332fef21', 'projectId': '18e87cfe-23a0-4f62-973d-4e22f0f4b8d8', 'datasetId': 'ebf3e4a6-458f-15d4-a6b1-aab1332fef21', 'type': 'KNN_CLASSIFIER', 'name': 'Test Model', 'moduleName': 'knn', 'fileId': 'models/ebf3e4a6-458f-15d4-a6b1-aab1332fef21/__TAG__/model.zip', 'trainingJobName': 'Training model: knn - [Label Detection]', 'ready': False, 'applySearch': {'query': {'match_all': {}}}, 'trainingArgs': {}, 'timeCreated': 1619725616046, 'timeModified': 1619725616046, 'actorCreated': '9250c03e-a167-4cb9-a0fc-2198a1a00779/Admin Console Generated Key - 5f52268e-749c-4141-80b3-2fe4daa4552b - jbuhler@zorroa.com_18e87cfe-23a0-4f62-973d-4e22f0f4b8d8', 'actorModified': '9250c03e-a167-4cb9-a0fc-2198a1a00779/Admin Console Generated Key - 5f52268e-749c-4141-80b3-2fe4daa4552b - jbuhler@zorroa.com_18e87cfe-23a0-4f62-973d-4e22f0f4b8d8'}  # noqa

        body = {'name': 'Test Model',
                'type': 'ZVI_KNN_CLASSIFIER',
                'description': ''}
        path = reverse('model-list', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        response = api_client.post(path, body)
        content = check_response(response, status.HTTP_201_CREATED)
        assert content['name'] == 'Test Model'
        assert content['type'] == 'KNN_CLASSIFIER'
        fields = ['id', 'projectId', 'type', 'name', 'moduleName', 'fileId', 'trainingJobName',
                  'ready', 'applySearch', 'timeCreated', 'timeModified', 'actorCreated',
                  'actorModified', 'actorLastTrained', 'actorLastTested', 'datasetId',
                  'dependencies', 'state', 'trainingArgs', 'type', 'uploadable', 'description']
        assert set(fields) == set(content.keys())


class TestModelViewSetActions:

    def test_model_types_empty(self, login, project, api_client, monkeypatch):

        def mock_response(*args, **kwargs):
            return []

        path = reverse('model-model-types', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'get', mock_response)
        response = api_client.get(path)
        content = check_response(response)
        assert content['results'] == []

    def test_model_types(self, login, project, api_client, monkeypatch):

        def mock_response(*args, **kwargs):
            return [{'name': 'KNN_CLASSIFIER', 'description': 'Classify images, documents and video clips using a KNN classifier.  This type of model can work great with just a single labeled example.If no labels are provided, the model automatically generates numbered groups of similar assets. These groups can be renamed and edited in subsequent training passes.', 'objective': 'Label Detection', 'provider': 'Boon AI', 'deployOnTrainingSet': True, 'minConcepts': 0, 'minExamples': 0, 'dependencies': [], 'label': 'K-Nearest Neighbors Classifier', 'datasetType': 'Classification'}, {'name': 'TF_CLASSIFIER', 'description': 'Classify images or documents using a custom strained CNN deep learning algorithm.  This type of modelgenerates multiple predictions and can be trained to identify very specific features. The label detection classifier requires at least 2 concepts with 10 labeled images each. ', 'objective': 'Label Detection', 'provider': 'Boon AI', 'deployOnTrainingSet': False, 'minConcepts': 2, 'minExamples': 10, 'dependencies': [], 'label': 'Tensorflow Transfer Learning Classifier', 'datasetType': 'Classification'}, {'name': 'FACE_RECOGNITION', 'description': 'Label faces detected by the boonai-face-detection module, and classify them with a KNN model. ', 'objective': 'Face Recognition', 'provider': 'Boon AI', 'deployOnTrainingSet': True, 'minConcepts': 1, 'minExamples': 1, 'dependencies': ['boonai-face-detection'], 'label': 'Face Recognition', 'datasetType': 'FaceRecognition'}, {'name': 'TORCH_MAR_CLASSIFIER', 'description': 'Upload a pre-trained Pytorch Model Archive', 'objective': 'Label Detection', 'provider': 'Boon AI', 'deployOnTrainingSet': True, 'minConcepts': 0, 'minExamples': 0, 'dependencies': [], 'label': 'A Torch Model Archive using the image_classifier handler.', 'datasetType': 'Classification'}, {'name': 'TORCH_MAR_DETECTOR', 'description': 'Upload a pre-trained Pytorch Model Archive', 'objective': 'Label Detection', 'provider': 'Boon AI', 'deployOnTrainingSet': True, 'minConcepts': 0, 'minExamples': 0, 'dependencies': [], 'label': 'A Torch Model Archive using the object_detector handler.', 'datasetType': 'Detection'}]  # noqa

        path = reverse('model-model-types', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'get', mock_response)
        response = api_client.get(path)
        content = check_response(response)
        results = content['results']
        assert len(results) == 5
        assert results[0]['name'] == 'KNN_CLASSIFIER'
        assert results[0]['label'] == 'K-Nearest Neighbors Classifier'
        assert 'ZVI_FACE_RECOGNITION' not in [x['name'] for x in results]

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
        train.assert_called_once_with(model, post_action=PostTrainAction.NONE)
        get_model.assert_called_once()

    def test_train_apply(self, login, project, api_client):
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        path = reverse('model-train', kwargs={'project_pk': project.id,
                                              'pk': model_id})
        with patch.object(ModelApp, 'train_model') as train:
            with patch.object(ModelViewSet, '_get_model') as get_model:
                model = Mock()
                get_model.return_value = model
                train.return_value = Mock(_data={})
                response = api_client.post(path, {'apply': True})
        check_response(response)
        train.assert_called_once_with(model, post_action=PostTrainAction.APPLY)
        get_model.assert_called_once()

    def test_train_test(self, login, project, api_client):
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        path = reverse('model-train', kwargs={'project_pk': project.id,
                                              'pk': model_id})
        with patch.object(ModelApp, 'train_model') as train:
            with patch.object(ModelViewSet, '_get_model') as get_model:
                model = Mock()
                get_model.return_value = model
                train.return_value = Mock(_data={})
                response = api_client.post(path, {'test': True})
        check_response(response)
        train.assert_called_once_with(model, post_action=PostTrainAction.TEST)
        get_model.assert_called_once()

    def test_train_apply_and_test(self, login, project, api_client):
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        path = reverse('model-train', kwargs={'project_pk': project.id,
                                              'pk': model_id})
        with patch.object(ModelApp, 'train_model') as train:
            with patch.object(ModelViewSet, '_get_model') as get_model:
                model = Mock()
                get_model.return_value = model
                train.return_value = Mock(_data={})
                response = api_client.post(path, {'test': True, 'apply': True})
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == ['Cannot specify both test and apply, please pick one.']

    def test_test_lolz(self, login, project, api_client):
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        path = reverse('model-test', kwargs={'project_pk': project.id,
                                             'pk': model_id})
        with patch.object(ModelApp, 'test_model') as test:
            with patch.object(ModelViewSet, '_get_model') as get_model:
                model = Mock()
                get_model.return_value = model
                test.return_value = Mock(_data={})
                response = api_client.post(path)
        check_response(response)
        test.assert_called_once_with(model)
        get_model.assert_called_once()

    def test_apply_model(self, login, project, api_client):
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        path = reverse('model-apply', kwargs={'project_pk': project.id,
                                              'pk': model_id})
        with patch.object(ModelApp, 'apply_model') as test:
            with patch.object(ModelViewSet, '_get_model') as get_model:
                model = Mock()
                get_model.return_value = model
                test.return_value = Mock(_data={})
                response = api_client.post(path)
        check_response(response)
        test.assert_called_once_with(model)
        get_model.assert_called_once()

    def test_confusion_matrix_actions(self, login, project, api_client, monkeypatch):
        def mock_aggs(*args, **kwargs):
            return {'nested#nested_labels': {'doc_count': 1744, 'filter#model_train_labels': {'doc_count': 838, 'sterms#labels': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'bird', 'doc_count': 90, 'reverse_nested#predictions_by_label': {'doc_count': 90, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'Unrecognized', 'doc_count': 60}, {'key': 'bird', 'doc_count': 28}, {'key': 'frog', 'doc_count': 2}]}}}, {'key': 'deer', 'doc_count': 90, 'reverse_nested#predictions_by_label': {'doc_count': 90, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'deer', 'doc_count': 88}, {'key': 'Unrecognized', 'doc_count': 2}]}}}, {'key': 'dog', 'doc_count': 90, 'reverse_nested#predictions_by_label': {'doc_count': 90, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'dog', 'doc_count': 64}, {'key': 'Unrecognized', 'doc_count': 20}, {'key': 'cat', 'doc_count': 3}, {'key': 'horse', 'doc_count': 2}, {'key': 'deer', 'doc_count': 1}]}}}, {'key': 'frog', 'doc_count': 87, 'reverse_nested#predictions_by_label': {'doc_count': 87, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'frog', 'doc_count': 83}, {'key': 'Unrecognized', 'doc_count': 4}]}}}, {'key': 'cat', 'doc_count': 86, 'reverse_nested#predictions_by_label': {'doc_count': 86, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'cat', 'doc_count': 83}, {'key': 'Unrecognized', 'doc_count': 3}]}}}, {'key': 'ship', 'doc_count': 84, 'reverse_nested#predictions_by_label': {'doc_count': 84, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'ship', 'doc_count': 70}, {'key': 'Unrecognized', 'doc_count': 14}]}}}, {'key': 'horse', 'doc_count': 82, 'reverse_nested#predictions_by_label': {'doc_count': 82, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'horse', 'doc_count': 75}, {'key': 'Unrecognized', 'doc_count': 7}]}}}, {'key': 'truck', 'doc_count': 82, 'reverse_nested#predictions_by_label': {'doc_count': 82, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'truck', 'doc_count': 63}, {'key': 'Unrecognized', 'doc_count': 19}]}}}, {'key': 'airplane', 'doc_count': 75, 'reverse_nested#predictions_by_label': {'doc_count': 75, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'airplane', 'doc_count': 61}, {'key': 'Unrecognized', 'doc_count': 14}]}}}, {'key': 'automobile', 'doc_count': 72, 'reverse_nested#predictions_by_label': {'doc_count': 72, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'Unrecognized', 'doc_count': 38}, {'key': 'truck', 'doc_count': 24}, {'key': 'automobile', 'doc_count': 9}, {'key': 'frog', 'doc_count': 1}]}}}]}}}}  # noqa
        monkeypatch.setattr(ConfusionMatrix, '_ConfusionMatrix__get_confusion_matrix_aggregations',
                            mock_aggs)
        monkeypatch.setattr(ModelApp, 'get_model', lambda self, pk: Model({'name': 'test',
                                                                           'moduleName': 'also-test',
                                                                           'datasetId': '12345',
                                                                           'ready': True,
                                                                           'state': 'Deployed'}))
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'

        # Get the confusion matrix data for a model.
        path = reverse('model-confusion-matrix', kwargs={'project_pk': project.id, 'pk': model_id})
        path = f'{path}?testSetOnly=false'
        response = check_response(api_client.get(path))
        assert response == {'labels': ['Unrecognized',
                                       'airplane',
                                       'automobile',
                                       'bird',
                                       'cat',
                                       'deer',
                                       'dog',
                                       'frog',
                                       'horse',
                                       'ship',
                                       'truck'],
                            'matrix': [[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                                       [14, 61, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                                       [38, 0, 9, 0, 0, 0, 0, 1, 0, 0, 24],
                                       [60, 0, 0, 28, 0, 0, 0, 2, 0, 0, 0],
                                       [3, 0, 0, 0, 83, 0, 0, 0, 0, 0, 0],
                                       [2, 0, 0, 0, 0, 88, 0, 0, 0, 0, 0],
                                       [20, 0, 0, 0, 3, 1, 64, 0, 2, 0, 0],
                                       [4, 0, 0, 0, 0, 0, 0, 83, 0, 0, 0],
                                       [7, 0, 0, 0, 0, 0, 0, 0, 75, 0, 0],
                                       [14, 0, 0, 0, 0, 0, 0, 0, 0, 70, 0],
                                       [19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 63]],
                            'maxScore': 1.0,
                            'minScore': 0.0,
                            'name': 'test',
                            'moduleName': 'also-test',
                            'overallAccuracy': 0.7446300715990454,
                            'testSetOnly': False,
                            'isMatrixApplicable': True,
                            'datasetId': '12345',
                            'unappliedChanges': False}

        # Get the confusion matrix thumbnail.
        path = reverse('model-confusion-matrix-thumbnail',
                       kwargs={'project_pk': project.id, 'pk': model_id})
        response = api_client.get(path)
        assert response.get('Content-Type') == 'image/png'

    def test_confusion_matrix_no_dataset(self, login, project, api_client, monkeypatch):
        monkeypatch.setattr(ModelApp, 'get_model',
                            lambda self, pk: Model({'name': 'test',
                                                    'moduleName': 'also-test',
                                                    'ready': True,
                                                    'state': 'Deployed'}))
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'

        # Try to get the confusion matrix data for a model that does not have a dataset
        path = reverse('model-confusion-matrix', kwargs={'project_pk': project.id, 'pk': model_id})
        response = check_response(api_client.get(path))
        assert response == {'labels': [],
                            'matrix': [],
                            'maxScore': 1.0,
                            'minScore': 0.0,
                            'name': 'test',
                            'moduleName': 'also-test',
                            'overallAccuracy': 0.0,
                            'testSetOnly': True,
                            'isMatrixApplicable': True,
                            'datasetId': None,
                            'unappliedChanges': False}

    def test_upload_url(self, login, api_client, monkeypatch, project):
        uri = 'https://storage.googleapis.com/zvi-dev-archivist-data/projects/d15d4f94-fe5a-4516-8d03-3aaa9c5b69f9/models/a8ae4ac5-8c68-1e29-ab93-e62c8425a62a/latest/model.mar?GoogleAccessId=zmlp-archivist@zvi-dev.iam.gserviceaccount.com&Expires=1626890567&Signature=paQsD1T12hVEbTXs4sJnSpLpE3RAU7qEI8uKJBMq%2BYefRHBan3b3lji61b8PBZA0wIrFcNj7%2FW%2BnD9O%2FwoJC6EK75437%2FiwhkR92Ao8BGSCRrjzGY8aUDiMjQ%2BgwUc0R6zw2XP5sOWkQe1lqnkJhQ%2ByEtKnNnmw4M9Mi%2F4jubjGaeV51m%2F6T4Efbovg7GY4N%2Fv7r21HQYduOzuwGyJDhBjf0%2F%2BATW9sx4DZMIxEA%2F%2BxbZroepQH1h9PAxIvOrD8fQUDgTzQMrILAMR9hXXjt508cOc%2BM41um8qbICFcksR1Psaef4TcN0kRZwOvkkzGzIBCnYOOrZM4Ax9XjbIC3yQ%3D%3D'

        def mock_get_response(*args, **kwargs):
            return {'uri': uri, 'mediaType': 'application/octet-stream'}  # noqa

        monkeypatch.setattr(BoonClient, 'get', mock_get_response)
        path = reverse('model-upload-url', kwargs={'project_pk': project.id, 'pk': '1'})
        response = check_response(api_client.get(path))
        assert response['signedUrl'] == uri

    def test_finish_upload(self, login, api_client, monkeypatch, project):

        def mock_post_response(*args, **kwargs):
            return {'type': 'Model', 'op': 'deploy', 'success': True}

        monkeypatch.setattr(BoonClient, 'post', mock_post_response)
        path = reverse('model-finish-upload', kwargs={'project_pk': project.id, 'pk': '1'})
        response = check_response(api_client.put(path, {}))
        assert response['success'] is True
