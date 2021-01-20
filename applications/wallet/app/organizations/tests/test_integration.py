import pytest

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
