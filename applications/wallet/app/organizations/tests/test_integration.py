import datetime
import json
from unittest.mock import patch, Mock

import pytest
import requests
from boonsdk import BoonClient
from django.contrib.auth.models import User
from requests import Response
from rest_framework.reverse import reverse

from organizations.models import Organization, Plan
from organizations.serializers import OrganizationSerializer
from organizations.utils import random_organization_name
from projects.models import Project, Membership
from wallet.tests.utils import check_response

pytestmark = pytest.mark.django_db


def test_projects(organization, project, project2):
    assert [p.name for p in organization.projects.all()] == [project.name, project2.name]


def test_deactivate_organization(organization, project, project2):
    organization.isActive = False
    organization.save()
    for _project in [project, project2]:
        _project = Project.all_objects.get(id=_project.id)
        assert not _project.isActive


def test_random_name():
    assert random_organization_name()


def test_organization_with_random_name():
    organization = Organization.objects.create()
    assert organization.name


class TestViews(object):
    def test_organization_list(self, login, zmlp_project_user, api_client, organization):
        path = reverse('organization-list')

        # User is not an organization owner.
        response = check_response(api_client.get(path))
        assert response['count'] == 0

        # User is an organization owner.
        organization.owners.add(zmlp_project_user)
        response = check_response(api_client.get(path))
        assert response['count'] == 1
        organization_result = response['results'][0]
        assert organization_result == OrganizationSerializer(organization).data
        assert organization_result['projectCount'] == 1
        assert organization_result['createdDate']

    def test_organization_retrieve(self, login, zmlp_project_user, api_client, organization):
        path = reverse('organization-detail', kwargs={'pk': organization.id})

        # User is not an organization owner.
        response = check_response(api_client.get(path), status=403)

        # User is an organization owner.
        organization.owners.add(zmlp_project_user)
        response = check_response(api_client.get(path))
        assert response['name'] == organization.name

    def test_organization_retrieve_does_not_exist(self, login, zmlp_project_user, api_client,
                                                  organization):
        path = reverse('organization-detail', kwargs={'pk': '1'})
        check_response(api_client.get(path), status=404)

    def test_org_project_list(self, login, zmlp_project_user, api_client, organization,
                              monkeypatch):
        mock_post_responses = [
            {'aggregations': {'sum#video_seconds': {'value': 16406}}},
            {"hits": {"total": {"value": 35}}},
        ]

        def mock_post(*args, **kwargs):
            return mock_post_responses.pop()

        def mock_get(*args, **kwargs):
            data = {"tier_1": {"image_count": 12, "video_minutes": 55.8},
                    "tier_2": {"image_count": 30, "video_minutes": 6.571}}
            response = Response()
            response.status_code = 200
            response._content = json.dumps(data).encode('utf-8')
            return response

        monkeypatch.setattr(requests, 'get', mock_get)
        monkeypatch.setattr(BoonClient, 'post', mock_post)
        path = reverse('org-project-list', kwargs={'organization_pk': organization.id})

        # User is not an organization owner
        check_response(api_client.get(path), status=403)

        # User is an organization owner.
        organization.owners.add(zmlp_project_user)
        response = check_response(api_client.get(path))
        assert response['count'] == 1
        assert response['results'][0] == {'id': '6abc33f0-4acf-4196-95ff-4cbb7f640a06',
                                          'mlUsageThisMonth': {'tier1': {'imageCount': 12,
                                                                         'videoMinutes': 55.8},
                                                               'tier2': {'imageCount': 30,
                                                                         'videoMinutes': 6.571}},
                                          'name': 'Test Project',
                                          'totalStorageUsage': {'imageCount': 35,
                                                                'videoMinutes': 274},
                                          'userCount': 1}

    def test_org_project_list_metrics_error(self, login, zmlp_project_user, api_client,
                                            organization, monkeypatch):
        mock_post_responses = [
            {'aggregations': {'sum#video_seconds': {'value': 16406}}},
            {"hits": {"total": {"value": 35}}},
        ]

        def mock_post(*args, **kwargs):
            return mock_post_responses.pop()

        monkeypatch.setattr(BoonClient, 'post', mock_post)
        path = reverse('org-project-list', kwargs={'organization_pk': organization.id})

        # User is an organization owner.
        organization.owners.add(zmlp_project_user)
        response = check_response(api_client.get(path))
        assert response['results'][0]['mlUsageThisMonth'] == {'tier1': {'imageCount': -1,
                                                                        'videoMinutes': -1},
                                                              'tier2': {'imageCount': -1,
                                                                        'videoMinutes': -1}}

    def test_org_project_create(self, login, zmlp_project_user, api_client, organization, monkeypatch):
        monkeypatch.setattr(Project, 'sync_with_zmlp', lambda x: None)
        path = reverse('org-project-list', kwargs={'organization_pk': organization.id})

        # User is not an organization owner
        check_response(api_client.post(path), status=403)

        # User is an organization owner.
        organization.owners.add(zmlp_project_user)

        # Try with maxed out projects for organization plan.
        check_response(api_client.post(path), status=405)

        # Try with missing arguments.
        organization.plan = Plan.BUILD
        organization.save()
        check_response(api_client.post(path), status=400)

        # Create a new project in the organization.
        check_response(api_client.post(path, data={'name': 'project_1'}))
        project = Project.objects.get(name='project_1')
        assert project.organization == organization

        # Create a duplicate project in the organization.
        response = check_response(api_client.post(path, data={'name': 'project_1'}), status=409)
        assert response == {'name': ['A project with that name already exists.']}

        # Create a project with the same name in a different organization.
        org2 = Organization.objects.create(name='org2')
        org2.owners.add(zmlp_project_user)
        path2 = reverse('org-project-list', kwargs={'organization_pk': org2.id})
        check_response(api_client.post(path2, data={'name': 'project_1'}))

    def test_org_user_list(self, login, zmlp_project_user, api_client, organization, project):
        path = reverse('org-user-list', kwargs={'organization_pk': organization.id})

        # User is not an organization owner
        check_response(api_client.get(path), status=403)

        # User is an organization owner.
        organization.owners.add(zmlp_project_user)
        other_user = User.objects.create(first_name='other', last_name='user',
                                         email='other@user.com', username='other@user.com')
        other_user.projects.add()
        organization.owners.add(other_user)
        project.users.add(other_user)
        Project.objects.create(name='1', organization=organization).users.add(other_user)
        Project.objects.create(name='2', organization=organization).users.add(other_user)
        response = check_response(api_client.get(path))
        assert response['count'] == 2
        for user in response['results']:
            if user['email'] == zmlp_project_user.email:
                assert user['projectCount'] == 1
            else:
                assert user['projectCount'] == 3

        # Search for user.
        response = check_response(api_client.get(path, {'search': 'other'}))
        assert response['count'] == 1
        assert response['results'][0]['firstName'] == 'other'

        # Sort users.
        response = check_response(api_client.get(path, {'ordering': 'firstName'}))
        assert response['results'][0]['firstName'] == 'fake'
        response = check_response(api_client.get(path, {'ordering': '-firstName'}))
        assert response['results'][0]['firstName'] == 'other'

    def test_org_user_retrieve(self, login, zmlp_project_user, api_client, organization, project):
        path = reverse('org-user-detail', kwargs={'organization_pk': organization.id,
                                                  'pk': zmlp_project_user.id})

        # User is not an organization owner
        check_response(api_client.get(path), status=403)

        # User is an organization owner.
        organization.owners.add(zmlp_project_user)
        other_project = Project.objects.create(name='1', organization=organization)
        other_project.users.add(zmlp_project_user)
        Project.objects.create(name='should_not_be_in_response')
        response = check_response(api_client.get(path))
        assert response['id'] == zmlp_project_user.id
        assert response['email'] == zmlp_project_user.email

    def test_org_user_project_retrieve(self, login, zmlp_project_user, api_client, organization, project):
        path = reverse('org-user-project-list', kwargs={'organization_pk': organization.id,
                                                        'user_pk': zmlp_project_user.id})

        # User is not an organization owner
        check_response(api_client.get(path), status=403)

        # User is an organization owner.
        organization.owners.add(zmlp_project_user)
        other_project = Project.objects.create(name='1', organization=organization)
        other_project.users.add(zmlp_project_user)
        Project.objects.create(name='should_not_be_in_response')
        response = check_response(api_client.get(path))
        assert response['count'] == 2
        expected = [{'id': project.id, 'name': 'Test Project',
                     'roles': ['ML_Tools', 'User_Admin']},
                    {'id': str(other_project.id), 'name': '1', 'roles': []}]
        for project in expected:
            assert project in response['results']

    def test_org_user_destroy(self, login, zmlp_project_user, organization, project, api_client,
                              monkeypatch):
        def mock_destroy_zmlp_api_key(self, client):
            assert type(client) == BoonClient

        monkeypatch.setattr(Membership, 'destroy_zmlp_api_key', mock_destroy_zmlp_api_key)
        other_user = User.objects.create(username='other')
        path = reverse('org-user-detail', kwargs={'organization_pk': organization.id,
                                                  'pk': other_user.id})

        # User is not an organization owner
        check_response(api_client.delete(path), status=403)

        # User is an organization owner.
        organization.owners.add(zmlp_project_user)
        project.users.add(other_user)
        assert project.users.filter(id=other_user.id).exists()
        check_response(api_client.delete(path))
        assert not project.users.filter(id=other_user.id).exists()

    def test_org_owner_list(self, login, zmlp_project_user, api_client, organization, project):
        path = reverse('org-owner-list', kwargs={'organization_pk': organization.id})

        # User is not an organization owner
        check_response(api_client.get(path), status=403)

        # User is an organization owner.
        organization.owners.add(zmlp_project_user)
        response = check_response(api_client.get(path))
        assert response['count'] == 2
        for user in response['results']:
            assert user['email']

        # Search for user.
        response = check_response(api_client.get(path, {'search': 'fake.com'}))
        assert response['count'] == 1
        assert response['results'][0]['firstName'] == 'fake'

        # Sort users.
        response = check_response(api_client.get(path, {'ordering': 'firstName'}))
        assert response['results'][0]['firstName'] == ''
        response = check_response(api_client.get(path, {'ordering': '-firstName'}))
        assert response['results'][0]['firstName'] == 'fake'

    def test_org_owner_destroy(self, login, zmlp_project_user, superuser, api_client,
                               organization, project):
        path = reverse('org-owner-detail', kwargs={'organization_pk': organization.id,
                                                   'pk': superuser.id})

        # User is not an organization owner
        check_response(api_client.delete(path), status=403)

        # User is an organization owner.
        organization.owners.add(zmlp_project_user)
        assert organization.owners.filter(id=superuser.id).exists()
        check_response(api_client.delete(path))
        assert not organization.owners.filter(id=superuser.id).exists()

    def test_org_owner_create(self, api_client, login, zmlp_project_user, organization, project):
        path = reverse('org-owner-list', kwargs={'organization_pk': organization.id})

        # User is not an organization owner
        check_response(api_client.post(path), status=403)

        # User is an organization owner.
        organization.owners.add(zmlp_project_user)

        # Try with missing arguments.
        check_response(api_client.post(path), status=400)

        # Add a new owner.
        user = User.objects.create(username='other@user.com', email='other@user.com')
        assert not organization.owners.filter(id=user.id).exists()
        data = {'emails': [user.email, 'fail@mail.com']}
        response = check_response(api_client.post(path, data=data), status=207)
        assert response == {'results': {'failed': ['fail@mail.com'],
                                        'succeeded': [user.email]}}
        assert organization.owners.filter(id=user.id).exists()


