import datetime
import re
import uuid
from datetime import timedelta

import axes.utils
import pytest
import pytz
from django.conf import settings
from django.contrib.auth import get_user_model
from django.urls import reverse
from django.utils.timezone import now
from google.oauth2 import id_token
from rest_framework.reverse import reverse

from agreements.models import Agreement
from registration.models import UserRegistrationToken
from wallet.tests.utils import check_response

pytestmark = pytest.mark.django_db
User = get_user_model()


def test_register_user_invalid_password(api_client):
    api_client.logout()
    response = api_client.post(reverse('api-user-register'), {'email': 'fake@fakerson.com',
                                                              'firstName': 'Fakey',
                                                              'lastName': 'Fakerson',
                                                              'password': 'simple'})
    assert response.status_code == 422
    assert response.json()['detail'] == ['Password not strong enough.']


def test_register_user_with_invalid_email(api_client):
    api_client.logout()
    response = api_client.post(reverse('api-user-register'), {'email': 'test@ise.io"or"2""="2"',
                                                              'firstName': 'Fakey',
                                                              'lastName': 'Fakerson',
                                                              'password': uuid.uuid4()})
    assert response.status_code == 422
    assert response.json()['detail'] == ['Email address invalid.']


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
    assert response.status_code == 200
    assert response.data['detail'] == ['Success, confirmation email has been sent.']


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
    password = str(uuid.uuid4())
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
    password = str(uuid.uuid4())
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
    assert agreement.policiesDate == '20200626'

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
    token.createdAt = now() - timedelta(days=100)
    token.save()
    response = api_client.post(reverse('api-user-confirm'),
                               {'token': token.token, 'userId': user.id})
    assert response.status_code == 403
    assert response.data['detail'] == ['The activation link has expired. Please sign up again.']


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
    assert message.subject == 'Boon AI Password Reset'
    search = re.search(r'token=(?P<token>.*)&uid=(?P<id>.*)', message.body)
    token = search.group('token')
    uid = search.group('id')
    response = api_client.post(reverse('api-password-reset-confirm'),
                               {'newPassword1': '7BMQv5Pb(KpdS+!z',
                                'newPassword2': '7BMQv5Pb(KpdS+!z',
                                'uid': uid, 'token': token})
    assert response.status_code == 200
    user = User.objects.get(username=user.username)
    assert user.check_password('7BMQv5Pb(KpdS+!z')


def test_api_login_user_pass(api_client, user):
    api_client.logout()
    response = api_client.post(reverse('api-login'),
                               {'username': 'user', 'password': 'letmein'})
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['email'] == 'user@fake.com'
    assert response_data['username'] == 'user'
    assert response_data['firstName'] == ''
    assert response_data['lastName'] == ''


def test_api_login_includes_projects(api_client, user, project, project2,
                                     zmlp_project_membership, zmlp_project2_membership):
    api_client.logout()
    response = api_client.post(reverse('api-login'),
                               {'username': 'user', 'password': 'letmein'})
    assert response.status_code == 200
    response_data = response.json()
    expected_data = {
        '6abc33f0-4acf-4196-95ff-4cbb7f640a06': ['ML_Tools', 'User_Admin'],
        'e93cbadb-e5ae-4598-8395-4cf5b30c0e94': ['ML_Tools', 'User_Admin', 'API_Keys']
    }

    assert len(response_data['roles']) == 2
    assert response_data['roles'] == expected_data


def test_api_login_includes_invalid_agreement(api_client, user):
    api_client.logout()
    response = api_client.post(reverse('api-login'),
                               {'username': 'user', 'password': 'letmein'})
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['agreedToPoliciesDate'] == '00000000'


def test_api_login_includes_agreement_date(api_client, user):
    timezone = pytz.timezone('America/Los_Angeles')

    date = datetime.datetime(2019, 12, 8, 0, 0)
    agreement = Agreement(user=user, policiesDate='20191204')
    agreement.save()
    agreement.createdDate = timezone.localize(date)
    agreement.save()

    date = datetime.datetime(2019, 12, 1, 0, 0)
    agreement2 = Agreement(user=user, policiesDate='20191122')
    agreement2.save()
    agreement2.createdDate = timezone.localize(date)
    agreement2.save()

    api_client.logout()
    response = api_client.post(reverse('api-login'),
                               {'username': 'user', 'password': 'letmein'})
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['agreedToPoliciesDate'] == '20191204'


def test_api_login_inactive_user_fail(api_client, user):
    api_client.logout()
    user.is_active = False
    user.save()
    response = api_client.post(reverse('api-login'),
                               {'username': 'user', 'password': 'letmein'})
    assert response.status_code == 401


def test_api_login_fail(api_client, user):
    response = api_client.post(reverse('api-login'),
                               {'username': 'user', 'password': 'fail'})
    assert response.status_code == 401


def test_api_login_google_oauth_provision_user(api_client, monkeypatch):
    def verify_oauth2_token(*args, **kwargs):
        return {
            'iss': 'accounts.google.com',
            'azp': '683985502197-140kfdlheorkbc6e9vo748v5260df3pf.apps.googleusercontent.com',
            'aud': '683985502197-140kfdlheorkbc6e9vo748v5260df3pf.apps.googleusercontent.com',
            'sub': '115808488052074574004',
            'hd': 'fake.com',
            'email': 'new@fake.com',
            'email_verified': True,
            'at_hash': 'TXWy0vT7thdC6rayWWrYYw',
            'name': 'New Person',
            'picture': 'https://lh3.googleusercontent.com/a-/AAuE7mBr7AD-MLBUKTGiUs-'
                       'bMis5M5YNnhva_WeY7Oj8=s96-c',
            'given_name': 'New',
            'family_name': 'Person',
            'locale': 'en',
            'iat': 1576801404,
            'exp': 1576805004,
            'jti': 'd07a7fced948ff06cce42a1327a68152b8cedaad'
        }
    monkeypatch.setattr(id_token, 'verify_oauth2_token', verify_oauth2_token)
    response = api_client.post(reverse('api-login'), json={},
                               HTTP_Authorization='Bearer skdhjflkjsdhfjks')
    assert response.status_code == 200
    user = User.objects.get(email='new@fake.com')
    assert user.first_name == 'New'
    assert user.last_name == 'Person'
    assert user.username == 'new@fake.com'


