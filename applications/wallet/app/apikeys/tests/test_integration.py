import pytest
from django.urls import reverse
from django.test import override_settings
from zmlp import ZmlpClient

pytestmark = pytest.mark.django_db


@pytest.fixture
def data():
    return {
        'id': 'b3a09695-b9fb-40bd-8ea8-bbe0c2cba33f',
        'name': 'Test',
        'projectId': '2fb4e52b-8791-4544-aafb-c16af66f19f8',
        'accessKey': 'P1klR1U1RgT3YfdLYN4-AHPlnOhXZHeD',
        'secretKey': '6Ti7kZZ7IcmWnR1bfdvCMUataoMh9Mbq9Kqvs3xctOM7y1OwbefdFiLewuEDAGBof_lV5y_JKuFtY11bmRjFEg',  # noqa
        'permissions': ['AssetsRead']
    }


class TestApikey:

    @override_settings(PLATFORM='zmlp')
    def test_get_zmlp_list(self, zmlp_project_user, project, api_client, monkeypatch, data):

        def mock_api_response(*args, **kwargs):
            return [data]

        monkeypatch.setattr(ZmlpClient, 'get', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('apikey-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        content = response.json()
        assert len(content['results']) == 1

    @override_settings(PLATFORM='zmlp')
    def test_get_detail(self, zmlp_project_user, project, api_client, monkeypatch, data):

        def mock_api_response(*args, **kwargs):
            return data

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
    def test_post_create(self, zmlp_project_user, project, api_client, monkeypatch, data):

        def mock_api_response(*args, **kwargs):
            return data

        monkeypatch.setattr(ZmlpClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'name': 'job-runner',
                'permissions': ['JobRunner', 'AssetsWrite', 'AssetsRead', 'StorageCreate']}
        response = api_client.post(reverse('apikey-list', kwargs={'project_pk': project.id}), body)
        assert response.status_code == 201
        content = response.json()
        assert content['id'] == 'b3a09695-b9fb-40bd-8ea8-bbe0c2cba33f'

    @override_settings(PLATFORM='zmlp')
    def test_post_create_bad_body(self, zmlp_project_user, project, api_client, monkeypatch, data):

        def mock_api_response(*args, **kwargs):
            return data

        monkeypatch.setattr(ZmlpClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.post(reverse('apikey-list', kwargs={'project_pk': project.id}), {})
        assert response.status_code == 400
        assert response.content == (b'{"name":["This field is required."],"permissions":'
                                    b'["This field is required."]}')
