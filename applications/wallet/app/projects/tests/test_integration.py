import pytest
from django.http import JsonResponse, HttpResponseForbidden
from django.test import RequestFactory
from django.urls import reverse

from pixml import PixmlClient

from projects.clients import ZviClient
from projects.views import BaseProjectViewSet
from projects.serializers import ProjectSerializer
from projects.models import Project


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
    expected_fields = ['id', 'name', 'users', 'jobs', 'url']
    assert expected_fields == list(data.keys())
    assert data['id'] == project.id
    assert data['name'] == project.name
    assert data['users'] == []
    assert data['url'] == f'/api/v1/projects/{project.id}/'
    assert data['jobs'] == f'/api/v1/projects/{project.id}/jobs/'


def test_project_serializer_list(project, project2):
    queryset = Project.objects.all()
    serializer = ProjectSerializer(queryset, many=True, context={'request': None})
    data = serializer.data
    assert isinstance(data, list)
    assert len(data) == 2
    assert [entry['id'] for entry in data] == [project.id, project2.id]