def test_api_login_google_oauth_existing_user(api_client, monkeypatch, user):
    def verify_oauth2_token(*args, **kwargs):
        return {
            'iss': 'accounts.google.com',
            'azp': '683985502197-140kfdlheorkbc6e9vo748v5260df3pf.apps.googleusercontent.com',
            'aud': '683985502197-140kfdlheorkbc6e9vo748v5260df3pf.apps.googleusercontent.com',
            'sub': '115808488052074574004',
            'hd': 'fake.com',
            'email': 'user@fake.com',
            'email_verified': True,
            'at_hash': 'TXWy0vT7thdC6rayWWrYYw',
            'name': 'New Person',
            'picture': 'https://lh3.googleusercontent.com/a-/AAuE7mBr7AD-MLBUKTGiUs-'
                       'bMis5M5YNnhva_WeY7Oj8=s96-c',
            'given_name': 'New',
            'family_name': 'Person',
            'locale': 'en',
            'iat': 1576801404,
            'exp': 1576805004,
            'jti': 'd07a7fced948ff06cce42a1327a68152b8cedaad'
        }
    monkeypatch.setattr(id_token, 'verify_oauth2_token', verify_oauth2_token)
    response = api_client.post(reverse('api-login'), json={},
                               HTTP_Authorization='Bearer skdhjflkjsdhfjks')
    assert response.status_code == 200
    user = User.objects.get(email='user@fake.com')
    assert user.first_name == ''
    assert user.last_name == ''
    assert user.username == 'user'


def test_api_login_google_oauth_bad_issuer(api_client, monkeypatch, user):
    def verify_oauth2_token(*args, **kwargs):
        return {
            'iss': 'accounts.malicious.com',
            'azp': '683985502197-140kfdlheorkbc6e9vo748v5260df3pf.apps.googleusercontent.com',
            'aud': '683985502197-140kfdlheorkbc6e9vo748v5260df3pf.apps.googleusercontent.com',
            'sub': '115808488052074574004',
            'hd': 'fake.com',
            'email': 'user@fake.com',
            'email_verified': True,
            'at_hash': 'TXWy0vT7thdC6rayWWrYYw',
            'name': 'New Person',
            'picture': 'https://lh3.googleusercontent.com/a-/AAuE7mBr7AD-MLBUKTGiUs-'
                       'bMis5M5YNnhva_WeY7Oj8=s96-c',
            'given_name': 'New',
            'family_name': 'Person',
            'locale': 'en',
            'iat': 1576801404,
            'exp': 1576805004,
            'jti': 'd07a7fced948ff06cce42a1327a68152b8cedaad'
        }
    monkeypatch.setattr(id_token, 'verify_oauth2_token', verify_oauth2_token)
    response = api_client.post(reverse('api-login'), json={},
                               HTTP_Authorization='Bearer skdhjflkjsdhfjks')
    assert response.status_code == 401


def test_get_me(login, api_client, project):
    response = api_client.get(reverse('me'))
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['email'] == 'user@fake.com'
    assert response_data['username'] == 'user'
    assert response_data['firstName'] == ''
    assert response_data['lastName'] == ''
    assert response_data['roles'] == {str(project.id): ['ML_Tools', 'User_Admin']}


def test_get_me_org_owner(api_client, project):
    api_client.force_authenticate(project.organization.owners.first())
    api_client.force_login(project.organization.owners.first())
    response = api_client.get(reverse('me'))
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['email'] == 'software@zorroa.com'
    assert response_data['username'] == 'superuser'
    assert response_data['firstName'] == ''
    assert response_data['lastName'] == ''
    assert response_data['roles'] == {str(project.id): ['ML_Tools', 'API_Keys', 'User_Admin']}


def test_patch_me(login, api_client):
    response = check_response(api_client.patch(reverse('me'),
                                               {'firstName': 'some', 'lastName': 'body'}))
    assert response['email'] == 'user@fake.com'
    assert response['username'] == 'user'
    assert response['firstName'] == 'some'
    assert response['lastName'] == 'body'


def test_api_logout(api_client, user):
    api_client.logout()
    api_client.force_login(user)
    assert api_client.get(reverse('project-list')).status_code == 200
    response = api_client.post(reverse('api-logout'), {})
    assert response.status_code == 200
    assert api_client.get(reverse('project-list')).status_code == 403


def test_api_login_lockout(api_client, user):
    api_client.logout()
    axes.utils.reset()
    credentials = {'username': user.username, 'password': 'nope'}

    for i in range(settings.AXES_FAILURE_LIMIT - 1):
        response = api_client.post(reverse('api-login'), credentials)
        assert response.json()['detail'] == ['Invalid email and password combination.']

    # Third failed attempt.
    response = api_client.post(reverse('api-login'), credentials)
    assert response.status_code == 423
    assert response.json()['detail'] == ['This account has been locked due to too many '
                                         'failed login attempts. Please contact support to '
                                         'unlock your account.']
