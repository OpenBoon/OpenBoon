import json
import pytest

from django.urls import reverse


pytestmark = pytest.mark.django_db


class LoginTests:

    def test_form_data(self, client, user):
        response = client.post(reverse('login'),
                               {'username': 'user',
                                'password': 'letmein'})
        assert response.status_code == 200


    def test_json(self, client, user):
        response = client.post(reverse('login'),
                               json.dumps({'username': 'user', 'password': 'letmein'}),
                               content_type='application/json')
        assert response.status_code == 200


    def test_inactive_user(self, client, user):
        user.is_active = False
        user.save()

        response = client.post(reverse('login'),
                               {'username': 'user',
                                'password': 'letmein'})
        assert response.status_code == 401
        assert response.json() == {'detail': 'No active user for the given '
                                             'email/password combination found.'}


def test_logout(client, user):
    assert client.login(username='user', password='letmein')
    response = client.post(reverse('logout'))
    assert response.status_code == 200
