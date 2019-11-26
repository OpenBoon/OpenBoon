import pytest
from django.http import JsonResponse, HttpResponseForbidden
from django.test import RequestFactory
from django.urls import reverse
from pixml import PixmlClient

from projects.clients import ZviClient
from projects.views import BaseProjectViewSet


pytestmark = pytest.mark.django_db


def test_get_zmlp_client(user, project, settings, zmlp_project_membership):
    settings.PLATFORM = 'zmlp'
    request = RequestFactory().get('/bunk/')
    request.user = user
    view = BaseProjectViewSet()
    client = view._get_archivist_client(request, project)
    assert type(client) == PixmlClient


def test_get_zvi_client(user, project, settings, zvi_project_membership):
    settings.PLATFORM = 'zvi'
    request = RequestFactory().get('/bunk/')
    request.user = user
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
    assert response.content == b'user is not a member of the project 6abc33f0-4acf-4196-95ff-4cbb7f640a06'


def test_projects_view_no_projects(project, user, api_client):
    api_client.force_authenticate(user)
    response = api_client.get(reverse('project-list')).json()
    assert response['count'] == 0


def test_projects_view_with_projects(project, user, api_client, zmlp_project_membership):
    api_client.force_authenticate(user)
    response = api_client.get(reverse('project-list')).json()
    assert response['count'] == 1
    assert response['results'][0]['name'] == project.name
