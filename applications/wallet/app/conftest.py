from base64 import b64encode

import pytest
from django.conf import settings
from rest_framework.test import APIClient, APIRequestFactory

from organizations.models import Organization
from projects.models import Project, Membership


@pytest.fixture(scope='session')
def api_client():
    return APIClient()


@pytest.fixture(scope='session')
def api_factory():
    return APIRequestFactory()


@pytest.fixture
def user(django_user_model, api_client):
    user = django_user_model.objects.create_user('user', 'user@fake.com', 'letmein',
                                                 first_name='fake', last_name='user')
    return user


@pytest.fixture
def superuser(django_user_model, api_client):
    user = django_user_model.objects.create_superuser('superuser', settings.SUPERUSER_EMAIL,
                                                      'letmein')
    return user


@pytest.fixture
def organization(superuser):
    org = Organization.objects.create(name='Test Org')
    org.owners.add(superuser)
    return org


@pytest.fixture
def organization2():
    return Organization.objects.create(name='Test Org 2')


@pytest.fixture
def project(organization):
    return Project.objects.create(id='6abc33f0-4acf-4196-95ff-4cbb7f640a06',
                                  name='Test Project',
                                  organization=organization,
                                  apikey='eyJpZCI6ICJiZGU2NTRkZi0zMzQ2LTRiOWQtOGYxNC0zMDk2NzMwODI0NjAiLCAicHJvamVjdElkIjogIjAwMDAwMDAwLTAwMDAtMDAwMC0wMDAwLTAwMDAwMDAwMDAwMCIsICJhY2Nlc3NLZXkiOiAiRzl2WTFJaFhSSzdTekRENUdkWnludyIsICJuYW1lIjogIkFkbWluIENvbnNvbGUgR2VuZXJhdGVkIEtleSAtIDU4ZDIwNzUyLTQ1MTQtNDgwZC1iYTZmLTU4NTg2ZWYwM2ZlOSAtIHdhbGxldC1wcm9qZWN0LWtleS0wMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDAiLCAicGVybWlzc2lvbnMiOiBbIkFzc2V0c0RlbGV0ZSIsICJEYXRhU291cmNlTWFuYWdlIiwgIkRhdGFRdWV1ZU1hbmFnZSIsICJBc3NldHNJbXBvcnQiLCAiUHJvamVjdE1hbmFnZSIsICJBc3NldHNSZWFkIl0sICJ0aW1lQ3JlYXRlZCI6IDE2MTkwMzczODU5NDMsICJ0aW1lTW9kaWZpZWQiOiAxNjE5MDM3Mzg1OTQzLCAiYWN0b3JDcmVhdGVkIjogIjQzMzhhODNmLWE5MjAtNDBhYi1hMjUxLWExMjNiMTdkZjFiYS9hZG1pbi1rZXkiLCAiYWN0b3JNb2RpZmllZCI6ICI0MzM4YTgzZi1hOTIwLTQwYWItYTI1MS1hMTIzYjE3ZGYxYmEvYWRtaW4ta2V5IiwgImVuYWJsZWQiOiB0cnVlLCAic3lzdGVtS2V5IjogZmFsc2UsICJoaWRkZW4iOiB0cnVlLCAic2VjcmV0S2V5IjogIlN3V2NwWjdLeUtOMzJ5OE11V0V5ekEifQ==')


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
def project_zero(organization):
    return Project.objects.create(id='00000000-0000-0000-0000-000000000000',
                                  name='Project Zero', organization=organization)


@pytest.fixture
def project_zero_membership(project_zero, superuser, zmlp_apikey):
    apikey = b64encode(zmlp_apikey).decode('utf-8')
    return Membership.objects.create(user=superuser, project=project_zero,
                                     apikey=apikey, roles=['ML_Tools', 'API_Key', 'User_Admin'])


@pytest.fixture
def project_zero_user(superuser, project_zero_membership):
    return superuser


@pytest.fixture
def login(api_client, zmlp_project_user):
    api_client.force_authenticate(zmlp_project_user)
    api_client.force_login(zmlp_project_user)


@pytest.fixture
def logout(api_client):
    api_client.logout()
