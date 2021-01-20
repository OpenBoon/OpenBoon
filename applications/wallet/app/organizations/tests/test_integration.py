import pytest

pytestmark = pytest.mark.django_db


def test_projects(organization, project, project2):
    assert [p.name for p in organization.projects.all()] == [project.name, project2.name]
