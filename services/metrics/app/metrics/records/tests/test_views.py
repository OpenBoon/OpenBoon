import pytest
from django.conf import settings
from django.urls import reverse

import boonsdk
from metrics.records.models import ApiCall

pytestmark = pytest.mark.django_db


class TestAPICallsViewSet:

    def test_get_apicalls_empty(self, api_client):
        response = api_client.get(reverse('apicalls-list'))
        assert response.json() == {'count': 0, 'next': None,
                                   'previous': None, 'results': []}

    def test_get_single_record(self, api_client, single_record):
        url = reverse('apicalls-detail', kwargs={'pk': single_record.id})
        response = api_client.get(url)
        assert response.status_code == 200
        content = response.json()
        assert content['id'] == single_record.id
        assert content['project'] == single_record.project
        assert content['asset_id'] == single_record.asset_id
        assert content['asset_path'] == single_record.asset_path
        assert content['image_count'] == single_record.image_count
        assert content['video_seconds'] == single_record.video_seconds
        assert 'created_date' in content
        assert 'modified_date' in content

    def test_report_action(self, api_client, test_set):
        response = api_client.get(reverse('apicalls-report'))
        assert response.status_code == 200
        content = response.json()
        assert content == [{'image_count': 1,
                            'project': '00000000-0000-0000-0000-000000000000',
                            'service': 'boonai-object-detection',
                            'tier': 'tier_1',
                            'video_seconds': 7.17,
                            'video_minutes': 0.1195,
                            'video_hours': 0.0019916666666666668},
                           {'image_count': 10,
                            'project': '00000000-0000-0000-0000-000000000000',
                            'service': 'gcp-label-detection',
                            'tier': 'tier_2',
                            'video_seconds': 46.51,
                            'video_minutes': 0.7751666666666667,
                            'video_hours': 0.012919444444444445},
                           {'image_count': 15,
                            'project': '00000000-0000-0000-0000-000000000000',
                            'service': 'standard',
                            'tier': 'free',
                            'video_seconds': 73.13,
                            'video_minutes': 1.2188333333333332,
                            'video_hours': 0.02031388888888889},
                           {'image_count': 11,
                            'project': '11111111-1111-1111-1111-111111111111',
                            'service': 'boonai-object-detection',
                            'tier': 'tier_1',
                            'video_seconds': 45.66,
                            'video_minutes': 0.7609999999999999,
                            'video_hours': 0.012683333333333331},
                           {'image_count': 10,
                            'project': '11111111-1111-1111-1111-111111111111',
                            'service': 'standard',
                            'tier': 'free',
                            'video_seconds': 48.8,
                            'video_minutes': 0.8133333333333332,
                            'video_hours': 0.013555555555555553},
                           {'image_count': 1,
                            'project': '22222222-2222-2222-2222-222222222222',
                            'service': 'boonai-label-detection',
                            'tier': 'tier_1',
                            'video_seconds': 2.5,
                            'video_minutes': 0.041666666666666664,
                            'video_hours': 0.0006944444444444444},
                           {'image_count': 12,
                            'project': '22222222-2222-2222-2222-222222222222',
                            'service': 'boonai-object-detection',
                            'tier': 'tier_1',
                            'video_seconds': 52.55,
                            'video_minutes': 0.8758333333333332,
                            'video_hours': 0.014597222222222222}]

    def test_report_csv(self, api_client, test_set):
        response = api_client.get(reverse('apicalls-report'), content_type='text/csv',
                                  HTTP_ACCEPT='text/csv')
        content = response.rendered_content.decode('utf-8').strip().split('\r\n')
        assert len(content) == 8
        assert content[0] == 'project,service,tier,image_count,video_seconds'
        assert response['content-disposition'] == 'attachment; filename=billing_report.csv'

    def test_report_csv_custom_filename(self, api_client, test_set):
        response = api_client.get(reverse('apicalls-report'),
                                  {'filename': 'my_custom_name.csv'},
                                  content_type='text/csv',
                                  HTTP_ACCEPT='text/csv')
        assert response['content-disposition'] == 'attachment; filename=my_custom_name.csv'

    def test_report_csv_dated_filename(self, api_client, test_set):
        response = api_client.get(reverse('apicalls-report'),
                                  {'after': '2020-12-01', 'before': '2020-12-25'},
                                  content_type='text/csv',
                                  HTTP_ACCEPT='text/csv')
        assert response['content-disposition'] == ('attachment; filename=billing_report_'
                                                   '2020-12-01_to_2020-12-25.csv')

    def test_tiered_usage(self, api_client, test_set):
        response = api_client.get(reverse('apicalls-tiered-usage'),
                                  {'project': '00000000-0000-0000-0000-000000000000'})
        assert response.status_code == 200
        assert response.json() == {'tier_1': {'image_count': 1,
                                              'video_hours': 1,
                                              'video_minutes': 1,
                                              'video_seconds': 7.17},
                                   'tier_2': {'image_count': 10,
                                              'video_hours': 1,
                                              'video_minutes': 1,
                                              'video_seconds': 46.51}}

    def test_tiered_usage_before_date(self, api_client, test_set):
        response = api_client.get(reverse('apicalls-tiered-usage'),
                                  {'project': '00000000-0000-0000-0000-000000000000',
                                   'before': '2020-12-01'})
        assert response.status_code == 200
        assert response.json() == {'tier_1': {'image_count': 0,
                                              'video_hours': 0.0,
                                              'video_minutes': 0.0,
                                              'video_seconds': 0.0},
                                   'tier_2': {'image_count': 0,
                                              'video_hours': 0.0,
                                              'video_minutes': 0.0,
                                              'video_seconds': 0.0}}


class TestTiers:

    def test_all_tiers_covered(self):
        app = boonsdk.BoonApp(settings.DEV_PIPELINES_KEY, server=settings.DEV_DOMAIN)

        # Get all current modules on Dev
        index = 0
        size = 50
        module_names = []
        response = app.client.post('/api/v1/pipeline-mods/_search', {'page': {'from': index,
                                                                              'size': size}})
        module_names.extend([x['name'] for x in response['list']])
        while len(module_names) < response['page']['totalCount']:
            index += size
            response = app.client.post('/api/v1/pipeline-mods/_search',
                                       {'page': {'from': index, 'size': size}})
            module_names.extend([x['name'] for x in response['list']])
        # Make sure we grabbed them all
        assert len(module_names) == response['page']['totalCount']
        # Make sure they're all accounted for in our tiers
        for module_name in module_names:
            assert (module_name in ApiCall.free_modules or
                    module_name in ApiCall.tier_1_modules or
                    module_name in ApiCall.tier_2_modules)
