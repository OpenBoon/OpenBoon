from copy import copy
from unittest.mock import Mock, patch
from uuid import uuid4

import pytest
from django.urls import reverse
from rest_framework import status

import datasets
from assets.utils import AssetBoxImager
from boonsdk import LabelScope, Asset
from boonsdk.app import AssetApp
from boonsdk.client import BoonClient, BoonSdkNotFoundException, BoonSdkSecurityException
from datasets.views import DatasetsViewSet
from wallet.tests.utils import check_response


class TestDatasetsViewsets:

    def test_list(self, login, api_client, project, monkeypatch):

        def mock_post_response(*args, **kwargs):
            return {'list': [{'id': 'ed756d9e-0106-1fb2-adab-0242ac12000e', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Testingerer', 'type': 'Classification', 'description': 'My second testing dataset.', 'modelCount': 0, 'timeCreated': 1622668587793, 'timeModified': 1622668587793, 'actorCreated': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000'}, {'id': 'ed756d9d-0106-1fb2-adab-0242ac12000e', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Testing', 'type': 'Classification', 'description': 'My first testing dataset.', 'modelCount': 0, 'timeCreated': 1622668561506, 'timeModified': 1622668561506, 'actorCreated': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000'}], 'page': {'from': 0, 'size': 20, 'disabled': False, 'totalCount': 2}}  # noqa

        def mock_get_response(*args, **kwargs):
            return {'cat': 2, 'horse': 3}

        monkeypatch.setattr(BoonClient, 'post', mock_post_response)
        monkeypatch.setattr(BoonClient, 'get', mock_get_response)
        path = reverse('dataset-list', kwargs={'project_pk': project.id})
        response = api_client.get(path)
        content = check_response(response)
        assert content['count'] == 2
        item = content['results'][0]
        assert item['name'] == 'Testingerer'
        assert item['type'] == 'Classification'
        assert item['conceptCount'] == 2

    def test_detail(self, login, api_client, project, monkeypatch):
        dataset_id = 'ed756d9e-0106-1fb2-adab-0242ac12000e'

        def mock_response(*args, **kwargs):
            return {'id': dataset_id, 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Testingerer', 'type': 'Classification', 'description': 'My second testing dataset.', 'modelCount': 0, 'timeCreated': 1622668587793, 'timeModified': 1622668587793, 'actorCreated': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000'}  # noqa

        monkeypatch.setattr(BoonClient, 'get', mock_response)
        path = reverse('dataset-detail', kwargs={'project_pk': project.id,
                                                 'pk': 'ed756d9e-0106-1fb2-adab-0242ac12000e'})
        response = api_client.get(path)
        content = check_response(response)
        assert content['id'] == dataset_id
        assert content['name'] == 'Testingerer'

    def test_create(self, login, api_client, project, monkeypatch):

        def mock_response(*args, **kwargs):
            return {'id': '274179cc-2122-167c-9a8a-0242ac12000c', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'My New Dataset', 'type': 'Detection', 'description': 'My detection dataset.', 'modelCount': 0, 'timeCreated': 1622748735068, 'timeModified': 1622748735068, 'actorCreated': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000'}  # noqa

        monkeypatch.setattr(BoonClient, 'post', mock_response)
        path = reverse('dataset-list', kwargs={'project_pk': project.id})
        body = {'projectId': project.id,
                'name': 'My New Dataset',
                'type': 'Detection',
                'description': 'My detection dataset.'}
        response = api_client.post(path, body)
        content = check_response(response, status=status.HTTP_201_CREATED)
        assert content['id'] == '274179cc-2122-167c-9a8a-0242ac12000c'
        assert content['name'] == 'My New Dataset'
        assert content['type'] == 'Detection'

    def test_create_no_project_id(self, login, api_client, project, monkeypatch):

        def mock_response(*args, **kwargs):
            return {'id': '274179cc-2122-167c-9a8a-0242ac12000c', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'My New Dataset', 'type': 'Detection', 'description': 'My detection dataset.', 'modelCount': 0, 'timeCreated': 1622748735068, 'timeModified': 1622748735068, 'actorCreated': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000'}  # noqa

        monkeypatch.setattr(BoonClient, 'post', mock_response)
        path = reverse('dataset-list', kwargs={'project_pk': project.id})
        body = {'name': 'My New Dataset',
                'type': 'Detection',
                'description': 'My detection dataset.'}
        response = api_client.post(path, body)
        content = check_response(response, status=status.HTTP_201_CREATED)
        assert content['id'] == '274179cc-2122-167c-9a8a-0242ac12000c'
        assert content['name'] == 'My New Dataset'
        assert content['type'] == 'Detection'

    def test_create_wrong_project_id(self, login, api_client, project, monkeypatch):
        wrong_id = '00000000-0000-0000-0000-000000000011'

        path = reverse('dataset-list', kwargs={'project_pk': project.id})
        body = {'projectId': wrong_id,
                'name': 'My New Dataset',
                'type': 'Detection',
                'description': 'My detection dataset.'}
        response = api_client.post(path, body)
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == ['Invalid request. You can only create datasets '
                                     'for the current project context.']

    def test_create_missing_args(self, login, api_client, project, monkeypatch):
        path = reverse('dataset-list', kwargs={'project_pk': project.id})
        body = {'projectId': project.id,
                'type': 'Detection',
                'description': 'My detection dataset.'}
        response = api_client.post(path, body)
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['name'] == ['This field is required.']

    def test_create_too_many_args(self, login, api_client, project, monkeypatch):

        def mock_response(*args, **kwargs):
            raise BoonSdkSecurityException({'message': 'You do not have permission '
                                                       'to perform this action.'})

        path = reverse('dataset-list', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        body = {'projectId': project.id,
                'name': 'My New Dataset',
                'type': 'Detection',
                'description': 'My detection dataset.',
                'modelCount': 10}  # Additional field
        response = api_client.post(path, body)
        content = check_response(response, status=status.HTTP_403_FORBIDDEN)
        assert content['detail'] == 'You do not have permission to perform this action.'

    def test_delete(self, login, api_client, project, monkeypatch):
        dataset_id = 'ed756d9d-0106-1fb2-adab-0242ac12000e'

        def mock_response(*args, **kwargs):
            return {'type': 'dataset', 'id': 'ed756d9d-0106-1fb2-adab-0242ac12000e', 'op': 'delete', 'success': True}  # noqa

        path = reverse('dataset-detail', kwargs={'project_pk': project.id,
                                                 'pk': dataset_id})
        monkeypatch.setattr(BoonClient, 'delete', mock_response)
        response = api_client.delete(path)
        content = check_response(response)
        assert content['detail'] == ['Successfully deleted resource.']

    def test_delete_different_project(self, login, api_client, project, project2, zmlp_project_membership,
                                      zmlp_project2_membership, monkeypatch):
        dataset_id = 'ed756d9d-0106-1fb2-adab-0242ac12000e'
        wrong_project_id = project2.id

        def mock_response(*args, **kwargs):
            raise BoonSdkNotFoundException({'message': f'The Dataset {dataset_id} does not exist'})

        path = reverse('dataset-detail', kwargs={'project_pk': wrong_project_id,
                                                 'pk': dataset_id})
        monkeypatch.setattr(BoonClient, 'delete', mock_response)
        response = api_client.delete(path)
        content = check_response(response, status=status.HTTP_404_NOT_FOUND)
        assert content['detail'] == ['Not found.']

    def test_update_dataset(self, login, api_client, project, monkeypatch):
        dataset_id = 'ef965880-4559-10f2-801c-4a8fc1a4e308'

        def mock_put_response(*args, **kwargs):
            return {'type': 'dataset', 'id': dataset_id, 'op': 'update', 'success': True}  # noqa

        def mock_get_response(*args, **kwargs):
            return {'defective': {'TEST': 403, 'TRAIN': 3758}, 'ok': {'TEST': 212, 'TRAIN': 2875}}  # noqa

        monkeypatch.setattr(BoonClient, 'put', mock_put_response)
        monkeypatch.setattr(BoonClient, 'get', mock_get_response)
        path = reverse('dataset-detail', kwargs={'project_pk': project.id, 'pk': dataset_id})
        body = {'id': dataset_id, 'projectId': project.id, 'name': 'quality', 'type': 'Classification', 'description': 'A dataset for model training', 'modelCount': 0, 'timeCreated': 1616783169869, 'timeModified': 1623874271586, 'actorCreated': '931fd6fc-6538-48d8-b7f5-cb45613d9503/Admin Console Generated Key - c4dfc976-2df2-47f4-8cd6-ad5b57f1d558 - jbuhler@zorroa.com_6892bd17-8660-49f5-be2a-843d87c47bb3', 'actorModified': '56128e24-9cd4-43c1-860f-4beedec93ef6/danny-dev-delete-me'}
        response = check_response(api_client.put(path, body))
        assert response == {'description': 'A dataset for model training',
                            'id': 'ef965880-4559-10f2-801c-4a8fc1a4e308',
                            'modelCount': 0,
                            'name': 'quality',
                            'projectId': '6abc33f0-4acf-4196-95ff-4cbb7f640a06',
                            'timeCreated': 1616783169869,
                            'timeModified': 1623874271586,
                            'type': 'Classification'}

    def test_update_dataset_bad_project_id(self, login, api_client, project, monkeypatch):
        dataset_id = 'ef965880-4559-10f2-801c-4a8fc1a4e308'
        body = {'id': dataset_id, 'projectId': project.id, 'name': 'quality', 'type': 'Classification', 'description': 'A dataset for model training', 'modelCount': 0, 'timeCreated': 1616783169869, 'timeModified': 1623874271586, 'actorCreated': '931fd6fc-6538-48d8-b7f5-cb45613d9503/Admin Console Generated Key - c4dfc976-2df2-47f4-8cd6-ad5b57f1d558 - jbuhler@zorroa.com_6892bd17-8660-49f5-be2a-843d87c47bb3', 'actorModified': '56128e24-9cd4-43c1-860f-4beedec93ef6/danny-dev-delete-me'}
        path = reverse('dataset-detail', kwargs={'project_pk': str(uuid4()), 'pk': dataset_id})
        check_response(api_client.put(path, body), status=403)

    def test_destroy_label(self, login, project, api_client, monkeypatch):
        def mock_response(*args, **kwargs):
            return {'updated': 1}

        dataset_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        monkeypatch.setattr(BoonClient, 'delete', mock_response)
        path = reverse('dataset-destroy-label', kwargs={'project_pk': project.id,
                                                        'pk': dataset_id})
        response = api_client.delete(path, {'label': 'Dog'})
        content = check_response(response)
        assert content == {'updated': 1}

    def test_rename_label(self, login, project, api_client, monkeypatch):
        def mock_response(*args, **kwargs):
            return {'updated': 26}

        dataset_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        monkeypatch.setattr(BoonClient, 'put', mock_response)
        path = reverse('dataset-rename-label', kwargs={'project_pk': project.id,
                                                       'pk': dataset_id})
        response = api_client.put(path, {'label': 'Dog', 'newLabel': 'Cat'})
        content = check_response(response)
        assert content == {'updated': 26}

    def test_get_labels(self, login, project, api_client, monkeypatch):

        def mock_response(*args, **kwargs):
            return {'defective': {'TEST': 403, 'TRAIN': 3758}, 'ok': {'TEST': 212, 'TRAIN': 2875}}

        dataset_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        path = reverse('dataset-get-labels', kwargs={'project_pk': project.id, 'pk': dataset_id})
        monkeypatch.setattr(BoonClient, 'get', mock_response)
        response = api_client.get(path)
        content = check_response(response)
        assert content == {'count': 2,
                           'results': [{'label': 'defective', 'trainCount': 3758, 'testCount': 403},
                                       {'label': 'ok', 'trainCount': 2875, 'testCount': 212}]}

    def test_get_models(self, login, project, api_client, monkeypatch):
        dataset_id = '0db0a87e-42a7-11d5-91ea-fea0e31b3bfe'

        def mock_response(*args, **kwargs):
            return {'list': [{'id': '0db0a87e-42a7-11d5-91ea-fea0e31b3bfe', 'projectId': '1c4e7b80-1ca0-4296-b051-3b8d05947eef', 'datasetId': '0db0a87e-42a7-11d5-91ea-fea0e31b3bfe', 'type': 'FACE_RECOGNITION', 'name': 'console', 'moduleName': 'zvi-face-recognition', 'fileId': 'models/0db0a87e-42a7-11d5-91ea-fea0e31b3bfe/__TAG__/model.zip', 'trainingJobName': 'Training model: console - [Face Recognition]', 'ready': False, 'applySearch': {'query': {'match_all': {}}}, 'trainingArgs': {}, 'timeCreated': 1610674842169, 'timeModified': 1610674842169, 'actorCreated': '6909b66f-f163-4661-b481-9f9ded3dfbb9/Admin Console Generated Key - b274724e-17d9-4e15-9aeb-ace0af564d64 - danny@zorroa.com_1c4e7b80-1ca0-4296-b051-3b8d05947eef', 'actorModified': '6909b66f-f163-4661-b481-9f9ded3dfbb9/Admin Console Generated Key - b274724e-17d9-4e15-9aeb-ace0af564d64 - danny@zorroa.com_1c4e7b80-1ca0-4296-b051-3b8d05947eef'}], 'page': {'from': 0, 'size': 50, 'disabled': False, 'totalCount': 1}}  # noqa

        path = reverse('dataset-get-models', kwargs={'project_pk': project.id, 'pk': dataset_id})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        response = api_client.get(path)
        content = check_response(response)
        assert content['count'] == 1
        assert content['results'][0]['name'] == 'console'
        assert content['results'][0]['type'] == 'FACE_RECOGNITION'

    def test_dataset_types(self, login, project, api_client, monkeypatch):

        def mock_response(*args, **kwargs):
            return [{'name': 'Classifier', 'label': 'Classifier', 'description': 'Used to classify assets.'}]  # noqa

        path = reverse('dataset-dataset-types', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'get', mock_response)
        response = api_client.get(path)
        content = check_response(response)
        results = content['results']
        assert len(results) == 1
        assert results[0]['name'] == 'Classifier'


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
        dataset_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        make_label_mock = Mock(return_value=Mock())
        dataset = Mock(make_label=make_label_mock)
        path = reverse('dataset-add-labels', kwargs={'project_pk': project.id,
                                                     'pk': dataset_id})
        with patch.object(DatasetsViewSet, '_get_dataset', return_value=dataset) as get_model:
            with patch.object(AssetApp, 'get_asset'):
                with patch.object(AssetApp, 'update_labels') as update_labels:
                    response = api_client.post(path, add_body)
                    check_response(response, status=status.HTTP_201_CREATED)
        get_model.assert_called_once()
        assert update_labels.call_count == 2
        dataset.make_label.assert_called_with('Mountains', bbox=None,
                                              scope=LabelScope.TEST, simhash=None)

    def test_update_labels(self, login, project, api_client, add_body, remove_body):
        add_body.update(remove_body)
        body = add_body
        dataset_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        make_label_mock = Mock(return_value=Mock())
        dataset = Mock(make_label=make_label_mock)
        path = reverse('dataset-update-labels', kwargs={'project_pk': project.id,
                                                        'pk': dataset_id})
        with patch.object(DatasetsViewSet, '_get_dataset', return_value=dataset) as get_model:
            with patch.object(AssetApp, 'get_asset'):
                with patch.object(AssetApp, 'update_labels') as update_labels:
                    response = api_client.post(path, body)
                    check_response(response, status=status.HTTP_200_OK)
        get_model.assert_called_once()
        # This is due to the mocked calls being condensed down to 1 "asset"
        assert update_labels.call_count == 1
        dataset.make_label.assert_called_with('Mountains', bbox=None,
                                              scope=LabelScope.TEST, simhash=None)

    def test_delete_labels(self, login, project, api_client, remove_body):
        dataset_id = 'b9c52abf-9914-1020-b9f0-0242ac12000a'
        make_label_mock = Mock(return_value=Mock())
        dataset = Mock(make_label=make_label_mock)
        path = reverse('dataset-delete-labels', kwargs={'project_pk': project.id,
                                                        'pk': dataset_id})
        with patch.object(DatasetsViewSet, '_get_dataset', return_value=dataset) as get_model:
            with patch.object(AssetApp, 'get_asset'):
                with patch.object(AssetApp, 'update_labels') as update_labels:
                    response = api_client.delete(path, remove_body)
                    check_response(response)
        get_model.assert_called_once()
        assert update_labels.call_count == 2
        dataset.make_label.assert_called_with('Mountains', bbox=None,
                                              scope=LabelScope.TEST, simhash=None)

    def test_label_tool_info_classification(self, login, project, api_client, monkeypatch):
        def mock_get_asset(*args, **kwargs):
            return asset

        def mock_get_dataset(*args, **kwargs):
            return {'type': 'Classification'}

        def mock_b64_image(*args, **kwargs):
            return 'b64-image-str'

        monkeypatch.setattr(AssetApp, 'get_asset', mock_get_asset)
        monkeypatch.setattr(BoonClient, 'get', mock_get_dataset)
        monkeypatch.setattr(AssetBoxImager, 'image', lambda *args: None)
        monkeypatch.setattr(datasets.views, 'get_b64', mock_b64_image)
        monkeypatch.setattr(datasets.views, 'resize_image', lambda *args: None)
        dataset_id = '287baa12-8f80-1a31-9273-76fd36c58a09'

        # Asset that has a classification label.
        asset_data = {'id': dataset_id, 'document': {'labels': [{'datasetId': '287baa12-8f80-1a31-9273-76fd36c58a09', 'scope': 'TRAIN', 'label': 'pretzel', }]}}
        asset = Asset(asset_data)
        path = reverse('dataset-label-tool-info', kwargs={'project_pk': project.id,
                                                          'pk': dataset_id})
        response = check_response(api_client.get(f'{path}?assetId={asset.id}'))
        assert response == {'count': 1, 'results': [{'datasetId': '287baa12-8f80-1a31-9273-76fd36c58a09', 'scope': 'TRAIN', 'label': 'pretzel', 'b64Image': 'b64-image-str'}]}

        # Asset that has no labels.
        asset_data = {'id': dataset_id, 'document': {'labels': []}}
        asset = Asset(asset_data)
        response = check_response(api_client.get(f'{path}?assetId={asset.id}'))
        assert response == {'count': 1, 'results': [{'datasetId': '287baa12-8f80-1a31-9273-76fd36c58a09', 'scope': 'TRAIN', 'label': '', 'b64Image': 'b64-image-str'}]}

    def test_label_tool_info_face_recognition(self, login, project, api_client, monkeypatch):
        def mock_get_asset(*args, **kwargs):
            return asset

        def mock_get_dataset(*args, **kwargs):
            return {'type': 'FaceRecognition'}

        def mock_b64_image(self, label, size):
            label['b64Image'] = 'b64-image-str'

        monkeypatch.setattr(AssetApp, 'get_asset', mock_get_asset)
        monkeypatch.setattr(BoonClient, 'get', mock_get_dataset)
        monkeypatch.setattr(AssetBoxImager, '_add_box_images', mock_b64_image)
        dataset_id = '287baa12-8f80-1a31-9273-76fd36c58a09'

        # Asset with 1 labeled face detection and 1 unlabeled face detection.
        asset_data = {'id': 'w1US8zZQfWuYPnEp2tIsIYzZsPJjHhzg','document': {'analysis': {'boonai-face-detection': {'count': 3,'type': 'labels','predictions': [{'score': 0.991,'bbox': [0.297,0.207,0.414,0.474],'label': 'face0','simhash': 'simhash'},{'score': 0.954,'bbox': [0.202,0.83,0.264,0.965],'label': 'face1','simhash': 'simhash'}]}},'labels': [{'datasetId': dataset_id,'scope': 'TRAIN','bbox': [0.297,0.207,0.414,0.474],'label': 'Jack Hodgens','simhash': 'simhash'}]}}  # noqa
        asset = Asset(asset_data)
        path = reverse('dataset-label-tool-info', kwargs={'project_pk': project.id, 'pk': dataset_id})
        response = check_response(api_client.get(f'{path}?assetId={asset.id}'))
        assert response == {'count': 2, 'results': [{'scope': 'TRAIN', 'bbox': [0.297, 0.207, 0.414, 0.474], 'datasetId': dataset_id, 'b64Image': 'b64-image-str', 'label': 'Jack Hodgens', 'simhash': 'simhash'}, {'scope': 'TRAIN', 'bbox': [0.202, 0.83, 0.264, 0.965], 'datasetId': dataset_id, 'b64Image': 'b64-image-str', 'label': '', 'simhash': 'simhash'}]}

    def test_label_tool_info_detection(self, login, project, api_client, monkeypatch):
        def mock_get_asset(*args, **kwargs):
            return asset

        def mock_get_dataset(*args, **kwargs):
            return {'type': 'Detection'}

        def mock_b64_image(self, label, size):
            label['b64Image'] = 'b64-image-str'

        monkeypatch.setattr(AssetApp, 'get_asset', mock_get_asset)
        monkeypatch.setattr(BoonClient, 'get', mock_get_dataset)
        monkeypatch.setattr(AssetBoxImager, '_add_box_images', mock_b64_image)
        dataset_id = '287baa12-8f80-1a31-9273-76fd36c58a09'

        # Asset with no detections.
        asset_data = {'id': 'w1US8zZQfWuYPnEp2tIsIYzZsPJjHhzg', 'document': {'labels': []}}
        asset = Asset(asset_data)
        path = reverse('dataset-label-tool-info',
                       kwargs={'project_pk': project.id, 'pk': dataset_id})
        response = check_response(api_client.get(f'{path}?assetId={asset.id}'))
        assert response == {'count': 0, 'results': []}

        # Asset with 1 detection.
        label = {'datasetId': dataset_id, 'scope': 'TRAIN', 'bbox': [0.297, 0.207, 0.414, 0.474], 'label': 'cup'}
        asset_data = {'id': 'w1US8zZQfWuYPnEp2tIsIYzZsPJjHhzg',
                      'document': {'labels': [label]}}
        asset = Asset(asset_data)
        path = reverse('dataset-label-tool-info',
                       kwargs={'project_pk': project.id, 'pk': dataset_id})
        response = check_response(api_client.get(f'{path}?assetId={asset.id}'))
        expected_label = copy(label)
        expected_label['b64Image'] = 'b64-image-str'
        assert response == {'count': 1, 'results': [expected_label]}

    def test_label_tool_info_no_query_param(self, login, project, api_client, monkeypatch):
        dataset_id = '287baa12-8f80-1a31-9273-76fd36c58a09'
        path = reverse('dataset-label-tool-info',
                       kwargs={'project_pk': project.id, 'pk': dataset_id})
        response_content = check_response(api_client.get(path), status=status.HTTP_400_BAD_REQUEST)
        assert response_content == {'detail': ['An `assetId` query param is required.']}

    def test_label_tool_info_no_faces(self, login, project, api_client, monkeypatch):
        def mock_get_asset(*args, **kwargs):
            return asset

        def mock_get_dataset(*args, **kwargs):
            return {'type': 'FaceRecognition'}

        def mock_b64_image(self, label, size):
            label['b64Image'] = 'b64-image-str'

        monkeypatch.setattr(AssetApp, 'get_asset', mock_get_asset)
        monkeypatch.setattr(BoonClient, 'get', mock_get_dataset)
        monkeypatch.setattr(AssetBoxImager, '_add_box_images', mock_b64_image)
        dataset_id = '287baa12-8f80-1a31-9273-76fd36c58a09'

        # Asset with 1 labeled face detection and 1 unlabeled face detection.
        asset_data = {'id': 'w1US8zZQfWuYPnEp2tIsIYzZsPJjHhzg', 'document': {'analysis': {}}}
        asset = Asset(asset_data)
        path = reverse('dataset-label-tool-info',
                       kwargs={'project_pk': project.id, 'pk': dataset_id})
        response = check_response(api_client.get(f'{path}?assetId={asset.id}'))
        assert response == {'count': 0, 'results': []}
