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


def test_api_login_user_pass(api_client, user):
    api_client.logout()
    response = api_client.post(reverse('api-login'),
                               {'username': 'user', 'password': 'letmein'})
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['email'] == 'user@fake.com'
    assert response_data['username'] == 'user'
    assert response_data['first_name'] == ''
    assert response_data['last_name'] == ''


def test_api_login_fail(api_client, user):
    response = api_client.post(reverse('api-login'),
                               {'username': 'user', 'password': 'fail'})
    assert response.status_code == 401


def api_login_google_ouath_provision(api_client, monkeypatch):
    def verify_oauth2_token(*args, **kwargs):
        return
    monkeypatch.setattr(id_token, 'verify_oauth2_token')

def test_api_logout(api_client, user):
    api_client.force_login(user)
    assert api_client.get(reverse('project-list')).status_code == 200
    response = api_client.post(reverse('api-logout'), {})
    assert response.status_code == 200
    assert api_client.get(reverse('project-list')).status_code == 403
