from uuid import uuid4

import pytest
from django.contrib.auth import get_user_model

from projects.models import Project, Membership

User = get_user_model()


@pytest.fixture
def project():
    return Project(id=uuid4(), name='butts',
                   createdDate="2021-01-11T22:31:37.594984Z",
                   modifiedDate="2021-01-11T22:31:37.755033Z")


@pytest.fixture
def user():
    return User(username='fakey')


def test_project_str(project):
    assert str(project) == 'butts'


def test_membership_str(user, project):
    membership = Membership(user=user, project=project)
    assert str(membership) == 'butts - fakey'


def test_project_has_default_id():
    project = Project(name='test')
    assert project.id
    assert len(str(project.id)) == 36
    assert str(project.id).split('-')[2].startswith('4')
