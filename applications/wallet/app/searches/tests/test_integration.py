import json
import base64
import pytest

from django.urls import reverse
from rest_framework import status

from searches.models import Search
from searches.filters import BaseFilter
from zmlp import ZmlpClient
from wallet.tests.utils import check_response
from wallet.utils import convert_base64_to_json, convert_json_to_base64

pytestmark = pytest.mark.django_db


@pytest.fixture()
def query():
    return {
        'query': {
            "prefix": {
                "files.name": {
                    "value": "image"
                }
            }
        }
    }


@pytest.fixture()
def search(project, user, query):
    return Search.objects.create(project=project, name='Test Search', search=query,
                                 created_by=user)


class TestSearchViewSetList:

    def test_empty_list(self, zmlp_project_membership, login, api_client, project):
        response = api_client.get(reverse('search-list', kwargs={'project_pk': str(project.id)}))
        content = check_response(response)
        assert content['results'] == []

    def test_list(self, zmlp_project_membership, login, api_client, project, search):
        response = api_client.get(reverse('search-list', kwargs={'project_pk': str(project.id)}))
        results = check_response(response)['results']
        assert len(results) == 1
        assert results[0]['name'] == 'Test Search'
        assert results[0]['project'].endswith(f'{project.id}/')
        assert results[0]['search']['query']['prefix']['files.name']['value'] == 'image'
        assert results[0]['createdBy'].endswith(f'{zmlp_project_membership.user.id}/')

    def test_list_filters_by_project(self, zmlp_project_membership, login, api_client, project,
                                     project2, search, query):
        Search.objects.create(project=project2, name='Other Project Search', search=query,
                              created_by=zmlp_project_membership.user)
        response = api_client.get(reverse('search-list', kwargs={'project_pk': str(project.id)}))
        results = check_response(response)['results']
        assert len(results) == 1
        assert results[0]['name'] == 'Test Search'
        assert results[0]['project'].endswith(f'{project.id}/')


class TestSearchViewSetRetrieve:

    def test_retrieve(self, zmlp_project_membership, login, api_client, project, search):
        response = api_client.get(reverse('search-detail', kwargs={'project_pk': str(project.id),
                                                                   'pk': search.id}))
        result = check_response(response)
        assert result['name'] == 'Test Search'
        assert result['project'].endswith(f'{project.id}/')
        assert result['search']['query']['prefix']['files.name']['value'] == 'image'
        assert result['createdBy'].endswith(f'{zmlp_project_membership.user.id}/')


class TestSearchViewSetCreate:

    def assert_created(self, content, project, query, user):
        """Helper to check the standard create worked for this test set."""
        assert content['project'].endswith(f'{project.id}/')
        assert content['name'] == 'Tester'
        assert content['search'] == query
        assert content['createdDate']
        assert content['modifiedDate']
        assert content['createdBy'].endswith(f'{user.id}/')

    def test_create(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "project": str(project.id),
            "name": "Tester",
            "search": query,
            "created_by": str(user.id)
        }
        response = api_client.post(reverse('search-list', kwargs={'project_pk': str(project.id)}), body)  # noqa
        content = check_response(response, status.HTTP_201_CREATED)
        self.assert_created(content, project, query, user)

    def test_no_project(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "name": "Tester",
            "search": query,
            "created_by": str(user.id)
        }
        response = api_client.post(reverse('search-list', kwargs={'project_pk': str(project.id)}),
                                   body)  # noqa
        content = check_response(response, status.HTTP_201_CREATED)
        self.assert_created(content, project, query, user)

    def test_no_name(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "project": str(project.id),
            "search": query,
            "created_by": str(user.id)
        }
        response = api_client.post(reverse('search-list', kwargs={'project_pk': str(project.id)}),
                                   body)  # noqa
        content = check_response(response, status.HTTP_400_BAD_REQUEST)
        assert content['name'] == ['This field is required.']

    def test_no_search(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "project": str(project.id),
            "name": "Tester",
            "created_by": str(user.id)
        }
        response = api_client.post(reverse('search-list', kwargs={'project_pk': str(project.id)}),
                                   body)  # noqa
        content = check_response(response, status.HTTP_400_BAD_REQUEST)
        assert content['search'] == ['This field is required.']

    def test_no_created_by(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "project": str(project.id),
            "name": "Tester",
            "search": query,
        }
        response = api_client.post(reverse('search-list', kwargs={'project_pk': str(project.id)}),
                                   body)  # noqa
        content = check_response(response, status.HTTP_201_CREATED)
        self.assert_created(content, project, query, user)