class TestGetMLUsageForTimePeriod:
    fake_usage = {'tier_1': {'video_minutes': 100,
                             'image_count': 100},
                  'tier_2': {'video_minutes': 100,
                             'image_count': 100}}

    @patch.object(requests, 'get')
    def test_get_ml_usage_no_times(self, get_mock, organization, project, project2):

        def side_effect(*args, **kwargs):
            return self.fake_usage
        get_mock.return_value = Mock(json=Mock(side_effect=side_effect))
        usage = organization.get_ml_usage_for_time_period()
        assert usage == {project.id: {'tier_1_image_count': 100,
                                      'tier_1_video_hours': 1,
                                      'tier_2_image_count': 100,
                                      'tier_2_video_hours': 1},
                         project2.id: {'tier_1_image_count': 100,
                                       'tier_1_video_hours': 1,
                                       'tier_2_image_count': 100,
                                       'tier_2_video_hours': 1}}
        assert get_mock.call_count == 2
        project_ids_called = [str(c[0][1]['project']) for c in get_mock.call_args_list]
        assert project.id in project_ids_called
        assert project2.id in project_ids_called

    @patch.object(requests, 'get')
    def test_get_ml_usage_inactive_project(self, get_mock, organization, project, project2):
        project2.isActive = False
        project2.save()

        def side_effect(*args, **kwargs):
            return self.fake_usage

        get_mock.return_value = Mock(json=Mock(side_effect=side_effect))
        usage = organization.get_ml_usage_for_time_period()
        assert usage == {
            project.id: {'tier_1_image_count': 100,
                         'tier_1_video_hours': 1,
                         'tier_2_image_count': 100,
                         'tier_2_video_hours': 1}
        }
        assert get_mock.call_count == 1
        project_ids_called = [str(c[0][1]['project']) for c in get_mock.call_args_list]
        assert [project.id] == project_ids_called

    @patch.object(requests, 'get')
    def test_get_ml_usage_custom_times(self, get_mock, organization, project, project2):

        def side_effect(*args, **kwargs):
            return self.fake_usage

        get_mock.return_value = Mock(json=Mock(side_effect=side_effect))

        end_time = datetime.datetime.utcnow()
        start_time = end_time - datetime.timedelta(minutes=5)

        project_usage = organization.get_ml_usage_for_time_period(start_time=start_time, end_time=end_time)
        assert project_usage
        assert get_mock.call_count == 2
        args = get_mock.call_args[0][1]
        assert args['after'] == start_time
        assert args['before'] == end_time
