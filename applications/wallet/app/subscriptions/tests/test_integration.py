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
    sub = Subscription(project=project, video_hours_limit=200, image_count_limit=1000,
                       modules='zmlp-classification,zmlp-objects')
    sub.save()
    return sub


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
        assert result['limits']['videoHours'] == 200
        assert result['limits']['imageCount'] == 1000
        assert result['usage']['videoHours'] == 0.0
        assert result['usage']['imageCount'] == 121

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
        assert content['limits']['videoHours'] == 200
        assert content['limits']['imageCount'] == 1000
