import pytest
from django.urls import reverse

pytestmark = pytest.mark.django_db


class TestRolesViewSet():

    def test_get_list(self, project, api_client, login):
        response = api_client.get(reverse('role-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        content = response.json()
        assert len(content['results']) == 4
        assert 'name' in content['results'][0]
        assert 'description' in content['results'][0]
        assert 'permissions' in content['results'][0]
