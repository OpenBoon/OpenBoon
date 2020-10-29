import json

from django.test import TestCase, Client
from django.urls import reverse
from django.contrib.auth import get_user_model


User = get_user_model()


class AuthTestCase(TestCase):

    def setUp(self):
        User.objects.create_user('user', 'user@fake.com', 'letmein')
        self.client = Client()


class LoginTestCase(AuthTestCase):

    def test_form_data(self):
        response = self.client.post(reverse('login'),
                                    {'username': 'user',
                                     'password': 'letmein'})
        self.assertEqual(response.status_code, 200)

    def test_json(self):
        response = self.client.post(reverse('login'),
                                    json.dumps({'username': 'user',
                                                'password': 'letmein'}),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 200)

    def test_inactive_user(self):
        user = User.objects.get(username='user')
        user.is_active = False
        user.save()

        response = self.client.post(reverse('login'),
                                    {'username': 'user',
                                     'password': 'letmein'})
        self.assertEqual(response.status_code, 401)
        self.assertEqual(response.json(), {'detail': 'No active user for the given '
                                                     'email/password combination found.'})


class LogoutTestCase(AuthTestCase):

    def test_logout(self):
        self.assertTrue(self.client.login(username='user',
                                          password='letmein'))
        response = self.client.post(reverse('logout'))
        self.assertEqual(response.status_code, 200)
