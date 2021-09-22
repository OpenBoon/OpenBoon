import pytest
from django.urls import reverse
from boonsdk import BoonClient
from boonsdk.client import BoonSdkDuplicateException

pytestmark = pytest.mark.django_db


@pytest.fixture
def detail_data():
    return {
        'id': 'b3a09695-b9fb-40bd-8ea8-bbe0c2cba33f',
        'name': 'Test',
        'projectId': '2fb4e52b-8791-4544-aafb-c16af66f19f8',
        'accessKey': 'myAccessKey',
        'secretKey': 'mySecretKey',
        'permissions': ['AssetsRead']
    }


@pytest.fixture
def list_data():
    return {
        "page": {"from": 0, "size": 50, "totalCount": 2},
        "list": [
            {
                "id": "a98360ed-3a2d-45b5-93cb-414e69cedf8d",
                "projectId": "00000000-0000-0000-0000-000000000000",
                "accessKey": "myAccessKey",
                "secretKey": "ENCRYPTED",
                "name": "Test",
                "permissions": [
                    "AssetsRead",
                    "DataSourceManage",
                    "AssetsDelete",
                    "DataQueueManage",
                    "AssetsImport",
                    "ProjectManage",
                ],
                "timeCreated": 1594157746720,
                "timeModified": 1594157746720,
                "actorCreated": "createdBy",
                "actorModified": "modifiedBy",
                "enabled": True,
                "systemKey": False,
            },
            {
                "id": "48a6795d-b6ee-4485-9e84-1a920c6071d5",
                "projectId": "00000000-0000-0000-0000-000000000000",
                "accessKey": "myAccessKey2",
                "secretKey": "ENCRYPTED",
                "name": "Admin Console Generated Key - 00000000-0000-0000-0000-000000000000",
                "permissions": [
                    "AssetsRead",
                    "DataSourceManage",
                    "AssetsDelete",
                    "DataQueueManage",
                    "ProjectManage",
                    "AssetsImport",
                ],
                "timeCreated": 1594157552802,
                "timeModified": 1594157552802,
                "actorCreated": "createdBy",
                "actorModified": "modifiedBy",
                "enabled": True,
                "systemKey": False,
            },
        ],
    }


class TestApikey:

    def test_get_zmlp_list(self, zmlp_project_user, project, api_client, monkeypatch, list_data):

        def mock_api_response(*args, **kwargs):
            return list_data

        monkeypatch.setattr(BoonClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('apikey-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        content = response.json()
        assert len(content['results']) == 2

    def test_get_detail(self, zmlp_project_user, project, api_client, monkeypatch, detail_data):

        def mock_api_response(*args, **kwargs):
            return detail_data

        monkeypatch.setattr(BoonClient, 'get', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('apikey-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': '9a27e6fd-3396-4d98-8641-37f6d05e3275'}))
        assert response.status_code == 200
        content = response.json()
        assert content['id'] == 'b3a09695-b9fb-40bd-8ea8-bbe0c2cba33f'

    def test_post_create(self, zmlp_project_user, project, api_client, monkeypatch, detail_data):

        def mock_post_response(*args, **kwargs):
            return detail_data

        def mock_get_response(*args, **kwargs):
            return {'accessKey': 'access',
                    'secretKey': 'secret'}

        monkeypatch.setattr(BoonClient, 'post', mock_post_response)
        monkeypatch.setattr(BoonClient, 'get', mock_get_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'name': 'job-runner',
                'permissions': ['JobRunner', 'AssetsWrite', 'AssetsRead', 'StorageCreate']}
        response = api_client.post(reverse('apikey-list', kwargs={'project_pk': project.id}), body)
        assert response.status_code == 201
        content = response.json()
        assert content == {'secretKey': 'secret', 'accessKey': 'access'}
        assert response['Cache-Control'] == 'max-age=0, no-store'

    def test_post_create_bad_body(self, zmlp_project_user, project, api_client,
                                  monkeypatch, detail_data):

        def mock_api_response(*args, **kwargs):
            return detail_data

        monkeypatch.setattr(BoonClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.post(reverse('apikey-list', kwargs={'project_pk': project.id}), {})
        assert response.status_code == 400
        assert response.content == (b'{"name":["This field is required."],"permissions":'
                                    b'["This field is required."]}')

    def test_post_create_existing_name(self, zmlp_project_user, project, api_client,
                                       monkeypatch, login):
        def mock_api_response(*args, **kwargs):
            raise BoonSdkDuplicateException({})

        monkeypatch.setattr(BoonClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'name': 'job-runner',
                'permissions': ['JobRunner', 'AssetsWrite', 'AssetsRead',
                                'StorageCreate']}
        response = api_client.post(
            reverse('apikey-list', kwargs={'project_pk': project.id}), body)
        assert response.status_code == 409
        assert response.json() == {
            'name': ['An API Key with this name already exists.']
        }

    def test_delete_detail(self, zmlp_project_user, project, api_client, monkeypatch, detail_data):
        def mock_api_response(*args, **kwargs):
            return {'success': True}

        monkeypatch.setattr(BoonClient, 'delete', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.delete(reverse('apikey-detail',
                                             kwargs={'project_pk': project.id,
                                                     'pk': '9a27e6fd-3396-4d98-8641-37f6d05e3275'}))
        assert response.status_code == 200
