import pytest
from django.urls import reverse
from django.test import override_settings
from pixml import PixmlClient

pytestmark = pytest.mark.django_db


class TestApikey:

    @override_settings(PLATFORM='zmlp')
    def test_get_pixml_list(self, pixml_project_user, project, api_client, monkeypatch):

        def mock_api_response(*args, **kwargs):
            return [{'keyId': '9a27e6fd-3396-4d98-8641-37f6d05e3275', 'projectId': '00000000-0000-0000-0000-000000000000', 'sharedKey': '8Uf36TXK6cvbMEJp2ktwreRuynaVof0rruKDQRLj5dwh8fvWZcJgk2fUIM7XrSjoTQBhcDilBJ84kvVmry_NIQ', 'name': 'job-runner', 'permissions': ['JobRunner', 'AssetsWrite', 'AssetsRead', 'StorageCreate']}]  # noqa

        monkeypatch.setattr(PixmlClient, 'get', mock_api_response)
        api_client.force_authenticate(pixml_project_user)
        api_client.force_login(pixml_project_user)
        response = api_client.get(reverse('apikey-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        content = response.json()
        assert len(content['results']) == 1

    @override_settings(PLATFORM='zmlp')
    def test_get_detail(self, pixml_project_user, project, api_client, monkeypatch):

        def mock_api_response(*args, **kwargs):
            return {'keyId': '9a27e6fd-3396-4d98-8641-37f6d05e3275', 'projectId': '00000000-0000-0000-0000-000000000000', 'sharedKey': '8Uf36TXK6cvbMEJp2ktwreRuynaVof0rruKDQRLj5dwh8fvWZcJgk2fUIM7XrSjoTQBhcDilBJ84kvVmry_NIQ', 'name': 'job-runner', 'permissions': ['JobRunner', 'AssetsWrite', 'AssetsRead', 'StorageCreate']}  # noqa

        monkeypatch.setattr(PixmlClient, 'get', mock_api_response)
        api_client.force_authenticate(pixml_project_user)
        api_client.force_login(pixml_project_user)
        response = api_client.get(reverse('apikey-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': '9a27e6fd-3396-4d98-8641-37f6d05e3275'}))
        assert response.status_code == 200
        content = response.json()
        assert content['keyId'] == '9a27e6fd-3396-4d98-8641-37f6d05e3275'

    @override_settings(PLATFORM='zmlp')
    def test_post_create(self, pixml_project_user, project, api_client, monkeypatch):

        def mock_api_response(*args, **kwargs):
            return {'keyId': '9a27e6fd-3396-4d98-8641-37f6d05e3275', 'projectId': '00000000-0000-0000-0000-000000000000', 'sharedKey': '8Uf36TXK6cvbMEJp2ktwreRuynaVof0rruKDQRLj5dwh8fvWZcJgk2fUIM7XrSjoTQBhcDilBJ84kvVmry_NIQ', 'name': 'job-runner', 'permissions': ['JobRunner', 'AssetsWrite', 'AssetsRead', 'StorageCreate']}  # noqa

        monkeypatch.setattr(PixmlClient, 'post', mock_api_response)
        api_client.force_authenticate(pixml_project_user)
        api_client.force_login(pixml_project_user)
        body = {'name': 'job-runner',
                'permissions': ['JobRunner', 'AssetsWrite', 'AssetsRead', 'StorageCreate']}
        response = api_client.post(reverse('apikey-list', kwargs={'project_pk': project.id}), body)
        assert response.status_code == 201
        content = response.json()
        assert content['keyId'] == '9a27e6fd-3396-4d98-8641-37f6d05e3275'

    @override_settings(PLATFORM='zmlp')
    def test_post_create_bad_body(self, pixml_project_user, project, api_client, monkeypatch):
        def mock_api_response(*args, **kwargs):
            return {'keyId': '9a27e6fd-3396-4d98-8641-37f6d05e3275', 'projectId': '00000000-0000-0000-0000-000000000000', 'sharedKey': '8Uf36TXK6cvbMEJp2ktwreRuynaVof0rruKDQRLj5dwh8fvWZcJgk2fUIM7XrSjoTQBhcDilBJ84kvVmry_NIQ', 'name': 'job-runner', 'permissions': ['JobRunner', 'AssetsWrite', 'AssetsRead', 'StorageCreate']}  # noqa

        monkeypatch.setattr(PixmlClient, 'post', mock_api_response)
        api_client.force_authenticate(pixml_project_user)
        api_client.force_login(pixml_project_user)
        response = api_client.post(reverse('apikey-list', kwargs={'project_pk': project.id}), {})
        assert response.status_code == 400
        assert response.content == (b'{"name":["This field is required."],"permissions":'
                                    b'["This field is required."]}')
