from uuid import uuid4

import pytest
from django.contrib.auth.models import User

from projects.models import Project, Membership


@pytest.fixture
def project():
    return Project(id=uuid4(), name='butts')


def test_project_str(project):
    assert str(project) == 'butts'


def test_membership_str(project):
    user = User(username='fakey')
    membership = Membership(user=user, project=project)
    assert str(membership) == 'butts - fakey'


def test_project_has_default_id():
    project = Project(name='test')
    assert project.id
    assert len(str(project.id)) == 36
    assert str(project.id).split('-')[2].startswith('4')
