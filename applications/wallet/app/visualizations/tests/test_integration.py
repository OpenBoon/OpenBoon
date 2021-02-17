import pytest

from django.urls import reverse
from unittest.mock import patch
from rest_framework import status

from boonsdk import BoonClient
from wallet.tests.utils import check_response
from wallet.utils import convert_json_to_base64
from searches.utils import FieldUtility

pytestmark = pytest.mark.django_db


class TestVisualizationsViewSet:

    def test_list(self, zmlp_project_membership, login, api_client, project):
        response = api_client.get(reverse('visualization-list', kwargs={'project_pk': project.id}))
        check_response(response, status=status.HTTP_418_IM_A_TEAPOT)

    def test_load_empty(self, zmlp_project_membership, login, api_client, project, monkeypatch):
        path = reverse('visualization-load', kwargs={'project_pk': project.id})
        response = api_client.get(path)
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == ['No `visuals` query param included.']

    def test_load_range(self, zmlp_project_membership, login, api_client, project, monkeypatch):

        def mock_response(*args, **kwargs):
            return {'took': 291, 'timed_out': False, '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0}, 'hits': {'total': {'value': 72, 'relation': 'eq'}, 'max_score': None, 'hits': []}, 'aggregations': {'stats#myRange': {'count': 72, 'min': 0.75, 'max': 1.7799999713897705, 'avg': 1.2333333608176973, 'sum': 88.8000019788742}}}  # noqa

        visuals = [{'type': 'range', 'id': 'myRange', 'attribute': 'media.aspect'}]
        path = reverse('visualization-load', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        with patch.object(FieldUtility, 'get_attribute_field_type', return_value='double'):
            response = api_client.get(path, {'visuals': convert_json_to_base64(visuals)})

        content = check_response(response)
        assert content[0]['id'] == 'myRange'
        assert content[0]['defaultFilterType'] == 'range'
        assert content[0]['results']['count'] == 72
        assert content[0]['results']['min'] == 0.75
        assert content[0]['results']['max'] == 1.7799999713897705
        assert content[0]['results']['avg'] == 1.2333333608176973
        assert content[0]['results']['sum'] == 88.8000019788742

    def test_load_facet(self, zmlp_project_membership, login, api_client, project, monkeypatch):

        def mock_response(*args, **kwargs):
            return {'took': 46, 'timed_out': False, '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0}, 'hits': {'total': {'value': 72, 'relation': 'eq'}, 'max_score': None, 'hits': []}, 'aggregations': {'sterms#myFacet': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'landscape', 'doc_count': 54}, {'key': 'portrait', 'doc_count': 12}, {'key': 'square', 'doc_count': 6}]}}}  # noqa

        visuals = [{"type": "facet", "id": "myFacet", "attribute": "media.orientation"}]
        path = reverse('visualization-load', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        with patch.object(FieldUtility, 'get_attribute_field_type', return_value='keyword'):
            response = api_client.get(path, {'visuals': convert_json_to_base64(visuals)})

        content = check_response(response)
        assert content[0]['id'] == 'myFacet'
        assert content[0]['defaultFilterType'] == 'facet'
        assert content[0]['results']['docCountErrorUpperBound'] == 0
        assert content[0]['results']['sumOtherDocCount'] == 0
        assert content[0]['results']['buckets'] == [
            {'key': 'landscape', 'docCount': 54},
            {'key': 'portrait', 'docCount': 12},
            {'key': 'square', 'docCount': 6}
        ]

    def test_load_both(self, zmlp_project_membership, login, api_client, project, monkeypatch):

        def mock_response(*args, **kwargs):
            return {'took': 24, 'timed_out': False, '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0}, 'hits': {'total': {'value': 72, 'relation': 'eq'}, 'max_score': None, 'hits': []}, 'aggregations': {'sterms#myFacet': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'landscape', 'doc_count': 54}, {'key': 'portrait', 'doc_count': 12}, {'key': 'square', 'doc_count': 6}]}, 'stats#myRange': {'count': 72, 'min': 0.75, 'max': 1.7799999713897705, 'avg': 1.2333333608176973, 'sum': 88.8000019788742}}}  # noqa

        visuals = [{'type': 'range', 'id': 'myRange', 'attribute': 'media.aspect'},
                   {"type": "facet", "id": "myFacet", "attribute": "media.orientation"}]
        path = reverse('visualization-load', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        with patch.object(FieldUtility, 'get_attribute_field_type', return_value='keyword'):
            response = api_client.get(path, {'visuals': convert_json_to_base64(visuals)})

        content = check_response(response)
        assert content[0]['id'] == 'myRange'
        assert content[0]['results']['count'] == 72
        assert content[0]['results']['min'] == 0.75
        assert content[0]['results']['max'] == 1.7799999713897705
        assert content[0]['results']['avg'] == 1.2333333608176973
        assert content[0]['results']['sum'] == 88.8000019788742
        assert content[1]['id'] == 'myFacet'
        assert content[1]['results']['docCountErrorUpperBound'] == 0
        assert content[1]['results']['sumOtherDocCount'] == 0
        assert content[1]['results']['buckets'] == [
            {'key': 'landscape', 'docCount': 54},
            {'key': 'portrait', 'docCount': 12},
            {'key': 'square', 'docCount': 6}
        ]

    def test_load_both_with_query(self, zmlp_project_membership, login, api_client,
                                  project, monkeypatch):

        def mock_response(*args, **kwargs):
            return {'took': 13, 'timed_out': False, '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0}, 'hits': {'total': {'value': 55, 'relation': 'eq'}, 'max_score': None, 'hits': []}, 'aggregations': {'sterms#myFacet': {'doc_count_error_upper_bound': 0, 'sum_other_doc_count': 0, 'buckets': [{'key': 'landscape', 'doc_count': 42}, {'key': 'portrait', 'doc_count': 10}, {'key': 'square', 'doc_count': 3}]}, 'stats#myRange': {'count': 55, 'min': 0.75, 'max': 1.7799999713897705, 'avg': 1.2416363900358027, 'sum': 68.29000145196915}}}  # noqa

        visuals = [{'type': 'range', 'id': 'myRange', 'attribute': 'media.aspect'},
                   {"type": "facet", "id": "myFacet", "attribute": "media.orientation"}]
        query = [{'type': 'labelConfidence',
                  'attribute': 'analysis.zvi-face-detection',
                  'values': {'labels': ['face0'], 'min': 0, 'max': 1}}]
        path = reverse('visualization-load', kwargs={'project_pk': project.id})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        with patch.object(FieldUtility, 'get_attribute_field_type', return_value='keyword'):
            response = api_client.get(path, {'visuals': convert_json_to_base64(visuals),
                                             'query': convert_json_to_base64(query)})

        content = check_response(response)
        assert content[0]['id'] == 'myRange'
        assert content[0]['results']['count'] == 55
        assert content[0]['results']['min'] == 0.75
        assert content[0]['results']['max'] == 1.7799999713897705
        assert content[0]['results']['avg'] == 1.2416363900358027
        assert content[0]['results']['sum'] == 68.29000145196915
        assert content[1]['id'] == 'myFacet'
        assert content[1]['results']['docCountErrorUpperBound'] == 0
        assert content[1]['results']['sumOtherDocCount'] == 0
        assert content[1]['results']['buckets'] == [
            {'key': 'landscape', 'docCount': 42},
            {'key': 'portrait', 'docCount': 10},
            {'key': 'square', 'docCount': 3}
        ]

    def test_load_bad_visual(self, zmlp_project_membership, login, api_client, project):
        visuals = [{'type': 'range', 'attribute': 'media.aspect'}]
        path = reverse('visualization-load', kwargs={'project_pk': project.id})
        response = api_client.get(path, {'visuals': convert_json_to_base64(visuals)})

        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content[0] == {'id': 'This value is required.'}
