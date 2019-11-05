from django.test import TestCase
from django.urls import reverse
from django.contrib.auth.models import User
from rest_framework.test import APIClient



class FrontendViewTests(TestCase):

    def setUp(self):
        self.client = APIClient()

    def test_index_file_returned(self):
        response = self.client.get('/')
        assert response.status_code == 200
        assert response.content.decode('utf-8').startswith('<!doctype html>')


class UsersTests(TestCase):

    def setUp(self):
        self.client = APIClient()

    def test_get_users_no_permissions(self):
        response = self.client.get(reverse('user-list'))
        assert response.status_code == 401

    def test_get_users(self):
        user = User.objects.get(username='admin')
        self.client.force_authenticate(user=user)
        response = self.client.get(reverse('user-list'))
        assert response.status_code == 200


