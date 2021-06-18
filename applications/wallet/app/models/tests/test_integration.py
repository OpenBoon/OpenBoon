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

pytestmark = pytest.mark.django_db


@pytest.fixture
def model_fields():
    return ['id', 'name', 'type', 'moduleName', 'fileId', 'trainingJobName',
            'unappliedChanges', 'applySearch', 'timeCreated', 'timeModified', 'actorCreated',
            'actorModified', 'link', 'projectId', 'datasetId']


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
            return {'list': [{'id': 'b9c52abf-9914-1020-b9f0-0242ac12000a', 'projectId': '00000000-0000-0000-0000-000000000000', 'type': 'ZVI_LABEL_DETECTION', 'name': 'Labeller', 'datasetId': None, 'moduleName': 'zvi-labeller-label-detection', 'fileId': 'models/b9c52abf-9914-1020-b9f0-0242ac12000a/zvi-labeller-label-detection/zvi-labeller-label-detection.zip', 'trainingJobName': 'Train Labeller / zvi-labeller-label-detection', 'ready': False, 'datasetId': None, 'applySearch': {'query': {'match_all': {}}}, 'timeCreated': 1594678625043, 'timeModified': 1594678625043, 'actorCreated': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000'}, {'id': 'b9c52abe-9914-1020-b9f0-0242ac12000a', 'projectId': '00000000-0000-0000-0000-000000000000', 'type': 'ZVI_KNN_CLASSIFIER', 'name': 'MyClassifier', 'moduleName': 'zvi-myclassifier-cluster', 'fileId': 'models/b9c52abe-9914-1020-b9f0-0242ac12000a/zvi-myclassifier-cluster/zvi-myclassifier-cluster.zip', 'trainingJobName': 'Train MyClassifier / zvi-myclassifier-cluster', 'ready': False, 'datasetId': None, 'applySearch': {'query': {'match_all': {}}}, 'timeCreated': 1594676501554, 'timeModified': 1594676501554, 'actorCreated': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000'}], 'page': {'from': 0, 'size': 50, 'disabled': False, 'totalCount': 2}}  # noqa

        def job_response(*args, **kwargs):
            return []

        path = reverse('model-list', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        monkeypatch.setattr(BoonClient, 'iter_paged_results', job_response)
        response = api_client.get(path)
        content = check_response(response)
        results = content['results']
        assert len(results) == 2
        assert results[0]['name'] == 'Labeller'
        assert results[0]['type'] == 'ZVI_LABEL_DETECTION'
        assert set(model_fields) == set(results[0].keys())


class TestModelViewSetListAll:

    @override_settings(REST_FRAMEWORK={'PAGE_SIZE': 2})
    def test_list_all(self, login, project, api_client, monkeypatch, model_fields):

        def mock_response(*args, **kwargs):
            return {'list': [{'id': 'b9c52abf-9914-1020-b9f0-0242ac12000a', 'projectId': '00000000-0000-0000-0000-000000000000', 'type': 'ZVI_LABEL_DETECTION', 'name': 'Labeller', 'datasetId': None, 'moduleName': 'zvi-labeller-label-detection', 'fileId': 'models/b9c52abf-9914-1020-b9f0-0242ac12000a/zvi-labeller-label-detection/zvi-labeller-label-detection.zip', 'trainingJobName': 'Train Labeller / zvi-labeller-label-detection', 'ready': False, 'applySearch': {'query': {'match_all': {}}}, 'timeCreated': 1594678625043, 'timeModified': 1594678625043, 'actorCreated': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000'}, {'id': 'b9c52abe-9914-1020-b9f0-0242ac12000a', 'projectId': '00000000-0000-0000-0000-000000000000', 'type': 'ZVI_KNN_CLASSIFIER', 'name': 'MyClassifier', 'datasetId': None, 'moduleName': 'zvi-myclassifier-cluster', 'fileId': 'models/b9c52abe-9914-1020-b9f0-0242ac12000a/zvi-myclassifier-cluster/zvi-myclassifier-cluster.zip', 'trainingJobName': 'Train MyClassifier / zvi-myclassifier-cluster', 'ready': False, 'applySearch': {'query': {'match_all': {}}}, 'timeCreated': 1594676501554, 'timeModified': 1594676501554, 'actorCreated': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': '33492e0d-9bf2-418e-b0cb-22310926baed/Admin Console Generated Key - a265c25a-0b21-48bc-b57f-42693b28bfaa - software@zorroa.com_00000000-0000-0000-0000-000000000000'}], 'page': {'from': 0, 'size': 50, 'disabled': False, 'totalCount': 9}}  # noqa

        def job_response(*args, **kwargs):
            return []

        path = reverse('model-all', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        monkeypatch.setattr(BoonClient, 'iter_paged_results', job_response)
        response = api_client.get(path)
        content = check_response(response)
        results = content['results']
        assert len(results) == 10
        assert results[0]['name'] == 'Labeller'
        assert results[0]['type'] == 'ZVI_LABEL_DETECTION'
        assert set(model_fields) == set(results[0].keys())


class TestModelViewSetRetrieve:
    def test_retrieve(self, login, project, api_client, monkeypatch, model_fields):
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'

        def mock_response(*args, **kwrags):
            return {'id': model_id, 'projectId': '18e87cfe-23a0-4f62-973d-4e22f0f4b8d8', 'datasetId': 'ebf3e4a6-458f-15d4-a6b1-aab1332fef21', 'type': 'KNN_CLASSIFIER', 'name': 'knn', 'moduleName': 'knn', 'fileId': 'models/ebf3e4a6-458f-15d4-a6b1-aab1332fef21/__TAG__/model.zip', 'trainingJobName': 'Training model: knn - [Label Detection]', 'ready': False, 'applySearch': {'query': {'match_all': {}}}, 'trainingArgs': {}, 'timeCreated': 1619725616046, 'timeModified': 1619725616046, 'actorCreated': '9250c03e-a167-4cb9-a0fc-2198a1a00779/Admin Console Generated Key - 5f52268e-749c-4141-80b3-2fe4daa4552b - jbuhler@zorroa.com_18e87cfe-23a0-4f62-973d-4e22f0f4b8d8', 'actorModified': '9250c03e-a167-4cb9-a0fc-2198a1a00779/Admin Console Generated Key - 5f52268e-749c-4141-80b3-2fe4daa4552b - jbuhler@zorroa.com_18e87cfe-23a0-4f62-973d-4e22f0f4b8d8'}  # noqa

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
        assert set(model_fields) == set(content.keys())
        restrictions = content['modelTypeRestrictions']
        assert restrictions['missingLabels'] == 0
        assert restrictions['missingLabelsOnAssets'] == 1


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


class TestModelViewSetCreate:

    def test_create(self, login, project, api_client, monkeypatch, model_fields):

        def mock_response(*args, **kwargs):
            return {'id': 'ebf3e4a6-458f-15d4-a6b1-aab1332fef21', 'projectId': '18e87cfe-23a0-4f62-973d-4e22f0f4b8d8', 'datasetId': 'ebf3e4a6-458f-15d4-a6b1-aab1332fef21', 'type': 'KNN_CLASSIFIER', 'name': 'Test Model', 'moduleName': 'knn', 'fileId': 'models/ebf3e4a6-458f-15d4-a6b1-aab1332fef21/__TAG__/model.zip', 'trainingJobName': 'Training model: knn - [Label Detection]', 'ready': False, 'applySearch': {'query': {'match_all': {}}}, 'trainingArgs': {}, 'timeCreated': 1619725616046, 'timeModified': 1619725616046, 'actorCreated': '9250c03e-a167-4cb9-a0fc-2198a1a00779/Admin Console Generated Key - 5f52268e-749c-4141-80b3-2fe4daa4552b - jbuhler@zorroa.com_18e87cfe-23a0-4f62-973d-4e22f0f4b8d8', 'actorModified': '9250c03e-a167-4cb9-a0fc-2198a1a00779/Admin Console Generated Key - 5f52268e-749c-4141-80b3-2fe4daa4552b - jbuhler@zorroa.com_18e87cfe-23a0-4f62-973d-4e22f0f4b8d8'}  # noqa

        body = {'name': 'Test Model',
                'type': 'ZVI_KNN_CLASSIFIER'}
        path = reverse('model-list', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        response = api_client.post(path, body)
        content = check_response(response, status.HTTP_201_CREATED)
        assert content['name'] == 'Test Model'
        assert content['type'] == 'KNN_CLASSIFIER'
        fields = ['id', 'projectId', 'type', 'name', 'moduleName', 'fileId', 'trainingJobName',
                  'ready', 'applySearch', 'timeCreated', 'timeModified', 'actorCreated',
                  'actorModified', 'datasetId']
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
            return [{'name': 'ZVI_KNN_CLASSIFIER', 'label': 'Sci-kit Learn KNN Classifier', 'description': 'Classify images or documents using a KNN classifier.  This type of model generates a single prediction which can be used to quickly organize assets into general groups.The KNN classifier works with just a single image and label.', 'objective': 'Label Detection', 'provider': 'Zorroa', 'deployOnTrainingSet': True, 'minConcepts': 1, 'minExamples': 1}, {'name': 'ZVI_LABEL_DETECTION', 'label': 'Tensorflow CNN Classifier', 'description': 'Classify images or documents using a custom trained CNN deep learning algorithm.  This type of model generates multiple predictions and can be trained to identify very specific features. The label detection classifier requires at least 2 concepts with 10 labeled images each. ', 'objective': 'Label Detection', 'provider': 'Zorroa', 'deployOnTrainingSet': False, 'minConcepts': 2, 'minExamples': 10}, {'name': 'ZVI_FACE_RECOGNITION', 'label': 'ZVI Face Recognition', 'description': 'Relabel existing ZVI faces using a KNN Face Recognition model.', 'objective': 'Face Recognition', 'provider': 'Zorroa', 'deployOnTrainingSet': True, 'minConcepts': 1, 'minExamples': 1}, {'name': 'GCP_AUTOML_CLASSIFIER', 'label': 'Google AutoML Classifier', 'description': 'Utilize Google AutoML to train an image classifier.', 'objective': 'Label Detection', 'provider': 'Google', 'deployOnTrainingSet': True, 'minConcepts': 2, 'minExamples': 10}]  # noqa

        path = reverse('model-model-types', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'get', mock_response)
        response = api_client.get(path)
        content = check_response(response)
        results = content['results']
        assert len(results) == 3
        assert results[0]['name'] == 'ZVI_KNN_CLASSIFIER'
        assert results[0]['label'] == 'Sci-kit Learn KNN Classifier'
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

    def test_confusion_matrix_actions(self, login, project, api_client, monkeypatch):
        def mock_aggs(*args, **kwargs):
            return {'nested#nested_labels': {'doc_count': 1744, 'filter#model_train_labels': {'doc_count': 838, 'sterms#labels': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'bird', 'doc_count': 90, 'reverse_nested#predictions_by_label': {'doc_count': 90, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'Unrecognized', 'doc_count': 60}, {'key': 'bird', 'doc_count': 28}, {'key': 'frog', 'doc_count': 2}]}}}, {'key': 'deer', 'doc_count': 90, 'reverse_nested#predictions_by_label': {'doc_count': 90, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'deer', 'doc_count': 88}, {'key': 'Unrecognized', 'doc_count': 2}]}}}, {'key': 'dog', 'doc_count': 90, 'reverse_nested#predictions_by_label': {'doc_count': 90, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'dog', 'doc_count': 64}, {'key': 'Unrecognized', 'doc_count': 20}, {'key': 'cat', 'doc_count': 3}, {'key': 'horse', 'doc_count': 2}, {'key': 'deer', 'doc_count': 1}]}}}, {'key': 'frog', 'doc_count': 87, 'reverse_nested#predictions_by_label': {'doc_count': 87, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'frog', 'doc_count': 83}, {'key': 'Unrecognized', 'doc_count': 4}]}}}, {'key': 'cat', 'doc_count': 86, 'reverse_nested#predictions_by_label': {'doc_count': 86, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'cat', 'doc_count': 83}, {'key': 'Unrecognized', 'doc_count': 3}]}}}, {'key': 'ship', 'doc_count': 84, 'reverse_nested#predictions_by_label': {'doc_count': 84, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'ship', 'doc_count': 70}, {'key': 'Unrecognized', 'doc_count': 14}]}}}, {'key': 'horse', 'doc_count': 82, 'reverse_nested#predictions_by_label': {'doc_count': 82, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'horse', 'doc_count': 75}, {'key': 'Unrecognized', 'doc_count': 7}]}}}, {'key': 'truck', 'doc_count': 82, 'reverse_nested#predictions_by_label': {'doc_count': 82, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'truck', 'doc_count': 63}, {'key': 'Unrecognized', 'doc_count': 19}]}}}, {'key': 'airplane', 'doc_count': 75, 'reverse_nested#predictions_by_label': {'doc_count': 75, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'airplane', 'doc_count': 61}, {'key': 'Unrecognized', 'doc_count': 14}]}}}, {'key': 'automobile', 'doc_count': 72, 'reverse_nested#predictions_by_label': {'doc_count': 72, 'sterms#predictions': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'Unrecognized', 'doc_count': 38}, {'key': 'truck', 'doc_count': 24}, {'key': 'automobile', 'doc_count': 9}, {'key': 'frog', 'doc_count': 1}]}}}]}}}}  # noqa
        monkeypatch.setattr(ConfusionMatrix, '_ConfusionMatrix__get_confusion_matrix_aggregations',
                            mock_aggs)
        monkeypatch.setattr(ModelApp, 'get_model', lambda self, pk: Model({'name': 'test', 'moduleName': 'also-test'}))
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
                            'isMatrixApplicable': True}

        # Get the confusion matrix thumbnail.
        path = reverse('model-confusion-matrix-thumbnail',
                       kwargs={'project_pk': project.id, 'pk': model_id})
        response = api_client.get(path)
        assert response.get('Content-Type') == 'image/png'

    def test_confusion_matrix_error(self, login, project, api_client, monkeypatch):
        monkeypatch.setattr(Model, 'get_confusion_matrix_search', TypeError)
        monkeypatch.setattr(ModelApp, 'get_model', lambda self, pk: Model({'name': 'test', 'moduleName': 'also-test'}))
        model_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'

        # Try to get the confusion matrix data for a model that does not support matrices.
        path = reverse('model-confusion-matrix', kwargs={'project_pk': project.id, 'pk': model_id})
        response = check_response(api_client.get(path))
        assert response == {'labels': [],
                            'matrix': [],
                            'maxScore': 1.0,
                            'minScore': 0.0,
                            'name': 'test',
                            'moduleName': 'also-test',
                            'overallAccuracy': 0,
                            'testSetOnly': True,
                            'isMatrixApplicable': False}
