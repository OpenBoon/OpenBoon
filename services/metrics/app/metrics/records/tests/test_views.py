import pytest

from django.urls import reverse

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
        assert content[0]['image_count'] == 15
        assert content[1]['image_count'] == 1
        assert content[2]['image_count'] == 10
        assert content[3]['image_count'] == 11
        assert content[4]['image_count'] == 1
        assert content[5]['image_count'] == 12
        assert content[0]['video_minutes'] == 73.13
        assert content[1]['video_minutes'] == 7.17
        assert content[2]['video_minutes'] == 48.8
        assert content[3]['video_minutes'] == 45.66
        assert content[4]['video_minutes'] == 2.5
        assert content[5]['video_minutes'] == 52.55

    def test_report_csv(self, api_client, test_set):
        response = api_client.get(reverse('apicalls-report'), content_type='text/csv',
                                  HTTP_ACCEPT='text/csv')
        content = response.rendered_content.decode('utf-8').strip().split('\r\n')
        assert len(content) == 7
        assert content[0] == 'project,service,image_count,video_minutes'
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

    def test_usage_this_month(self, api_client, test_set):
        path = reverse('apicalls-usage-this-month')
        response = api_client.get(path, {'project_id': '00000000-0000-0000-0000-000000000000'})
        assert response.status_code == 200
        assert response.json() == {'image_count': 16,
                                   'project': '00000000-0000-0000-0000-000000000000',
                                   'video_minutes': 80.3}