class TestSearchViewSetUpdate:

    def test_update(self, zmlp_project_membership, login, api_client, query, project, user):
        search = Search.objects.create(project=project, name='My Old Search',
                                       search=query, created_by=user)
        response = api_client.get(reverse('search-list', kwargs={'project_pk': str(project.id)}))
        results = check_response(response)['results']
        assert len(results) == 1
        assert results[0]['name'] == 'My Old Search'

        new_query = {"hey": "you guys"}
        body = {
            "project": str(project.id),
            "name": "Tester",
            "search": new_query,
            "created_by": str(user.id)
        }
        response = api_client.put(reverse('search-detail', kwargs={'project_pk': str(project.id),
                                                                   'pk': str(search.id)}), body)
        content = check_response(response, status.HTTP_200_OK)
        assert content['id'] == str(search.id)
        assert content['name'] == 'Tester'
        assert content['search'] == new_query

    def test_partial_with_bad_method(self, zmlp_project_membership, login, api_client, query,
                                     project, user):
        search = Search.objects.create(project=project, name='My Old Search',
                                       search=query, created_by=user)

        new_query = {"hey": "you guys"}
        body = {
            "search": new_query,
        }
        response = api_client.put(reverse('search-detail', kwargs={'project_pk': str(project.id),
                                                                   'pk': str(search.id)}), body)
        content = check_response(response, status.HTTP_400_BAD_REQUEST)
        for field in ('project', 'name', 'createdBy'):
            assert content[field] == ['This field is required.']

    def test_partial_update(self, zmlp_project_membership, login, api_client, query, project, user):
        search = Search.objects.create(project=project, name='My Old Search',
                                       search=query, created_by=user)
        response = api_client.get(reverse('search-list', kwargs={'project_pk': str(project.id)}))
        results = check_response(response)['results']
        assert len(results) == 1
        assert results[0]['name'] == 'My Old Search'

        new_query = {"hey": "you guys"}
        body = {
            "search": new_query,
        }
        response = api_client.patch(reverse('search-detail', kwargs={'project_pk': str(project.id),
                                                                     'pk': str(search.id)}), body)
        content = check_response(response, status.HTTP_200_OK)
        assert content['id'] == str(search.id)
        assert content['name'] == 'My Old Search'
        assert content['search'] == new_query


class TestSearchDestroy:

    def test_destroy(self, zmlp_project_membership, login, api_client, project, search):
        response = api_client.delete(reverse('search-detail', kwargs={'project_pk': str(project.id),
                                                                      'pk': str(search.id)}))
        assert response.status_code == status.HTTP_204_NO_CONTENT

    def test_destroy_non_member(self, zmlp_project_membership, login, api_client, project2, query):
        search2 = Search.objects.create(project=project2, name='Other Project Search', search=query,
                                        created_by=zmlp_project_membership.user)
        response = api_client.delete(reverse('search-detail', kwargs={'project_pk': str(project2.id),  # noqa
                                                                      'pk': str(search2.id)}))
        assert response.status_code == status.HTTP_403_FORBIDDEN


