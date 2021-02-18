import pytest
# import zmlp
import boonsdk

from django.urls import reverse
from django.conf import settings
from metrics.records.models import ApiCall

pytestmark = pytest.mark.django_db


class TestAPICallsViewSet:

    def test_get_apicalls_empty(self, api_client):
        response = api_client.get(reverse('apicalls-list'))
        assert response.json() == {'count': 0, 'next': None,
                                   'previous': None, 'results': []}

    def test_create_call(self, api_client):
        body = {'project': '92db38cc-a9d1-43af-bfb5-7be32de1d33d',
                'service': 'CoolMlStuff',
                'asset_id': 'GTZ6ppbXYwXO4ssWYcPVaQJsXNC-cVap',
                'asset_path': 'gs://bucket/image.jpg',
                'image_count': 1,
                'video_minutes': 0.0}
        response = api_client.post(reverse('apicalls-list'), body)
        assert response.status_code == 201
        content = response.json()
        assert content['id'] == 1
        assert 'created_date' in content
        assert 'modified_date' in content
        response = api_client.post(reverse('apicalls-list'), body)
        assert response.json() == {'non_field_errors': ['The fields service, asset_id, project must make a unique set.']}  # noqa

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
        assert content['video_minutes'] == single_record.video_minutes
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
                            'video_minutes': 7.17},
                           {'image_count': 10,
                            'project': '00000000-0000-0000-0000-000000000000',
                            'service': 'gcp-label-detection',
                            'tier': 'tier_2',
                            'video_minutes': 46.51},
                           {'image_count': 15,
                            'project': '00000000-0000-0000-0000-000000000000',
                            'service': 'standard',
                            'tier': 'free',
                            'video_minutes': 73.13},
                           {'image_count': 11,
                            'project': '11111111-1111-1111-1111-111111111111',
                            'service': 'boonai-object-detection',
                            'tier': 'tier_1',
                            'video_minutes': 45.66},
                           {'image_count': 10,
                            'project': '11111111-1111-1111-1111-111111111111',
                            'service': 'standard',
                            'tier': 'free',
                            'video_minutes': 48.8},
                           {'image_count': 1,
                            'project': '22222222-2222-2222-2222-222222222222',
                            'service': 'boonai-label-detection',
                            'tier': 'tier_1',
                            'video_minutes': 2.5},
                           {'image_count': 12,
                            'project': '22222222-2222-2222-2222-222222222222',
                            'service': 'boonai-object-detection',
                            'tier': 'tier_1',
                            'video_minutes': 52.55}]

    def test_report_csv(self, api_client, test_set):
        response = api_client.get(reverse('apicalls-report'), content_type='text/csv',
                                  HTTP_ACCEPT='text/csv')
        content = response.rendered_content.decode('utf-8').strip().split('\r\n')
        assert len(content) == 8
        assert content[0] == 'project,service,tier,image_count,video_minutes'
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
                                              'video_minutes': 7.17},
                                   'tier_2': {'image_count': 10,
                                              'video_minutes': 46.51}}

    def test_tiered_usage_before_date(self, api_client, test_set):
        response = api_client.get(reverse('apicalls-tiered-usage'),
                                  {'project': '00000000-0000-0000-0000-000000000000',
                                   'before': '2020-12-01'})
        assert response.status_code == 200
        assert response.json() == {'tier_1': {'image_count': 0,
                                              'video_minutes': 0.0},
                                   'tier_2': {'image_count': 0,
                                              'video_minutes': 0.0}}


class TestTiers:

    def test_all_tiers_covered(self):
        app = boonsdk.BoonApp(settings.DEV_PIPELINES_KEY, server=settings.DEV_DOMAIN)
        # app = zmlp.ZmlpApp(settings.DEV_PIPELINES_KEY, server=settings.DEV_DOMAIN)
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
