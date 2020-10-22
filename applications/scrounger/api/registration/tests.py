import pytest
from django.urls import reverse


pytestmark = pytest.mark.django_db


def test_login(client, user):
    response = client.post(reverse('login'),
                           {'username': 'user',
                            'password': 'letmein'})
    assert response.status_code == 200


def test_logout(client, user):
    assert client.login(username='user', password='letmein')
    response = client.post(reverse('logout'))
    assert response.status_code == 200
