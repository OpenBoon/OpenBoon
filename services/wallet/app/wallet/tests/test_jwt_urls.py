from django.test import TestCase
from django.urls import reverse
from rest_framework.test import APIClient


class JwtTokenTests(TestCase):

    def setUp(self):
        self.client = APIClient()

    def test_obtain_token_pair(self):
        response = self.client.post('/auth/token/',
                                    {'username': 'admin',
                                     'password': 'admin'},
                                    format='json')
        assert response.status_code == 200
        assert 'refresh' in response.json()
        assert 'access' in response.json()

    def test_refresh_a_token(self):
        json = self.client.post('/auth/token/',
                                {'username': 'admin',
                                 'password': 'admin'},
                                format='json').json()
        access = json['access']
        refresh = json['refresh']
        response = self.client.post('/auth/refresh/',
                                    {'refresh': refresh},
                                    format='json')
        new_access = response.json()['access']
        assert response.status_code == 200
        assert access != new_access

    def test_full_auth_flow_with_get_request(self):
        json = self.client.post('/auth/token/',
                                {'username': 'admin',
                                 'password': 'admin'},
                                format='json').json()
        access = json['access']
        self.client.credentials(HTTP_AUTHORIZATION='Bearer {}'.format(access))
        response = self.client.get(reverse('user-list'), format='json')
        assert response.status_code == 200





