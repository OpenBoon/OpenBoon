import datetime

import pytest
from django.urls import reverse
from rest_framework import status

from agreements.models import Agreement

pytestmark = pytest.mark.django_db


class TestAgreementModel:

    def test_model(self, user):
        agreement = Agreement(user=user, privacy_policy_filename='privacy',
                              terms_and_conditions_filename='terms',
                              ip_address='127.0.0.1')
        agreement.save()
        agreement = Agreement.objects.get(id=agreement.id)
        assert agreement.user == user
        assert agreement.privacy_policy_filename == 'privacy'
        assert agreement.terms_and_conditions_filename == 'terms'
        assert agreement.ip_address == '127.0.0.1'
        assert agreement.created_date
        assert agreement.modified_date

    def test_str(self, user):
        date = datetime.datetime.now().strftime("%Y%m%d")
        agreement = Agreement(user=user)
        assert str(agreement) == f'{user} - {date} - Unsaved'
        agreement.save()
        assert str(agreement) == f'{user} - {date}'


class TestAgreementList:

    def test_get_list_no_agreements(self, zmlp_project_user, api_client, login):
        response = api_client.get(reverse('agreement-list', kwargs={'user_pk': str(zmlp_project_user.id)}))  # noqa
        assert response.status_code == 200
        assert response.json()['results'] == []

    def test_get_list(self, zmlp_project_user, api_client, login):
        agreement = Agreement(user=zmlp_project_user, privacy_policy_filename='privacy',
                              terms_and_conditions_filename='terms', ip_address='127.0.0.1')
        agreement.save()
        response = api_client.get(reverse('agreement-list', kwargs={'user_pk': str(zmlp_project_user.id)}))  # noqa
        assert response.status_code == 200
        content = response.json()
        assert content['count'] == 1
        result = content['results'][0]
        assert result['id']
        assert result['user'].endswith(f'api/v1/users/{zmlp_project_user.id}/')
        assert result['privacyPolicyFilename'] == 'privacy'
        assert result['termsAndConditionsFilename'] == 'terms'
        assert result['ipAddress'] == '127.0.0.1'
        assert result['createdDate']
        assert result['modifiedDate']


class TestAgreementCreate:

    def test_create_forwarded_for(self, zmlp_project_user, api_client, login):
        body = {}
        response = api_client.post(reverse('agreement-list', kwargs={'user_pk': str(zmlp_project_user.id)}),  # noqa
                                   body, **{'HTTP_X_FORWARDED_FOR': '127.0.0.1,proxy1,proxy2'})
        assert response.status_code == 201
        result = response.json()
        assert result['id']
        assert result['user'].endswith(f'api/v1/users/{zmlp_project_user.id}/')
        assert result['privacyPolicyFilename'] == ''
        assert result['termsAndConditionsFilename'] == ''
        assert result['ipAddress'] == '127.0.0.1'
        assert result['createdDate']
        assert result['modifiedDate']

    def test_create_remote_addr(self, zmlp_project_user, api_client, login):
        body = {}
        response = api_client.post(reverse('agreement-list', kwargs={'user_pk': str(zmlp_project_user.id)}), body)  # noqa
        assert response.status_code == 201
        result = response.json()
        assert result['ipAddress'] == '127.0.0.1'

    def test_create_for_different_user(self, zmlp_project_user, project_zero_user, api_client, login):  # noqa
        # Logged in as zmlp_project_user, post to project_zero_user
        response = api_client.post(reverse('agreement-list', kwargs={'user_pk': str(project_zero_user.id)}), {})  # noqa
        assert response.status_code == status.HTTP_403_FORBIDDEN
        assert response.json()['detail'] == 'Request user and context user do not match.'
