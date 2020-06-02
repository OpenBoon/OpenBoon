import pytest
from django.urls import reverse
from rest_framework import status

from subscriptions.models import Subscription
from wallet.tests.utils import check_response
from zmlp import ZmlpClient

pytestmark = pytest.mark.django_db


def test_subscripton_str(project):
    subscription = Subscription(project=project)
    assert str(subscription) == 'Test Project'


@pytest.fixture
def subscription(project):
    sub = Subscription(project=project)
    sub.save()
    return sub


class TestSubscriptionModel:
    def test_usage_last_hour(self, subscription, project_zero_membership, monkeypatch):
        def mock_get_return(*args, **kwargs):
            return [{
                "timestamp": 1588694400000,
                "videoSecondsCount": 10.00,
                "pageCount": 1,
                "videoFileCount": 1,
                "documentFileCount": 1,
                "imageFileCount": 1,
                "videoClipCount": 1
            }, {
                "timestamp": 1588698000000,
                "videoSecondsCount": 0.00,
                "pageCount": 0,
                "videoFileCount": 0,
                "documentFileCount": 0,
                "imageFileCount": 0,
                "videoClipCount": 0
            }, {
                "timestamp": 1588701600000,
                "videoSecondsCount": 20.00,
                "pageCount": 2,
                "videoFileCount": 2,
                "documentFileCount": 2,
                "imageFileCount": 2,
                "videoClipCount": 2
            }]

        monkeypatch.setattr(ZmlpClient, 'get', mock_get_return)
        expected = {'end_time': 1588701600.0,
                    'video_hours': 1,
                    'image_count': 2}
        assert subscription.usage_last_hour() == expected


class TestSubscriptionViewSet:

    def test_empty_list(self, zmlp_project_membership, api_client, login):
        project_pk = zmlp_project_membership.project_id
        response = api_client.get(reverse('subscription-list',
                                          kwargs={'project_pk': project_pk}))
        content = check_response(response)
        assert content['count'] == 0
        assert 'next' in content
        assert 'previous' in content

    def test_list(self, zmlp_project_membership, subscription, api_client, login, monkeypatch,
                  project_zero_membership):
        def mock_response(*args, **kwargs):
            return {'videoSecondsMax': 86400,
                    'videoSecondsCount': 0.0,
                    'pageMax': 10000,
                    'pageCount': 121}
        monkeypatch.setattr(ZmlpClient, 'get', mock_response)
        project_pk = zmlp_project_membership.project_id
        response = api_client.get(reverse('subscription-list',
                                          kwargs={'project_pk': project_pk}))
        content = check_response(response)
        result = content['results'][0]
        assert result['id'] == str(subscription.id)
        assert result['usage']['videoHours'] == 0.0
        assert result['usage']['imageCount'] == 121
        assert result['tier'] == 'essentials'

    def test_get_bad_pk(self, zmlp_project_membership, api_client, login):
        project_pk = zmlp_project_membership.project_id
        response = api_client.get(reverse('subscription-detail',
                                          kwargs={'project_pk': project_pk,
                                                  'pk': '114c021d-38a6-44cf-82f7-fb4c1fda1847'}))  # noqa
        check_response(response, status.HTTP_404_NOT_FOUND)

    def test_get_detail(self, zmlp_project_membership, subscription, api_client, login, monkeypatch,
                        project_zero_membership):
        def mock_response(*args, **kwargs):
            return {'videoSecondsMax': 86400,
                    'videoSecondsCount': 0.0,
                    'pageMax': 10000,
                    'pageCount': 121}
        monkeypatch.setattr(ZmlpClient, 'get', mock_response)
        project_pk = zmlp_project_membership.project_id
        response = api_client.get(reverse('subscription-detail',
                                          kwargs={'project_pk': project_pk,
                                                  'pk': subscription.id}))
        content = check_response(response)
        assert content['id'] == str(subscription.id)
        assert content['tier'] == 'essentials'
