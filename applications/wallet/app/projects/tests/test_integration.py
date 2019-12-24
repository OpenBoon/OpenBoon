import json
import pytest
from base64 import b64encode
from django.http import JsonResponse, HttpResponseForbidden
from django.test import RequestFactory
from django.urls import reverse

from pixml import PixmlClient
from pixml.rest import PixmlDuplicateException

from projects.clients import ZviClient
from projects.views import BaseProjectViewSet
from projects.serializers import ProjectSerializer
from projects.models import Project, Membership


pytestmark = pytest.mark.django_db


def test_get_zmlp_client(pixml_project_user, project, settings):
    settings.PLATFORM = 'zmlp'
    request = RequestFactory().get('/bunk/')
    request.user = pixml_project_user
    view = BaseProjectViewSet()
    client = view._get_archivist_client(request, project)
    assert type(client) == PixmlClient


def test_get_zvi_client(zvi_project_user, project, settings):
    settings.PLATFORM = 'zvi'
    request = RequestFactory().get('/bunk/')
    request.user = zvi_project_user
    view = BaseProjectViewSet()
    client = view._get_archivist_client(request, project)
    assert type(client) == ZviClient


def test_project_view_user_does_not_belong_to_project(user, project):
    class FakeViewSet(BaseProjectViewSet):
        def get(self, request, project):
            return JsonResponse({'success': True})
    request = RequestFactory().get('/fake-path')
    request.user = user
    view = FakeViewSet()
    view.request = request
    view.args = []
    view.kwargs = {'project_pk': project.id}
    response = view.dispatch(view.request, *view.args, **view.kwargs)
    assert type(response) == HttpResponseForbidden
    assert response.content == (b'user is not a member of the project '
                                b'6abc33f0-4acf-4196-95ff-4cbb7f640a06')


def test_projects_view_no_projects(project, user, api_client):
    api_client.force_authenticate(user)
    response = api_client.get(reverse('project-list')).json()
    assert response['count'] == 0


def test_projects_view_with_projects(project, pixml_project_user, api_client):
    api_client.force_authenticate(pixml_project_user)
    response = api_client.get(reverse('project-list')).json()
    assert response['count'] == 1
    assert response['results'][0]['name'] == project.name


def test_project_serializer_detail(project):
    serializer = ProjectSerializer(project, context={'request': None})
    data = serializer.data
    expected_fields = ['id', 'name', 'url', 'jobs', 'apikeys', 'users']
    assert expected_fields == list(data.keys())
    assert data['id'] == project.id
    assert data['name'] == project.name
    assert data['users'] == []
    assert data['url'] == f'/api/v1/projects/{project.id}/'
    assert data['jobs'] == f'/api/v1/projects/{project.id}/jobs/'
    assert data['apikeys'] == f'/api/v1/projects/{project.id}/apikeys/'


def test_project_serializer_list(project, project2):
    queryset = Project.objects.all()
    serializer = ProjectSerializer(queryset, many=True, context={'request': None})
    data = serializer.data
    assert isinstance(data, list)
    assert len(data) == 2
    assert [entry['id'] for entry in data] == [project.id, project2.id]


class TestProjectViewSet:

    @pytest.fixture
    def project_zero(self):
        return Project.objects.create(id='00000000-0000-0000-0000-000000000000',
                                      name='Project Zero')

    @pytest.fixture
    def project_zero_membership(self, user, project_zero):
        apikey = {
            "name": "admin-key",
            "projectId": "00000000-0000-0000-0000-000000000000",
            "keyId": "123455678-a920-40ab-a251-a123b17df1ba",
            "sharedKey": "notyourbusiness",
            "permissions": [
                "SuperAdmin", "ProjectAdmin", "AssetsRead", "AssetsImport"
            ]
        }
        apikey = b64encode(json.dumps(apikey).encode('utf-8')).decode('utf-8')
        return Membership.objects.create(user=user, project=project_zero, apikey=apikey)

    @pytest.fixture
    def project_zero_user(self, user, project_zero_membership):
        return user

    def test_post_create(self, project_zero, project_zero_user, api_client, monkeypatch):

        def mock_api_response(*args, **kwargs):
            return True

        monkeypatch.setattr(PixmlClient, 'post', mock_api_response)
        api_client.force_authenticate(project_zero_user)

        with pytest.raises(Project.DoesNotExist):
            Project.objects.get(name='Create Project Test')

        body = {'name': 'Create Project Test'}
        response = api_client.post(reverse('project-list'), body)
        assert response.status_code == 201
        project = Project.objects.get(name='Create Project Test')
        assert project.name == 'Create Project Test'

    def test_post_create_no_project_zero(self, project, pixml_project_user, api_client):
        api_client.force_authenticate(pixml_project_user)
        body = {'name': 'Test Project'}
        response = api_client.post(reverse('project-list'), body)
        assert response.status_code == 403
        assert response.json()['detail'] == ('user is either not a member of Project Zero '
                                             'or the Project has not been created yet.')

    def test_post_create_bad_data(self, project_zero, project_zero_user, api_client):
        api_client.force_authenticate(project_zero_user)
        body = {'pointless': 'field'}
        response = api_client.post(reverse('project-list'), body)
        assert response.status_code == 400
        assert response.json()['name'][0] == 'This field is required.'

    def test_post_create_dup_pixml_project(self, project_zero, project_zero_user, api_client,
                                           monkeypatch):

        def mock_api_response(*args, **kwargs):
            raise PixmlDuplicateException(data={'msg': 'Duplicate'})

        api_client.force_authenticate(project_zero_user)
        body = {'name': 'Create Project Test'}
        monkeypatch.setattr(PixmlClient, 'post', mock_api_response)
        api_client.force_authenticate(project_zero_user)

        with pytest.raises(Project.DoesNotExist):
            Project.objects.get(name='Create Project Test')

        response = api_client.post(reverse('project-list'), body)
        assert response.status_code == 201
        project = Project.objects.get(name='Create Project Test')
        assert project.name == 'Create Project Test'
