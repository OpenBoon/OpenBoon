import pytest
from django.conf import settings
from django.test import Client


@pytest.fixture
def client():
    return Client()


@pytest.fixture
def user(django_user_model):
    user = django_user_model.objects.create_user('user', 'user@fake.com', 'letmein')
    return user


@pytest.fixture
def superuser(django_user_model):
    user = django_user_model.objects.create_superuser('superuser', settings.SUPERUSER_EMAIL,
                                                      'letmein')
    return user
