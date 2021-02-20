import pytest
from django.urls import reverse
from django.test import override_settings

from boonsdk import BoonClient

pytestmark = pytest.mark.django_db


@pytest.fixture
def data():
    return [
        {'name': 'SystemMonitor',
         'description': 'Allows access to monitoring endpoints'},
        {'name': 'SystemManage',
         'description': 'Allows access to platform management endpoints'},
        {'name': 'SystemProjectOverride',
         'description': 'Provides ability to switch projects'},
        {'name': 'SystemProjectDecrypt',
         'description': 'Provides ability to view encrypted project data'}
    ]


class TestPermission:

    @override_settings(PLATFORM='zmlp')
    def test_get_list(self, data, zmlp_project_user, project, api_client, monkeypatch):

        def mock_api_response(*args, **kwargs):
            return data

        monkeypatch.setattr(BoonClient, 'get', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('permission-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        content = response.json()
        assert len(content['results']) == 4
