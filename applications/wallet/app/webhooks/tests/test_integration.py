import pytest
from boonsdk import BoonClient
from rest_framework.reverse import reverse

from wallet.tests.utils import check_response
from webhooks.models import Trigger

pytestmark = pytest.mark.django_db


@pytest.fixture
def zmlp_webhook_response():
    return {'id': '02c93950-58bc-4a37-bfc7-d5945e080658', 'projectId': '1a0cbcd6-cf49-4992-a858-7966400082da', 'url': 'https://boonai.app/rattletrap', 'secretKey': 'secret', 'triggers': ['ASSET_ANALYZED'], 'active': True, 'timeCreated': 1617922719690, 'timeModified': 1617922719690, 'actorCreated': 'de19584d-56b7-4faa-8fcf-6c639ae7fd22/webhooks', 'actorModified': 'de19584d-56b7-4faa-8fcf-6c639ae7fd22/webhooks'}  # noqa


def test_webhook_util_list(login, api_client):
    response = check_response(api_client.get(reverse('webhook-util-list')))
    assert response == {'test': 'http://testserver/api/v1/webhooks/test/',
                        'triggers': 'http://testserver/api/v1/webhooks/triggers/'}


def test_webhook_util_list_logged_out(api_client, logout):
    check_response(api_client.get(reverse('webhook-util-list')), status=403)


def test_webhook_util_trigger_list(login, api_client):
    Trigger.objects.create(name='test', displayName='Test', description='Test Trigger')
    path = reverse('webhook-util-trigger-list')
    response = check_response(api_client.get(path))
    assert response == {'count': 1, 'next': None, 'previous': None,
                        'results': [{'description': 'Test Trigger',
                                     'displayName': 'Test',
                                     'id': 1,
                                     'name': 'test',
                                     'sort_index': 10}]}


def test_webhooks_create(login, api_client, monkeypatch, project, zmlp_webhook_response):
    monkeypatch.setattr(BoonClient, 'post', lambda *args, **kwargs: zmlp_webhook_response)
    path = reverse('webhook-list', kwargs={'project_pk': project.id})
    body = {'url': 'https://boonai.app/rattletrap',
            'secretKey': 'secret',
            'triggers': ['asset_analyzed']}
    response = check_response(api_client.post(path, body), status=201)
    assert response == {'active': True,
                        'id': '02c93950-58bc-4a37-bfc7-d5945e080658',
                        'projectId': '1a0cbcd6-cf49-4992-a858-7966400082da',
                        'secretKey': 'secret',
                        'timeCreated': 1617922719690,
                        'timeModified': 1617922719690,
                        'triggers': ['ASSET_ANALYZED'],
                        'url': 'https://boonai.app/rattletrap'}


def test_webhooks_list(login, api_client, monkeypatch, project, zmlp_webhook_response):
    zmlp_post_response = {'list': [zmlp_webhook_response], 'page': {'from': 0, 'size': 50, 'disabled': False, 'totalCount': 1}}  # noqa
    monkeypatch.setattr(BoonClient, 'post', lambda *args, **kwargs: zmlp_post_response)
    path = reverse('webhook-list', kwargs={'project_pk': project.id})
    response = check_response(api_client.get(path))
    assert response == {'count': 1,
                        'next': None,
                        'previous': None,
                        'results': [{'active': True,
                                     'id': '02c93950-58bc-4a37-bfc7-d5945e080658',
                                     'projectId': '1a0cbcd6-cf49-4992-a858-7966400082da',
                                     'secretKey': 'secret',
                                     'timeCreated': 1617922719690,
                                     'timeModified': 1617922719690,
                                     'triggers': ['ASSET_ANALYZED'],
                                     'url': 'https://boonai.app/rattletrap',
                                     'link': 'http://testserver/api/v1/projects/6abc33f0-4acf-4196-95ff-4cbb7f640a06/webhooks/02c93950-58bc-4a37-bfc7-d5945e080658/'}]}


