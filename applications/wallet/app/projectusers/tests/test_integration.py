import pytest
import base64
from django.urls import reverse
from django.test import override_settings
from rest_framework.response import Response
from rest_framework import status
from zmlp.client import ZmlpInvalidRequestException

from zmlp import ZmlpClient
from projects.models import Project, Membership
from apikeys.utils import decode_apikey

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(project):
    return {
        'id': 'b3a09695-b9fb-40bd-8ea8-bbe0c2cba33f',
        'name': 'tester@fake.com',
        'projectId': project.id,
        'accessKey': 'P1klR1U1RgT3YfdLYN4-AHPlnOhXZHeD',
        'secretKey': '6Ti7kZZ7IcmWnR1bfdvCMUataoMh9Mbq9Kqvs3xctOM7y1OwbefdFiLewuEDAGBof_lV5y_JKuFtY11bmRjFEg',  # noqa
        'permissions': ['AssetsRead']
    }


class TestProjectUser:

    def _make_users_for_project(self, project, count, user_model, apikey):
        users = []
        for index in range(0, count):
            username = f'user_{index}'
            user = user_model.objects.create_user(username, f'{username}@fake.com', 'letmein')  # noqa
            Membership.objects.create(user=user, project=project,
                                      apikey=base64.b64encode(apikey).decode('utf-8'))
            users.append(user)
        return users

    @override_settings(PLATFORM='zmlp')
    def test_get_list(self, project, zmlp_project_user, zmlp_project_membership, api_client):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('projectuser-list', kwargs={'project_pk': project.id}))
        assert response.status_code == status.HTTP_200_OK
        content = response.json()
        assert content['results'][0]['id'] == zmlp_project_user.id

    @override_settings(PLATFORM='zmlp')
    def test_get_paginated_list(self, project, zmlp_project_user, zmlp_project_membership,
                                api_client, django_user_model, zmlp_apikey):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        self._make_users_for_project(project, 5, django_user_model, zmlp_apikey)
        uri = reverse('projectuser-list', kwargs={'project_pk': project.id})
        response = api_client.get(f'{uri}?from=0&size=2')
        assert response.status_code == status.HTTP_200_OK
        content = response.json()
        assert content['count'] == 6
        assert len(content['results']) == 2
        assert 'next' in content
        assert content['next'] is not None
        assert 'previous' in content

    @override_settings(PLATFORM='zmlp')
    def test_get_list_bad_project(self, project, zmlp_project_user, zmlp_project_membership,
                                  api_client):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        new_project = Project.objects.create(id='0820a307-c3dd-460e-a9c4-0e5f582e09c3',
                                             name='Test Project')
        response = api_client.get(reverse('projectuser-list', kwargs={'project_pk': new_project.id}))  # noqa
        assert response.status_code == status.HTTP_403_FORBIDDEN

    @override_settings(PLATFORM='zmlp')
    def test_get_retrieve(self, project, zmlp_project_user, zmlp_project_membership, api_client):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': zmlp_project_user.id}))
        assert response.status_code == status.HTTP_200_OK
        content = response.json()
        assert content['id'] == zmlp_project_user.id
        assert content['username'] == zmlp_project_user.username
        assert content['permissions'] == ['SuperAdmin', 'ProjectAdmin',
                                          'AssetsRead', 'AssetsImport']

    @override_settings(PLATFORM='zmlp')
    def test_get_with_bad_apikey(self, project, zmlp_project_user,
                                 zmlp_project_membership, api_client, monkeypatch):
        monkeypatch.setattr(ZmlpClient, '_ZmlpClient__load_apikey', lambda x, y: {})
        zmlp_project_membership.apikey = 'no good'
        zmlp_project_membership.save()
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': zmlp_project_user.id}))
        assert response.status_code == status.HTTP_200_OK
        content = response.json()
        assert content['permissions'] == 'Could not parse apikey, please check.'

    @override_settings(PLATFORM='zmlp')
    def test_get_retrieve_bad_user_pk(self, project, zmlp_project_user, zmlp_project_membership,
                                      api_client):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': 9999}))
        assert response.status_code == status.HTTP_404_NOT_FOUND
        content = response.json()
        assert content['detail'] == 'Not found.'

    @override_settings(PLATFORM='zmlp')
    def test_get_retrieve_non_member_user(self, project, zmlp_project_user,
                                          zmlp_project_membership, api_client,
                                          django_user_model):
        user = django_user_model.objects.create_user('newGuy', 'newGuy@fake.com', 'letmein')
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': user.id}))
        assert response.status_code == status.HTTP_404_NOT_FOUND
        content = response.json()
        assert content['detail'] == 'Not found.'

    @override_settings(PLATFORM='zmlp')
    def test_delete(self, project, zmlp_project_user, zmlp_project_membership, api_client,
                    monkeypatch):

        def mock_return(*args, **kwargs):
            return Response(status=status.HTTP_200_OK)

        monkeypatch.setattr(ZmlpClient, 'delete', mock_return)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.delete(reverse('projectuser-detail',
                                             kwargs={'project_pk': project.id,
                                                     'pk': zmlp_project_user.id}))
        assert response.status_code == status.HTTP_200_OK
        with pytest.raises(Membership.DoesNotExist):
            zmlp_project_user.memberships.get(project=project.id)

    @override_settings(PLATFORM='zmlp')
    def test_delete_non_member_user(self, project, zmlp_project_user,
                                    zmlp_project_membership, api_client, django_user_model):
        user = django_user_model.objects.create_user('newGuy', 'newGuy@fake.com', 'letmein')
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.delete(reverse('projectuser-detail',
                                             kwargs={'project_pk': project.id,
                                                     'pk': user.id}))
        assert response.status_code == status.HTTP_404_NOT_FOUND
        content = response.json()
        assert content['detail'] == 'Not found.'

    @override_settings(PLATFORM='zmlp')
    def test_delete_bad_apikey(self, project, zmlp_project_user, zmlp_project_membership,
                               api_client, django_user_model):
        user = self._make_users_for_project(project, 1, django_user_model,
                                            'oh hey there'.encode('utf-8'))[0]
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.delete(reverse('projectuser-detail',
                                             kwargs={'project_pk': project.id,
                                                     'pk': user.id}))
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content == 'Unable to parse apikey.'

    @override_settings(PLATFORM='zmlp')
    def test_delete_incomplete_apikey(self, project, zmlp_project_user, zmlp_project_membership,
                                      api_client, django_user_model):
        user = self._make_users_for_project(project, 1, django_user_model,
                                            '{"hi": "there"}'.encode('utf-8'))[0]
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.delete(reverse('projectuser-detail',
                                             kwargs={'project_pk': project.id,
                                                     'pk': user.id}))
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content == 'Apikey is incomplete.'

    @override_settings(PLATFORM='zmlp')
    def test_delete_failed_zmlp_delete(self, project, zmlp_project_user,
                                       zmlp_project_membership, api_client, monkeypatch):
        def mock_return(*args, **kwargs):
            return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        monkeypatch.setattr(ZmlpClient, 'delete', mock_return)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.delete(reverse('projectuser-detail',
                                             kwargs={'project_pk': project.id,
                                                     'pk': zmlp_project_user.id}))
        assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
        content = response.json()
        assert content == 'Unable to delete apikey in ZMLP.'

    @override_settings(PLATFORM='zmlp')
    def test_post_create(self, project, zmlp_project_user, zmlp_project_membership,
                         api_client, monkeypatch, django_user_model, data):

        def mock_api_response(*args, **kwargs):
            return data

        new_user = django_user_model.objects.create_user('tester', 'tester@fake.com', 'letmein')
        monkeypatch.setattr(ZmlpClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': 'tester@fake.com', 'permissions': ['AssetsRead']}
        response = api_client.post(reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        assert response.status_code == status.HTTP_201_CREATED
        membership = Membership.objects.get(user=new_user, project=project)
        decoded_apikey = decode_apikey(membership.apikey)
        assert decoded_apikey['permissions'] == ['AssetsRead']

    @override_settings(PLATFORM='zmlp')
    def test_post_create_missing_email(self, project, zmlp_project_user, zmlp_project_membership,
                                       api_client):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'permissions': ['AssetsRead']}
        response = api_client.post(reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content == 'No email given.'

    @override_settings(PLATFORM='zmlp')
    def test_post_create_missing_permissions(self, project, zmlp_project_user,
                                             zmlp_project_membership, api_client):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': 'tester@fake.com'}
        response = api_client.post(reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content == 'No permissions given.'

    @override_settings(PLATFORM='zmlp')
    def test_post_create_multiple_users(self, project, zmlp_project_user, zmlp_project_membership,
                                        api_client, django_user_model):
        django_user_model.objects.create_user('tester', 'tester@fake.com', 'letmein')
        django_user_model.objects.create_user('tester1', 'tester@fake.com', 'letmein')
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': 'tester@fake.com', 'permissions': ['AssetsRead']}
        response = api_client.post(reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content == 'Multiple Users with the given email exist.'

    @override_settings(PLATFORM='zmlp')
    def test_post_create_nonexistent_user(self, project, zmlp_project_user, zmlp_project_membership,
                                          api_client):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': 'tester@fake.com', 'permissions': ['AssetsRead']}
        response = api_client.post(reverse('projectuser-list', kwargs={'project_pk': project.id}),
                                   body)  # noqa
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content == 'No user with the given email.'

    @override_settings(PLATFORM='zmlp')
    def test_post_create_bad_zmlp_response(self, project, zmlp_project_user, monkeypatch, data,
                                           zmlp_project_membership, api_client, django_user_model):

        def mock_api_response(*args, **kwargs):
            raise ZmlpInvalidRequestException({'msg': 'bad'})

        django_user_model.objects.create_user('tester', 'tester@fake.com', 'letmein')
        monkeypatch.setattr(ZmlpClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': 'tester@fake.com', 'permissions': ['AssetsRead']}
        response = api_client.post(reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content == 'Unable to create apikey.'
