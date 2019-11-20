import pytest
from django.urls import reverse


pytestmark = pytest.mark.django_db


def test_index_file_returned(api_client):
    response = api_client.get('/')
    assert response.status_code == 200
    assert response.content.decode('utf-8').startswith('<!DOCTYPE html>')


def test_get_users_no_permissions(api_client, user):
    api_client.logout()
    response = api_client.get(reverse('user-list'))
    assert response.status_code == 401


def test_get_users(api_client, superuser):
    api_client.force_authenticate(superuser)
    response = api_client.get(reverse('user-list'))
    assert response.status_code == 200


