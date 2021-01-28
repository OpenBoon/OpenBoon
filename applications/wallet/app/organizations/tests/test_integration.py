import pytest
import requests
import datetime

from unittest.mock import patch, Mock

from organizations.models import Organization
from organizations.utils import random_organization_name
from projects.models import Project

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


def test_orgainzation_with_random_name():
    organization = Organization.objects.create()
    assert organization.name


class TestGetMLUsageForTimePeriod:

    fake_usage = {'tier_1_image_count': 100,
                  'tier_1_video_hours': 100,
                  'tier_2_image_count': 100,
                  'tier_2_video_hours': 100}

    @patch.object(requests, 'get')
    def test_get_ml_usage_no_times(self, get_mock, organization, project, project2):

        def side_effect(*args, **kwargs):
            return self.fake_usage
        get_mock.return_value = Mock(json=Mock(side_effect=side_effect))
        usage = organization.get_ml_usage_for_time_period()
        assert usage == {'tier_1_image_count': 200,
                         'tier_1_video_hours': 200,
                         'tier_2_image_count': 200,
                         'tier_2_video_hours': 200}
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
        assert usage == self.fake_usage
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

        usage = organization.get_ml_usage_for_time_period(start_time=start_time, end_time=end_time)
        assert usage
        assert get_mock.call_count == 2
        args = get_mock.call_args[0][1]
        assert args['after'] == start_time
        assert args['before'] == end_time
