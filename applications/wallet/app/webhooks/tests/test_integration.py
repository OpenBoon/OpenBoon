import pytest
from boonsdk import BoonClient
from rest_framework.reverse import reverse

from wallet.tests.utils import check_response
from webhooks.models import Trigger

pytestmark = pytest.mark.django_db


def test_webhook_util_list(login, api_client):
    response = check_response(api_client.get(reverse('webhook-util-list')))
    assert response == {'test': 'http://testserver/api/v1/webhooks/test/',
                        'triggers': 'http://testserver/api/v1/webhooks/triggers/'}


def test_webhook_util_list_logged_out(api_client, logout):
    check_response(api_client.get(reverse('webhook-util-list')), status=403)


def test_webhook_util_test(login, api_client, monkeypatch):
    monkeypatch.setattr(BoonClient, 'post', lambda *args: None)
    path = reverse('webhook-util-test')
    data = {'url': 'https://boonai.app/treble',
            'trigger': 'asset_analyzed'}
    check_response(api_client.post(path, data))


def test_webhook_util_trigger_list(login, api_client):
    Trigger.objects.create(name='test', displayName='Test', description='Test Trigger')
    path = reverse('webhook-util-trigger-list')
    response = check_response(api_client.get(path))
    assert response == {'count': 1, 'next': None, 'previous': None,
                        'results': [{'description': 'Test Trigger',
                                     'displayName': 'Test',
                                     'id': 1,
                                     'name': 'test'}]}
