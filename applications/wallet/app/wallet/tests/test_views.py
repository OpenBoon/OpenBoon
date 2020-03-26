import re

import pytest
from django.contrib.auth.models import User
from django.urls import reverse
from google.oauth2 import id_token

pytestmark = pytest.mark.django_db


def test_get_users_no_permissions(api_client, user):
    api_client.logout()
    response = api_client.get(reverse('user-list'))
    assert response.status_code == 403


def test_get_users(api_client, superuser):
    api_client.force_authenticate(superuser)
    response = api_client.get(reverse('user-list'))
    assert response.status_code == 200


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


def test_api_logout(api_client, user):
    api_client.force_login(user)
    assert api_client.get(reverse('project-list')).status_code == 200
    response = api_client.post(reverse('api-logout'), {})
    assert response.status_code == 200
    assert api_client.get(reverse('project-list')).status_code == 403


def test_password_change(api_client, user):
    api_client.force_login(user)
    response = api_client.post(reverse('api-password-change'),
                               {'old_password': 'letmein',
                                'new_password1': '7BMQv5Pb(KpdS+!z',
                                'new_password2': '7BMQv5Pb(KpdS+!z'})
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
    search = re.search(r'token=(?P<token>.*)&uid=(?P<uid>.*)', message.body)
    token = search.group('token')
    uid = search.group('uid')
    response = api_client.post(reverse('api-password-reset-confirm'),
                               {'new_password1': '7BMQv5Pb(KpdS+!z',
                                'new_password2': '7BMQv5Pb(KpdS+!z',
                                'uid': uid, 'token': token})
    assert response.status_code == 200
    user = User.objects.get(username=user.username)
    assert user.check_password('7BMQv5Pb(KpdS+!z')
