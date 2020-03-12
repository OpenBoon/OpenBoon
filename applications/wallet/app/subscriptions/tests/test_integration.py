import pytest
from django.urls import reverse

from subscriptions.models import Subscription

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


class TestSubscriptionViewSet():

    def test_empty_list(self, zmlp_project_membership, api_client):
        api_client.force_authenticate(zmlp_project_membership.user)
        api_client.force_login(zmlp_project_membership.user)
        project_pk = zmlp_project_membership.project_id
        response = api_client.get(reverse('subscription-list',
                                          kwargs={'project_pk': project_pk}))
        assert response.status_code == 200
        content = response.json()
        assert content['count'] == 0
        assert 'next' in content
        assert 'previous' in content

    def test_list(self, zmlp_project_membership, subscription, api_client):
        api_client.force_authenticate(zmlp_project_membership.user)
        api_client.force_login(zmlp_project_membership.user)
        project_pk = zmlp_project_membership.project_id
        response = api_client.get(reverse('subscription-list',
                                          kwargs={'project_pk': project_pk}))
        assert response.status_code == 200
        content = response.json()
        result = content['results'][0]
        assert result['id'] == str(subscription.id)
        assert result['videoHoursLimit'] == 200
        assert result['imageCountLimit'] == 1000

    def test_get_bad_pk(self, zmlp_project_membership, api_client):
        api_client.force_authenticate(zmlp_project_membership.user)
        api_client.force_login(zmlp_project_membership.user)
        project_pk = zmlp_project_membership.project_id
        response = api_client.get(reverse('subscription-detail',
                                          kwargs={'project_pk': project_pk,
                                                  'pk': '114c021d-38a6-44cf-82f7-fb4c1fda1847'}))  # noqa
        assert response.status_code == 404

    def test_get_detail(self, zmlp_project_membership, subscription, api_client):
        api_client.force_authenticate(zmlp_project_membership.user)
        api_client.force_login(zmlp_project_membership.user)
        project_pk = zmlp_project_membership.project_id
        response = api_client.get(reverse('subscription-detail',
                                          kwargs={'project_pk': project_pk,
                                                  'pk': subscription.id}))
        assert response.status_code == 200
        content = response.json()
        assert content['id'] == str(subscription.id)
        assert content['videoHoursLimit'] == 200
        assert content['imageCountLimit'] == 1000
