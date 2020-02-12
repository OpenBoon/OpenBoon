import re
import uuid
from datetime import timedelta

import pytest
from django.contrib.auth.models import User
from django.utils.timezone import now
from rest_framework.reverse import reverse

from registration.models import UserRegistrationToken

pytestmark = pytest.mark.django_db


def test_register_user_invalid_password(api_client):
    api_client.logout()
    response = api_client.post(reverse('api-user-register'), {'email': 'fake@fakerson.com',
                                                              'firstName': 'Fakey',
                                                              'lastName': 'Fakerson',
                                                              'password': 'simple'})
    assert response.status_code == 422
    assert response.json()['message'] == 'Password not strong enough'


def test_register_invalid_request(api_client):
    api_client.logout()
    response = api_client.post(reverse('api-user-register'), {'email': 'fake@fakerson.com',
                                                              'firstName': 'Fakey',
                                                              'password': 'simple'})
    assert response.status_code == 400


def test_register_already_active_user(api_client, user):
    api_client.logout()
    response = api_client.post(reverse('api-user-register'), {'email': user.username,
                                                              'firstName': 'Fakey',
                                                              'lastName': 'Fakerson',
                                                              'password': uuid.uuid4()})
    assert response.status_code == 409


def test_re_register_user(api_client, user):
    api_client.logout()
    request_data = {'email': 'fake@fakerson.com',
                    'firstName': 'Fakey',
                    'lastName': 'Fakerson',
                    'password': uuid.uuid4()}
    response = api_client.post(reverse('api-user-register'), request_data)
    assert response.status_code == 200
    first_token = UserRegistrationToken.objects.get(user__username='fake@fakerson.com')
    response = api_client.post(reverse('api-user-register'), request_data)
    assert response.status_code == 200
    assert first_token != UserRegistrationToken.objects.get(
        user__username='fake@fakerson.com').token


def test_register_and_confirm(api_client, mailoutbox):
    api_client.logout()

    # Register a user
    response = api_client.post(reverse('api-user-register'), {'email': 'fake@fakerson.com',
                                                              'firstName': 'Fakey',
                                                              'lastName': 'Fakerson',
                                                              'password': uuid.uuid4()})
    assert response.status_code == 200
    user = User.objects.get(username='fake@fakerson.com')
    assert not user.is_active
    assert len(mailoutbox) == 1
    email = mailoutbox[0]
    search = re.search(r'token=(?P<token>.*)&userId=(?P<id>.*)', email.body)
    token = search.group('token')
    user_id = search.group('id')
    assert UserRegistrationToken.objects.filter(token=token, user=user_id).exists()

    # Activate the user's account.
    response = api_client.post(reverse('api-user-confirm'), {'token': token, 'userId': user_id})
    assert response.status_code == 200
    assert User.objects.get(id=user.id).is_active
    assert not UserRegistrationToken.objects.filter(token=token, user=user_id).exists()


def test_confirm_expired_registration_token(api_client, user):
    api_client.logout()
    token = UserRegistrationToken.objects.create(user=user)
    token.created_at = now() - timedelta(days=100)
    token.save()
    response = api_client.post(reverse('api-user-confirm'),
                               {'token': token.token, 'userId': user.id})
    assert response.status_code == 403
    assert response.content == b'The activation link has expired. Please sign up again.'


def test_confirm_missing_registration_token(api_client):
    api_client.logout()
    response = api_client.post(reverse('api-user-confirm'), {'token': uuid.uuid4(), 'userId': 1})
    assert response.status_code == 404


def test_confirm_missing_params(api_client):
    api_client.logout()
    response = api_client.post(reverse('api-user-confirm'), {'token': uuid.uuid4()})
    assert response.status_code == 400
