from base64 import b64encode

import pytest
from django.conf import settings
from rest_framework.test import APIClient, APIRequestFactory

from organizations.models import Organization
from projects.models import Project, Membership
from subscriptions.models import Subscription


@pytest.fixture(scope='session')
def api_client():
    return APIClient()


@pytest.fixture(scope='session')
def api_factory():
    return APIRequestFactory()


@pytest.fixture
def user(django_user_model, api_client):
    user = django_user_model.objects.create_user('user', 'user@fake.com', 'letmein')
    return user


@pytest.fixture
def superuser(django_user_model, api_client):
    user = django_user_model.objects.create_superuser('superuser', settings.SUPERUSER_EMAIL,
                                                      'letmein')
    return user


@pytest.fixture
def organization(superuser):
    return Organization.objects.create(name='Test Org', owner=superuser)


@pytest.fixture
def project(organization):
    return Project.objects.create(id='6abc33f0-4acf-4196-95ff-4cbb7f640a06',
                                  name='Test Project',
                                  organization=organization)


@pytest.fixture
def project2(organization):
    return Project.objects.create(id='e93cbadb-e5ae-4598-8395-4cf5b30c0e94',
                                  name='Test Project 2',
                                  organization=organization)


@pytest.fixture
def zmlp_apikey():
    return b"""{
    "name": "admin-key",
    "projectId": "97271bd2-5b51-427a-8fbf-93b4cdb2ba85",
    "id": "4338a83f-a920-hedb-a251-a123b17df1ba",
    "accessKey": "Nn6RoLw9TRe2vTUXmy74CZWPDkVwVbcp",
    "secretKey": "beecda19ed4120b8172309e47242ea88bf35d86aca19bdefb189fe468641b48c8e17241ec955b6a6653b5f1b96ed6e88ccb5f251a04efe70d0e2ef93b60bf9b3",
    "permissions": ["SuperAdmin", "ProjectAdmin", "AssetsRead", "AssetsImport"]
}""" # noqa


@pytest.fixture
def zmlp_project_membership(project, user, zmlp_apikey):
    apikey = b64encode(zmlp_apikey).decode('utf-8')
    return Membership.objects.create(user=user, project=project, apikey=apikey,
                                     roles=['ML_Tools', 'User_Admin'])


@pytest.fixture
def zmlp_project2_membership(project2, user, zmlp_apikey):
    apikey = b64encode(zmlp_apikey).decode('utf-8')
    return Membership.objects.create(user=user, project=project2, apikey=apikey,
                                     roles=['ML_Tools', 'User_Admin', 'API_Keys'])


@pytest.fixture
def zmlp_project_user(user, zmlp_project_membership):
    return user


@pytest.fixture
def project_zero():
    return Project.objects.create(id='00000000-0000-0000-0000-000000000000',
                                  name='Project Zero')


@pytest.fixture
def project_zero_membership(project_zero, superuser, zmlp_apikey):
    apikey = b64encode(zmlp_apikey).decode('utf-8')
    return Membership.objects.create(user=superuser, project=project_zero,
                                     apikey=apikey, roles=['ML_Tools', 'API_Key', 'User_Admin'])


@pytest.fixture
def project_zero_subscription(project_zero):
    return Subscription.objects.create(project=project_zero)


@pytest.fixture
def project_zero_user(superuser, project_zero_membership):
    return superuser


@pytest.fixture
def zvi_project_membership(project, user):
    apikey = b"""{"userId": "00000000-7b0b-480e-8c36-f06f04aed2f1",
    "user": "admin",
    "key": "65950f84a6f97c111be559f54666308c719210468c3476e9bae813484bc703ce",
    "server": "https://dev.zorroa.com/"}"""
    apikey = b64encode(apikey).decode('utf-8')
    return Membership.objects.create(user=user, project=project, apikey=apikey)


@pytest.fixture
def zvi_project_user(user, zvi_project_membership):
    return user


@pytest.fixture
def login(api_client, zmlp_project_user):
    api_client.force_authenticate(zmlp_project_user)
    api_client.force_login(zmlp_project_user)