class TestFieldsAction:

    @pytest.fixture
    def mapping_response(self):
        return {'u1oi7jlkrqcxxvkc': {'aliases': {}, 'mappings': {'dynamic': 'strict', 'dynamic_templates': [{'analysis_label': {'path_match': 'analysis.*.predictions.label', 'match_mapping_type': 'string', 'mapping': {'fields': {'fulltext': {'analyzer': 'default', 'type': 'text'}}, 'type': 'keyword'}}}, {'analysis_score': {'path_match': 'analysis.*.predictions.score', 'mapping': {'coerce': True, 'type': 'float'}}}, {'analysis_point': {'path_match': 'analysis.*.predictions.point', 'mapping': {'type': 'geo_point'}}}, {'analysis_bbox': {'path_match': 'analysis.*.predictions.bbox', 'mapping': {'type': 'float'}}}, {'analysis_tags': {'path_match': 'analysis.*.predictions.tags', 'match_mapping_type': 'string', 'mapping': {'fields': {'fulltext': {'analyzer': 'default', 'type': 'text'}}, 'type': 'keyword'}}}, {'simhash': {'match': 'simhash', 'match_mapping_type': 'string', 'match_pattern': 'regex', 'mapping': {'index': False, 'type': 'keyword'}}}, {'content': {'match': 'content', 'match_mapping_type': 'string', 'mapping': {'analyzer': 'default', 'type': 'text'}}}], 'properties': {'analysis': {'dynamic': 'true', 'properties': {'zvi': {'properties': {'tinyProxy': {'type': 'text', 'fields': {'keyword': {'type': 'keyword', 'ignore_above': 256}}}}}, 'zvi-image-similarity': {'properties': {'simhash': {'type': 'keyword', 'index': False}, 'type': {'type': 'text', 'fields': {'keyword': {'type': 'keyword', 'ignore_above': 256}}}}}}}, 'aux': {'type': 'object', 'enabled': False}, 'clip': {'dynamic': 'strict', 'properties': {'length': {'type': 'double'}, 'pile': {'type': 'keyword'}, 'sourceAssetId': {'type': 'keyword'}, 'start': {'type': 'double'}, 'stop': {'type': 'double'}, 'timeline': {'type': 'keyword'}, 'type': {'type': 'keyword'}}}, 'files': {'type': 'object', 'enabled': False}, 'location': {'dynamic': 'strict', 'properties': {'city': {'type': 'keyword'}, 'code': {'type': 'keyword'}, 'country': {'type': 'keyword'}, 'point': {'type': 'geo_point'}}}, 'media': {'dynamic': 'strict', 'properties': {'aspect': {'type': 'float'}, 'author': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'content': {'type': 'text'}, 'description': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'height': {'type': 'float'}, 'keywords': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'length': {'type': 'float'}, 'orientation': {'type': 'keyword'}, 'timeCreated': {'type': 'date'}, 'title': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'type': {'type': 'keyword'}, 'width': {'type': 'float'}}}, 'metrics': {'dynamic': 'strict', 'properties': {'pipeline': {'type': 'nested', 'dynamic': 'strict', 'properties': {'checksum': {'type': 'long'}, 'error': {'type': 'keyword'}, 'executionDate': {'type': 'date'}, 'executionTime': {'type': 'double'}, 'module': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'processor': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}}}}}, 'source': {'dynamic': 'strict', 'properties': {'checksum': {'type': 'long'}, 'extension': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'filename': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'filesize': {'type': 'long'}, 'mimetype': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'path': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}, 'path': {'type': 'text', 'analyzer': 'path_analyzer', 'fielddata': True}}}}}, 'system': {'dynamic': 'strict', 'properties': {'dataSourceId': {'type': 'keyword'}, 'jobId': {'type': 'keyword'}, 'projectId': {'type': 'keyword'}, 'state': {'type': 'keyword'}, 'taskId': {'type': 'keyword'}, 'timeCreated': {'type': 'date'}, 'timeModified': {'type': 'date'}}}, 'tmp': {'type': 'object', 'enabled': False}}}, 'settings': {'index': {'mapping': {'coerce': 'false', 'ignore_malformed': 'false'}, 'number_of_shards': '2', 'provided_name': 'u1oi7jlkrqcxxvkc', 'creation_date': '1586588369756', 'analysis': {'filter': {'delimiter_filter': {'type': 'word_delimiter', 'preserve_original': 'true'}, 'stemmer_english': {'type': 'stemmer', 'language': 'english'}}, 'analyzer': {'default': {'filter': ['trim', 'stop', 'lowercase', 'delimiter_filter', 'stemmer_english'], 'tokenizer': 'standard'}, 'path_analyzer': {'type': 'custom', 'tokenizer': 'path_tokenizer'}}, 'tokenizer': {'path_tokenizer': {'type': 'path_hierarchy', 'delimiter': '/'}}}, 'number_of_replicas': '0', 'uuid': 'Y1vGPS26QWGiDJNhtV8FLQ', 'version': {'created': '7050199'}}}}}  # noqa

    def test_get(self, login, api_client, monkeypatch, mapping_response, project):
        def mock_response(*args, **kwargs):
            return mapping_response

        monkeypatch.setattr(ZmlpClient, 'get', mock_response)
        response = api_client.get(reverse('search-fields', kwargs={'project_pk': project.id}))
        content = check_response(response)
        assert content['analysis']['zvi']['tinyProxy'] == ['facet', 'text', 'exists']
        assert content['clip']['start'] == ['range', 'exists']
        assert content['media']['type'] == ['facet', 'text', 'exists']
        assert content['aux'] == ['exists']
        assert content['tmp'] == ['exists']

    def test_failed_get(self, login, api_client, monkeypatch, project):
        def mock_response(*args, **kwargs):
            return {'34tdfgsdfgddas': {}, '23rgfdfg34f3df': {}}

        monkeypatch.setattr(ZmlpClient, 'get', mock_response)
        response = api_client.get(reverse('search-fields', kwargs={'project_pk': project.id}))
        content = check_response(response, status.HTTP_500_INTERNAL_SERVER_ERROR)
        assert content == {'detail': 'ZMLP did not return field mappings as expected.'}


