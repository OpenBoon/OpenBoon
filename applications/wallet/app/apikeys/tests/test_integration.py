import pytest
from django.urls import reverse
from django.test import override_settings
from zmlp import ZmlpClient

pytestmark = pytest.mark.django_db


@pytest.fixture
def detail_data():
    return {
        'id': 'b3a09695-b9fb-40bd-8ea8-bbe0c2cba33f',
        'name': 'Test',
        'projectId': '2fb4e52b-8791-4544-aafb-c16af66f19f8',
        'accessKey': 'P1klR1U1RgT3YfdLYN4-AHPlnOhXZHeD',
        'secretKey': '6Ti7kZZ7IcmWnR1bfdvCMUataoMh9Mbq9Kqvs3xctOM7y1OwbefdFiLewuEDAGBof_lV5y_JKuFtY11bmRjFEg',  # noqa
        'permissions': ['AssetsRead']
    }


@pytest.fixture
def list_data():
    return [{'id': '591ae0c6-5e1e-48ca-929a-3bee4f16b363', 'projectId': '00000000-0000-0000-0000-000000000000', 'accessKey': 'r0fN72Qu59PaJV0-aEebkA', 'secretKey': 'ENCRYPTED', 'name': 'test-key', 'permissions': ['AssetsDelete', 'DataSourceManage', 'ProjectManage', 'DataQueueManage', 'AssetsImport', 'AssetsRead'], 'timeCreated': 1593729216924, 'timeModified': 1593729216924, 'actorCreated': 'ba4003f0-81e3-4a99-8921-4cec6e7f4b2f/Admin Console Generated Key - f7e17680-f261-4738-8c22-cfad20fb4acd - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': 'ba4003f0-81e3-4a99-8921-4cec6e7f4b2f/Admin Console Generated Key - f7e17680-f261-4738-8c22-cfad20fb4acd - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'enabled': True, 'systemKey': False}]  # noqa


class TestApikey:

    @override_settings(PLATFORM='zmlp')
    def test_get_zmlp_list(self, zmlp_project_user, project, api_client, monkeypatch, list_data):

        def mock_api_response(*args, **kwargs):
            return list_data

        monkeypatch.setattr(ZmlpClient, 'get', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('apikey-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        content = response.json()
        assert len(content['results']) == 1

    @override_settings(PLATFORM='zmlp')
    def test_get_detail(self, zmlp_project_user, project, api_client, monkeypatch, detail_data):

        def mock_api_response(*args, **kwargs):
            return detail_data

        monkeypatch.setattr(ZmlpClient, 'get', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('apikey-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': '9a27e6fd-3396-4d98-8641-37f6d05e3275'}))
        assert response.status_code == 200
        content = response.json()
        assert content['id'] == 'b3a09695-b9fb-40bd-8ea8-bbe0c2cba33f'

    @override_settings(PLATFORM='zmlp')
    def test_post_create(self, zmlp_project_user, project, api_client, monkeypatch, detail_data):

        def mock_post_response(*args, **kwargs):
            return detail_data

        def mock_get_response(*args, **kwargs):
            return {'accessKey': 'access',
                    'secretKey': 'secret'}

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'name': 'job-runner',
                'permissions': ['JobRunner', 'AssetsWrite', 'AssetsRead', 'StorageCreate']}
        response = api_client.post(reverse('apikey-list', kwargs={'project_pk': project.id}), body)
        assert response.status_code == 201
        content = response.json()
        assert content == {'secretKey': 'secret', 'accessKey': 'access'}

    @override_settings(PLATFORM='zmlp')
    def test_post_create_bad_body(self, zmlp_project_user, project, api_client,
                                  monkeypatch, detail_data):

        def mock_api_response(*args, **kwargs):
            return detail_data

        monkeypatch.setattr(ZmlpClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.post(reverse('apikey-list', kwargs={'project_pk': project.id}), {})
        assert response.status_code == 400
        assert response.content == (b'{"name":["This field is required."],"permissions":'
                                    b'["This field is required."]}')

    @override_settings(PLATFORM='zmlp')
    def test_delete_detail(self, zmlp_project_user, project, api_client, monkeypatch, detail_data):
        def mock_api_response(*args, **kwargs):
            return {}

        monkeypatch.setattr(ZmlpClient, 'delete', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.delete(reverse('apikey-detail',
                                             kwargs={'project_pk': project.id,
                                                     'pk': '9a27e6fd-3396-4d98-8641-37f6d05e3275'}))
        assert response.status_code == 200
