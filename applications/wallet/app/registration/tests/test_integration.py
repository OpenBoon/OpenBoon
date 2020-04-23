import re
import uuid
from datetime import timedelta

import pytest
from django.contrib.auth.models import User
from django.utils.timezone import now
from rest_framework.reverse import reverse

from agreements.models import Agreement
from registration.models import UserRegistrationToken

pytestmark = pytest.mark.django_db


def test_register_user_invalid_password(api_client):
    api_client.logout()
    response = api_client.post(reverse('api-user-register'), {'email': 'fake@fakerson.com',
                                                              'firstName': 'Fakey',
                                                              'lastName': 'Fakerson',
                                                              'password': 'simple'})
    assert response.status_code == 422
    assert response.json()['detail'] == 'Password not strong enough'


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
    password = uuid.uuid4()
    response = api_client.post(reverse('api-user-register'), {'email': 'fake@fakerson.com',
                                                              'firstName': 'Fakey',
                                                              'lastName': 'Fakerson',
                                                              'password': password,
                                                              'policiesDate': '20010101'})
    assert response.status_code == 200
    user = User.objects.get(username='fake@fakerson.com')
    assert not user.is_active
    assert len(mailoutbox) == 1
    email = mailoutbox[0]
    search = re.search(r'token=(?P<token>.*)&userId=(?P<id>.*)', email.body)
    token = search.group('token')
    user_id = search.group('id')
    assert UserRegistrationToken.objects.filter(token=token, user=user_id).exists()
    assert Agreement.objects.get(user=user)

    # Activate the user's account.
    response = api_client.post(reverse('api-user-confirm'), {'token': token, 'userId': user_id})
    assert response.status_code == 200
    user = User.objects.get(id=user.id)
    assert user.is_active
    assert user.check_password(password)
    assert not UserRegistrationToken.objects.filter(token=token, user=user_id).exists()


def test_register_confirm_with_policies_date(api_client, mailoutbox):
    api_client.logout()

    # Register a user
    password = uuid.uuid4()
    headers = {'HTTP_X_FORWARDED_FOR': '172.19.0.1',
               'REMOTE_ADDR': '127.0.0.1'}
    body = {'email': 'fake@fakerson.com',
            'firstName': 'Fakey',
            'lastName': 'Fakerson',
            'password': password,
            'policiesDate': '20200626'}
    response = api_client.post(reverse('api-user-register'), body, **headers)
    assert response.status_code == 200
    user = User.objects.get(username='fake@fakerson.com')
    assert not user.is_active
    assert len(mailoutbox) == 1
    email = mailoutbox[0]
    search = re.search(r'token=(?P<token>.*)&userId=(?P<id>.*)', email.body)
    token = search.group('token')
    user_id = search.group('id')
    assert UserRegistrationToken.objects.filter(token=token, user=user_id).exists()
    agreement = Agreement.objects.get(user=user)
    assert agreement.policies_date == '20200626'

    # Activate the user's account.
    response = api_client.post(reverse('api-user-confirm'), {'token': token, 'userId': user_id})
    assert response.status_code == 200
    user = User.objects.get(id=user.id)
    assert user.is_active
    assert user.check_password(password)
    assert not UserRegistrationToken.objects.filter(token=token, user=user_id).exists()


def test_confirm_expired_registration_token(api_client, user):
    api_client.logout()
    token = UserRegistrationToken.objects.create(user=user)
    token.created_at = now() - timedelta(days=100)
    token.save()
    response = api_client.post(reverse('api-user-confirm'),
                               {'token': token.token, 'userId': user.id})
    assert response.status_code == 403
    assert response.data['detail'] == 'The activation link has expired. Please sign up again.'


def test_confirm_missing_registration_token(api_client):
    api_client.logout()
    response = api_client.post(reverse('api-user-confirm'), {'token': uuid.uuid4(), 'userId': 1})
    assert response.status_code == 404


def test_confirm_missing_params(api_client):
    api_client.logout()
    response = api_client.post(reverse('api-user-confirm'), {'token': uuid.uuid4()})
    assert response.status_code == 400


def test_password_change(api_client, user):
    api_client.force_login(user)
    response = api_client.post(reverse('api-password-change'),
                               {'oldPassword': 'letmein',
                                'newPassword1': '7BMQv5Pb(KpdS+!z',
                                'newPassword2': '7BMQv5Pb(KpdS+!z'})
    assert response.status_code == 200
    user = User.objects.get(username=user.username)
    assert user.check_password('7BMQv5Pb(KpdS+!z')


def test_reset_password(api_client, user, mailoutbox):
    api_client.logout()
    assert not user.check_password('7BMQv5Pb(KpdS+!z')
    response = api_client.post(reverse('api-password-reset'), {'email': user.email})
    assert response.status_code == 200
    message = mailoutbox[0]
    assert message.subject == 'Wallet Password Reset'
    search = re.search(r'token=(?P<token>.*)&uid=(?P<id>.*)', message.body)
    token = search.group('token')
    uid = search.group('id')
    response = api_client.post(reverse('api-password-reset-confirm'),
                               {'new_password1': '7BMQv5Pb(KpdS+!z',
                                'new_password2': '7BMQv5Pb(KpdS+!z',
                                'uid': uid, 'token': token})
    assert response.status_code == 200
    user = User.objects.get(username=user.username)
    assert user.check_password('7BMQv5Pb(KpdS+!z')
