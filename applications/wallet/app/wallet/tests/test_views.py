import pytest
from django.urls import reverse


pytestmark = pytest.mark.django_db


def test_get_users_no_permissions(api_client, user):
    api_client.logout()
    response = api_client.get(reverse('user-list'))
    assert response.status_code == 403


def test_get_users(api_client, superuser):
    api_client.force_authenticate(superuser)
    response = api_client.get(reverse('user-list'))
    assert response.status_code == 200


def test_api_login(api_client, user):
    api_client.logout()
    response = api_client.post(reverse('api-login'),
                               {'username': 'user', 'password': 'letmein'})
    assert response.status_code == 200
    assert response.json() == {'email': 'user@fake.com', 'first_name': '',
                               'last_name': '', 'username': 'user', 'id': user.id}


def test_api_logout(api_client, user):
    api_client.force_login(user)
    assert api_client.get(reverse('project-list')).status_code == 200
    response = api_client.post(reverse('api-logout'), {})
    assert response.status_code == 200
    assert api_client.get(reverse('project-list')).status_code == 403