def test_webhooks_retrieve(login, api_client, monkeypatch, project, zmlp_webhook_response):
    monkeypatch.setattr(BoonClient, 'get', lambda *args, **kwargs: zmlp_webhook_response)
    path = reverse('webhook-detail', kwargs={'project_pk': project.id, 'pk': 1})
    response = check_response(api_client.get(path))
    assert response == {'active': True,
                        'id': '02c93950-58bc-4a37-bfc7-d5945e080658',
                        'projectId': '1a0cbcd6-cf49-4992-a858-7966400082da',
                        'secretKey': 'secret',
                        'timeCreated': 1617922719690,
                        'timeModified': 1617922719690,
                        'triggers': ['ASSET_ANALYZED'],
                        'url': 'https://boonai.app/rattletrap'}


def test_webhooks_update(login, api_client, monkeypatch, project):
    zml_put_response = {'type': 'WebHook', 'id': '02c93950-58bc-4a37-bfc7-d5945e080658', 'op': 'update', 'success': True}  # noqa
    monkeypatch.setattr(BoonClient, 'put', lambda *args, **kwargs: zml_put_response)
    path = reverse('webhook-detail', kwargs={'project_pk': project.id, 'pk': 1})
    body = {'active': True,
            'id': '02c93950-58bc-4a37-bfc7-d5945e080658',
            'projectId': '1a0cbcd6-cf49-4992-a858-7966400082da',
            'secretKey': 'secret',
            'timeCreated': 1617922719690,
            'timeModified': 1617922719690,
            'triggers': ['ASSET_ANALYZED'],
            'url': 'https://boonai.app/rattletrap'}
    response = check_response(api_client.put(path, body))
    assert response == {'active': True,
                        'id': '02c93950-58bc-4a37-bfc7-d5945e080658',
                        'projectId': '1a0cbcd6-cf49-4992-a858-7966400082da',
                        'secretKey': 'secret',
                        'timeCreated': 1617922719690,
                        'timeModified': 1617922719690,
                        'triggers': ['ASSET_ANALYZED'],
                        'url': 'https://boonai.app/rattletrap'}


def test_webhooks_delete(login, api_client, monkeypatch, project):
    zmlp_delete_response = {'type': 'WebHook', 'id': '02c93950-58bc-4a37-bfc7-d5945e080658', 'op': 'delete', 'success': True}  # noqa
    monkeypatch.setattr(BoonClient, 'delete', lambda *args, **kwargs: zmlp_delete_response)
    path = reverse('webhook-detail', kwargs={'project_pk': project.id, 'pk': 1})
    response = check_response(api_client.delete(path))
    assert response == {'detail': ['Successfully deleted resource.']}


def test_webhooks_test_success(login, api_client, monkeypatch):
    zmlp_post_response = {'type': 'webhook', 'op': '_test', 'success': True}
    monkeypatch.setattr(BoonClient, 'post', lambda *args, **kwargs: zmlp_post_response)
    path = reverse('webhook-util-test')
    body = {'url': 'https://boonai.app/rattletrap',
            'secretKey': 'secret',
            'triggers': ['asset_analyzed']}
    check_response(api_client.post(path, body))


def test_webhooks_test_failure(login, api_client, monkeypatch):
    zmlp_post_response = {'type': 'webhook', 'op': '_test', 'success': False}
    monkeypatch.setattr(BoonClient, 'post', lambda *args, **kwargs: zmlp_post_response)
    path = reverse('webhook-util-test')
    body = {'url': 'https://boonai.app/rattletrap',
            'secretKey': 'secret',
            'triggers': ['asset_analyzed']}
    check_response(api_client.post(path, body), status=500)


def test_trigger_sort_order():
    Trigger.objects.create(name='three', displayName='three', sort_index=3)
    Trigger.objects.create(name='one', displayName='one', sort_index=1)
    Trigger.objects.create(name='two', displayName='two', sort_index=2)
    trigger_order = [t.name for t in Trigger.objects.all()]
    assert trigger_order == ['one', 'two', 'three']