class BaseFiltersTestCase(object):

    @pytest.fixture
    def range_load(self):
        return {
            'type': 'range',
            'attribute': 'source.filesize',
            'exists': True
        }

    @pytest.fixture
    def range_load_qs(self, range_load):
        return convert_json_to_base64(range_load)


class TestQueryFilters(BaseFiltersTestCase):

    def test_get(self, login, api_client, project):
        response = api_client.get(reverse('search-query', kwargs={'project_pk': project.id}))
        content = check_response(response, status=status.HTTP_501_NOT_IMPLEMENTED)


class TestLoadFilter(BaseFiltersTestCase):

    def test_get(self, login, api_client, project, range_load_qs, monkeypatch):
        def mock_response(*args, **kwargs):
            return {'took': 34,
                    'timed_out': False,
                    '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0},
                    'hits': {'total': {'value': 24, 'relation': 'eq'},
                             'max_score': None,
                             'hits': []},
                    'aggregations': {'stats#0d2a26ea-1006-4e75-8626-b81809a7a021': {'count': 24,
                                                                                    'min': 7555.0,
                                                                                    'max': 64657027.0,
                                                                                    'avg': 5725264.875,
                                                                                    'sum': 137406357.0}}}

        def mock_init(*args, **kwargs):
            # Need to override the internally created name so we can parse our fake response
            (self, data) = (args[0], args[1])
            self.data = data
            self.name = '0d2a26ea-1006-4e75-8626-b81809a7a021'

        monkeypatch.setattr(ZmlpClient, 'post', mock_response)
        monkeypatch.setattr(BaseFilter, '__init__', mock_init)
        response = api_client.get(reverse('search-aggregate', kwargs={'project_pk': project.id}),
                                  {'filter': range_load_qs})
        content = check_response(response, status=status.HTTP_200_OK)
        assert content['count'] == 24
        assert content['results']['min'] == 7555.0
        assert content['results']['max'] == 64657027.0

    def test_get_missing_querystring(self, login, api_client, project, range_load_qs):
        response = api_client.get(reverse('search-aggregate', kwargs={'project_pk': project.id}))
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == 'No `filter` querystring included.'

    def test_get_bad_querystring_encoding(self, login, api_client, project, range_load_qs):
        range_load_qs = 'thisisnolongerencodedright' + range_load_qs.decode('utf-8')
        response = api_client.get(reverse('search-aggregate', kwargs={'project_pk': project.id}),
                                  {'filter': range_load_qs})
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == 'Unable to decode `filter` querystring.'

    def test_get_missing_filter_type(self, login, api_client, project, range_load):
        del(range_load['type'])
        encoded_filter = convert_json_to_base64(range_load)
        response = api_client.get(reverse('search-aggregate', kwargs={'project_pk': project.id}),
                                  {'filter': encoded_filter})
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == 'Filter description is missing a `type`.'

    def test_get_missing_filter_type(self, login, api_client, project, range_load):
        range_load['type'] = 'fake_type'
        encoded_filter = convert_json_to_base64(range_load)
        response = api_client.get(reverse('search-aggregate', kwargs={'project_pk': project.id}),
                                  {'filter': encoded_filter})
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == 'Unsupported filter `fake_type` given.'