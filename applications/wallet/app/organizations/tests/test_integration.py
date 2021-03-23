import datetime
from unittest.mock import patch, Mock

import pytest
import requests
from rest_framework.reverse import reverse

from organizations.models import Organization
from organizations.serializers import OrganizationSerializer
from organizations.utils import random_organization_name
from projects.models import Project
from projects.serializers import ProjectDetailSerializer
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

    def test_org_project_list(self, login, zmlp_project_user, api_client, organization):
        path = reverse('org-project-list', kwargs={'organization_pk': organization.id})

        # User is not an organization owner
        response = check_response(api_client.get(path))
        assert response['count'] == 0

        # User is an organization owner.
        organization.owners.add(zmlp_project_user)
        response = check_response(api_client.get(path))
        assert response['count'] == 1
        assert response['results'][0] == ProjectDetailSerializer(organization.projects.first()).data


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
