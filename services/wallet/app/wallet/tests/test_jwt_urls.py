import pytest
from django.urls import reverse


pytestmark = pytest.mark.django_db


def test_obtain_token_pair(api_client, user):
    response = api_client.post('/auth/token/', {'username': 'user', 'password': 'letmein'},
                               format='json')
    assert response.status_code == 200
    assert 'refresh' in response.json()
    assert 'access' in response.json()


def test_refresh_a_token(api_client, user):
    json = api_client.post('/auth/token/', {'username': 'user', 'password': 'letmein'},
                           format='json').json()
    access = json['access']
    refresh = json['refresh']
    response = api_client.post('/auth/refresh/', {'refresh': refresh}, format='json')
    new_access = response.json()['access']
    assert response.status_code == 200
    assert access != new_access


def test_full_auth_flow_with_get_request(api_client, user):
    _json = api_client.post('/auth/token/',
                            {'username': 'user',
                             'password': 'letmein'},
                            format='json').json()
    access = _json['access']
    api_client.credentials(HTTP_AUTHORIZATION='Bearer {}'.format(access))
    response = api_client.get(reverse('user-list'), format='json')
    assert response.status_code == 200
