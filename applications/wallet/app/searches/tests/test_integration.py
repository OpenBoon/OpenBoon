from unittest.mock import patch, Mock

import pytest
from django.urls import reverse
from rest_framework import status
from rest_framework.response import Response
from zmlp import ZmlpClient

from searches.filters import BaseFilter
from searches.models import Search
from searches.views import MetadataExportViewSet, SearchViewSet, search_asset_modifier
from wallet.tests.utils import check_response
from wallet.utils import convert_json_to_base64

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
                                 createdBy=user)


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
        assert results[0]['project'] == project.id
        assert results[0]['search']['query']['prefix']['files.name']['value'] == 'image'
        assert results[0]['createdBy'] == zmlp_project_membership.user.id

    def test_list_filters_by_project(self, zmlp_project_membership, login, api_client, project,
                                     project2, search, query):
        Search.objects.create(project=project2, name='Other Project Search', search=query,
                              createdBy=zmlp_project_membership.user)
        response = api_client.get(reverse('search-list', kwargs={'project_pk': str(project.id)}))
        results = check_response(response)['results']
        assert len(results) == 1
        assert results[0]['name'] == 'Test Search'
        assert results[0]['project'] == project.id


class TestSearchViewSetRetrieve:

    def test_retrieve(self, zmlp_project_membership, login, api_client, project, search):
        response = api_client.get(reverse('search-detail', kwargs={'project_pk': str(project.id),
                                                                   'pk': search.id}))
        result = check_response(response)
        assert result['name'] == 'Test Search'
        assert result['project'] == project.id
        assert result['search']['query']['prefix']['files.name']['value'] == 'image'
        assert result['createdBy'] == zmlp_project_membership.user.id


class TestSearchViewSetCreate:

    def assert_created(self, content, project, query, user):
        """Helper to check the standard create worked for this test set."""
        assert content['project'] == project.id
        assert content['name'] == 'Tester'
        assert content['search'] == query
        assert content['createdDate']
        assert content['modifiedDate']
        assert content['createdBy'] == user.id

    def test_create(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "project": str(project.id),
            "name": "Tester",
            "search": query,
            "createdBy": str(user.id)
        }
        response = api_client.post(reverse('search-list', kwargs={'project_pk': str(project.id)}), body)  # noqa
        content = check_response(response, status.HTTP_201_CREATED)
        self.assert_created(content, project, query, user)

    def test_no_project(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "name": "Tester",
            "search": query,
            "createdBy": str(user.id)
        }
        response = api_client.post(reverse('search-list', kwargs={'project_pk': str(project.id)}),
                                   body)  # noqa
        content = check_response(response, status.HTTP_201_CREATED)
        self.assert_created(content, project, query, user)

    def test_no_name(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "project": str(project.id),
            "search": query,
            "createdBy": str(user.id)
        }
        response = api_client.post(reverse('search-list', kwargs={'project_pk': str(project.id)}),
                                   body)
        content = check_response(response, status.HTTP_400_BAD_REQUEST)
        assert content['name'] == ['This field is required.']

    def test_no_search(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "project": str(project.id),
            "name": "Tester",
            "createdBy": str(user.id)
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
                                       search=query, createdBy=user)
        response = api_client.get(reverse('search-list', kwargs={'project_pk': str(project.id)}))
        results = check_response(response)['results']
        assert len(results) == 1
        assert results[0]['name'] == 'My Old Search'

        new_query = {"hey": "you guys"}
        body = {
            "project": str(project.id),
            "name": "Tester",
            "search": new_query,
            "createdBy": str(user.id)
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
                                       search=query, createdBy=user)

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
                                       search=query, createdBy=user)
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
                                        createdBy=zmlp_project_membership.user)
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
        assert content['analysis']['zvi']['tinyProxy'] == ['exists']
        assert content['clip']['start'] == ['range', 'exists']
        assert content['media']['type'] == ['facet', 'exists']
        assert content['aux'] == ['exists']
        assert content['tmp'] == ['exists']

    def test_get_field_with_underscore(self, login, api_client, monkeypatch, mapping_response,
                                       project):
        def mock_response(*args, **kwargs):
            return {'u1oi7jlkrqcxxvkc': {'aliases': {}, 'mappings': {'dynamic': 'strict', 'dynamic_templates': [{'analysis_label': {'path_match': 'analysis.*.predictions.label', 'match_mapping_type': 'string', 'mapping': {'fields': {'fulltext': {'analyzer': 'default', 'type': 'text'}}, 'type': 'keyword'}}}, {'analysis_score': {'path_match': 'analysis.*.predictions.score', 'mapping': {'coerce': True, 'type': 'float'}}}, {'analysis_point': {'path_match': 'analysis.*.predictions.point', 'mapping': {'type': 'geo_point'}}}, {'analysis_bbox': {'path_match': 'analysis.*.predictions.bbox', 'mapping': {'type': 'float'}}}, {'analysis_tags': {'path_match': 'analysis.*.predictions.tags', 'match_mapping_type': 'string', 'mapping': {'fields': {'fulltext': {'analyzer': 'default', 'type': 'text'}}, 'type': 'keyword'}}}, {'simhash': {'match': 'simhash', 'match_mapping_type': 'string', 'match_pattern': 'regex', 'mapping': {'index': False, 'type': 'keyword'}}}, {'content': {'match': 'content', 'match_mapping_type': 'string', 'mapping': {'analyzer': 'default', 'type': 'text'}}}], 'properties': {'analysis': {'dynamic': 'true', 'properties': {'analyis_underscore': {'properties': {'tinyProxy': {'type': 'text', 'fields': {'keyword': {'type': 'keyword', 'ignore_above': 256}}}}}, 'zvi-image-similarity': {'properties': {'simhash': {'type': 'keyword', 'index': False}, 'type': {'type': 'text', 'fields': {'keyword': {'type': 'keyword', 'ignore_above': 256}}}}}}}, 'aux': {'type': 'object', 'enabled': False}, 'clip': {'dynamic': 'strict', 'properties': {'length': {'type': 'double'}, 'pile': {'type': 'keyword'}, 'sourceAssetId': {'type': 'keyword'}, 'start': {'type': 'double'}, 'stop': {'type': 'double'}, 'timeline': {'type': 'keyword'}, 'type': {'type': 'keyword'}}}, 'files': {'type': 'object', 'enabled': False}, 'location': {'dynamic': 'strict', 'properties': {'city': {'type': 'keyword'}, 'code': {'type': 'keyword'}, 'country': {'type': 'keyword'}, 'point': {'type': 'geo_point'}}}, 'media': {'dynamic': 'strict', 'properties': {'aspect': {'type': 'float'}, 'author': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'content': {'type': 'text'}, 'description': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'height': {'type': 'float'}, 'keywords': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'length': {'type': 'float'}, 'orientation': {'type': 'keyword'}, 'timeCreated': {'type': 'date'}, 'title': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'type': {'type': 'keyword'}, 'width': {'type': 'float'}}}, 'metrics': {'dynamic': 'strict', 'properties': {'pipeline': {'type': 'nested', 'dynamic': 'strict', 'properties': {'checksum': {'type': 'long'}, 'error': {'type': 'keyword'}, 'executionDate': {'type': 'date'}, 'executionTime': {'type': 'double'}, 'module': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'processor': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}}}}}, 'source': {'dynamic': 'strict', 'properties': {'checksum': {'type': 'long'}, 'extension': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'filename': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'filesize': {'type': 'long'}, 'mimetype': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}}}, 'path': {'type': 'keyword', 'fields': {'fulltext': {'type': 'text'}, 'path': {'type': 'text', 'analyzer': 'path_analyzer', 'fielddata': True}}}}}, 'system': {'dynamic': 'strict', 'properties': {'dataSourceId': {'type': 'keyword'}, 'jobId': {'type': 'keyword'}, 'projectId': {'type': 'keyword'}, 'state': {'type': 'keyword'}, 'taskId': {'type': 'keyword'}, 'timeCreated': {'type': 'date'}, 'timeModified': {'type': 'date'}}}, 'tmp': {'type': 'object', 'enabled': False}}}, 'settings': {'index': {'mapping': {'coerce': 'false', 'ignore_malformed': 'false'}, 'number_of_shards': '2', 'provided_name': 'u1oi7jlkrqcxxvkc', 'creation_date': '1586588369756', 'analysis': {'filter': {'delimiter_filter': {'type': 'word_delimiter', 'preserve_original': 'true'}, 'stemmer_english': {'type': 'stemmer', 'language': 'english'}}, 'analyzer': {'default': {'filter': ['trim', 'stop', 'lowercase', 'delimiter_filter', 'stemmer_english'], 'tokenizer': 'standard'}, 'path_analyzer': {'type': 'custom', 'tokenizer': 'path_tokenizer'}}, 'tokenizer': {'path_tokenizer': {'type': 'path_hierarchy', 'delimiter': '/'}}}, 'number_of_replicas': '0', 'uuid': 'Y1vGPS26QWGiDJNhtV8FLQ', 'version': {'created': '7050199'}}}}}  # noqa

        monkeypatch.setattr(ZmlpClient, 'get', mock_response)
        response = api_client.get(reverse('search-fields', kwargs={'project_pk': project.id}))
        content = check_response(response)
        assert content['analysis']['analyis_underscore']['tinyProxy'] == ['exists']
        assert content['clip']['start'] == ['range', 'exists']
        assert content['media']['type'] == ['facet', 'exists']
        assert content['aux'] == ['exists']
        assert content['tmp'] == ['exists']

    def test_failed_get(self, login, api_client, monkeypatch, project):
        def mock_response(*args, **kwargs):
            return {'34tdfgsdfgddas': {}, '23rgfdfg34f3df': {}}

        monkeypatch.setattr(ZmlpClient, 'get', mock_response)
        response = api_client.get(reverse('search-fields', kwargs={'project_pk': project.id}))
        content = check_response(response, status.HTTP_500_INTERNAL_SERVER_ERROR)
        assert content == {'detail': ['ZMLP did not return field mappings as expected.']}


class BaseFiltersTestCase(object):

    @pytest.fixture
    def range_agg(self):
        return {
            'type': 'range',
            'attribute': 'source.filesize',
            'exists': True
        }

    @pytest.fixture
    def range_agg_qs(self, range_agg):
        return convert_json_to_base64(range_agg)

    @pytest.fixture
    def facet_query(self):
        return {
            'type': 'facet',
            'attribute': 'source.extension',
            'values': {
                'facets': ['tiff']
            }
        }

    @pytest.fixture
    def facet_query_qs(self, facet_query):
        return convert_json_to_base64([facet_query])


class TestSearchAssetModifier:

    @pytest.fixture
    def mock_request(self, api_factory, project):

        def uri(url):
            return f'http://testserver{url}'

        mock = Mock(build_absolute_uri=uri)
        mock.parser_context = {'view': Mock(kwargs={'project_pk': 'asdf'})}

        return mock

    @pytest.fixture
    def image_item(self):
        return {'_index': 'x6vghtkcc9t4bi06', '_type': '_doc', '_id': 'lUfjW2IAunzEx-3Rn8vMYyhlkNZGsdtI', '_score': None, '_source': {'files': [{'size': 582248, 'name': 'image_1024x640.jpg', 'mimetype': 'image/jpeg', 'id': 'assets/lUfjW2IAunzEx-3Rn8vMYyhlkNZGsdtI/proxy/image_1024x640.jpg', 'category': 'proxy', 'attrs': {'width': 1024, 'height': 640}}, {'size': 154814, 'name': 'image_512x320.jpg', 'mimetype': 'image/jpeg', 'id': 'assets/lUfjW2IAunzEx-3Rn8vMYyhlkNZGsdtI/proxy/image_512x320.jpg', 'category': 'proxy', 'attrs': {'width': 512, 'height': 320}}, {'size': 162368, 'name': 'web-proxy.jpg', 'mimetype': 'image/jpeg', 'id': 'assets/lUfjW2IAunzEx-3Rn8vMYyhlkNZGsdtI/web-proxy/web-proxy.jpg', 'category': 'web-proxy', 'attrs': {'width': 1024, 'height': 640}}], 'source': {'path': 'gs://zmlp-private-test-data/zorroa-deploy-testdata/zorroa-dev-testdata/zorroa-test-data/pictures/mountains_239.jpg', 'extension': 'jpg', 'filename': 'mountains_239.jpg', 'checksum': 183104247, 'mimetype': 'image/jpeg', 'filesize': 975478}, 'media': {'orientation': 'landscape', 'aspect': 1.6, 'width': 2560, 'length': 1, 'type': 'image', 'height': 1600}}, 'sort': [1589841573228]}  # noqa

    @pytest.fixture
    def document_item(self):
        return {'_index': 'x6vghtkcc9t4bi06', '_type': '_doc', '_id': 'FaFSqlDesCECU8FFj3VlnO64d9u44Vl_', '_score': None, '_source': {'files': [{'size': 430701, 'name': 'image_786x1024.jpg', 'mimetype': 'image/jpeg', 'id': 'assets/FaFSqlDesCECU8FFj3VlnO64d9u44Vl_/proxy/image_786x1024.jpg', 'category': 'proxy', 'attrs': {'width': 786, 'height': 1024}}, {'size': 126192, 'name': 'image_393x512.jpg', 'mimetype': 'image/jpeg', 'id': 'assets/FaFSqlDesCECU8FFj3VlnO64d9u44Vl_/proxy/image_393x512.jpg', 'category': 'proxy', 'attrs': {'width': 393, 'height': 512}}, {'size': 164668, 'name': 'web-proxy.jpg', 'mimetype': 'image/jpeg', 'id': 'assets/FaFSqlDesCECU8FFj3VlnO64d9u44Vl_/web-proxy/web-proxy.jpg', 'category': 'web-proxy', 'attrs': {'width': 786, 'height': 1024}}], 'source': {'path': 'gs://zmlp-private-test-data/zorroa-deploy-testdata/zorroa-dev-testdata/zorroa-test-data/pdf_test/195213_P4.pdf', 'extension': 'pdf', 'filename': '195213_P4.pdf', 'checksum': 46422929, 'mimetype': 'application/pdf', 'filesize': 1662878}, 'media': {'orientation': 'portrait', 'length': 27, 'width': 611.04, 'type': 'document', 'content': " BCANNEDUNDERGROUND ENGINEERING & ENVIRONMENTAL SOLUTIONS Haley & Aldrich, Inc. 465 Medford Street Suite 2200 Boston, MA 02129-1400 Tel: 617.886.7400 Fax: 617.886.7600 www.HaleyAldrich.com MEMORANDUM 25 July 2001 File No. 12515-220 TO: Massachusetts Department of Environmental Protection - Attn: Hector Laguette, Stephen Johnson C: Wellesley College; Attn: Barry Monahan Wellesley College: Attn: Bonnie Weeks V Richard White & Sons; Attn: Kevin Hines Ropes & Gray; Attn: Peter Alpert FROM: Haley & Aldrich, Inc. Debo Gevalt/R UThSuck/Se a hrroll SUBJECT: Results of the May 01 Groundwater Monitoring Event Former Henry Woods Paint Factory OmFICEs Wellesley, Massachusetts Cleveland Ohio In accordance with our 11 August 2000 memorandum, we continue to maintain a quarterly Dayton groundwater monitoring program at the Uplands portion of the former Henry Woods Paint Ohio Factory Site. Denver Colorado Historical groundwater analytical data for the former Henry Woods Paint Factory (FHWPF) Site (the Site) indicate the presence of dissolved hexavalent chromium (CrVI) in monitoring Detroit Michigan wells and surface water at the Site. The groundwater monitoring program was developed for the Site to collect data that will provide additional information regarding groundwater Hartford Connecticut conditions at the Site and assist in evaluating potential remedial solutions, if required. This memorandum presents the results of the May 2001 monitoring event. The activities Los Angeles conducted during the event are listed below. California Manchester 1. Groundwater level measurements. Groundwater level measurements were made to New Hampshire provide the data required to determine groundwater elevation and flow direction Newark beneath the Site. This data will be evaluated together with data from other quarterly New Jersey events to assess the range of seasonal variation in groundwater flow paths, which will Portland be considered for the proper design of a groundwater treatment system, if necessary. Maine A better understanding of groundwater flow paths may also provide additional insight Rochester into contaminant fate and transport at the Site. New York San Diego 2. Groundwater sampling and analysis. Groundwater samples were collected from 93 California Site monitoring wells and piezometers at the Site during the May 2001 monitoring Tucson Arizona Washington District of Columbia P,'nh4on r e .", 'height': 795.12}}, 'sort': [1589843442885]}  # noqa

    @pytest.fixture
    def video_item(self):
        return {'_index': 'x6vghtkcc9t4bi06', '_type': '_doc', '_id': 'mUqByg6ARFdORH1UaO2NH4JvxfN2Wk7W', '_score': None, '_source': {'files': [{'size': 109909, 'name': 'image_640x360.jpg', 'mimetype': 'image/jpeg', 'id': 'assets/mUqByg6ARFdORH1UaO2NH4JvxfN2Wk7W/proxy/image_640x360.jpg', 'category': 'proxy', 'attrs': {'time_offset': 7.52, 'width': 640, 'height': 360}}, {'size': 120644, 'name': 'image_512x288.jpg', 'mimetype': 'image/jpeg', 'id': 'assets/mUqByg6ARFdORH1UaO2NH4JvxfN2Wk7W/proxy/image_512x288.jpg', 'category': 'proxy', 'attrs': {'time_offset': 7.52, 'width': 512, 'height': 288}}, {'size': 39152, 'name': 'web-proxy.jpg', 'mimetype': 'image/jpeg', 'id': 'assets/mUqByg6ARFdORH1UaO2NH4JvxfN2Wk7W/web-proxy/web-proxy.jpg', 'category': 'web-proxy', 'attrs': {'width': 640, 'height': 360}}, {'size': 6800212, 'name': 'video_640x360.mp4', 'mimetype': 'video/mp4', 'id': 'assets/mUqByg6ARFdORH1UaO2NH4JvxfN2Wk7W/proxy/video_640x360.mp4', 'category': 'proxy', 'attrs': {'frameRate': 29.97, 'frames': 452, 'width': 640, 'height': 360}}], 'source': {'path': 'gs://zmlp-private-test-data/zorroa-deploy-testdata/zorroa-dev-testdata/zorroa-test-data/video/sample_ipad.m4v', 'extension': 'm4v', 'filename': 'sample_ipad.m4v', 'checksum': 2883801597, 'mimetype': 'video/x-m4v', 'filesize': 6192402}, 'media': {'orientation': 'landscape', 'aspect': 1.78, 'width': 640, 'length': 15.048367, 'description': 'A short description of luke sledding in winter.', 'timeCreated': '2016-04-08T15:02:31.000000Z', 'title': 'Luke crashes sled', 'type': 'video', 'height': 360}}, 'sort': [1589843375190]}  # noqa

    def test_add_asset_style_image(self, mock_request, image_item):
        search_asset_modifier(mock_request, image_item)
        assert image_item['assetStyle'] == 'image'

    def test_add_asset_style_document(self, mock_request, document_item):
        search_asset_modifier(mock_request, document_item)
        # Documents should have the image style
        assert document_item['assetStyle'] == 'image'

    def test_add_asset_style_video(self, mock_request, video_item):
        search_asset_modifier(mock_request, video_item)
        assert video_item['assetStyle'] == 'video'

    def test_add_video_length_for_image(self, mock_request, image_item):
        search_asset_modifier(mock_request, image_item)
        assert image_item['videoLength'] is None

    def test_add_video_length_for_video(self, mock_request, video_item):
        search_asset_modifier(mock_request, video_item)
        assert video_item['videoLength'] == 15.048367

    def test_videoUrl_for_image(self, mock_request, image_item):
        search_asset_modifier(mock_request, image_item)
        assert image_item['videoProxyUrl'] is None

    def test_videoUrl_for_video(self, mock_request, video_item):
        search_asset_modifier(mock_request, video_item)
        assert video_item['videoProxyUrl'] == 'http://testserver/api/v1/projects/asdf/assets/mUqByg6ARFdORH1UaO2NH4JvxfN2Wk7W/files/category/proxy/name/video_640x360.mp4/'  # noqa

    def test_fullscreen_url_not_included(self, mock_request, video_item):
        search_asset_modifier(mock_request, video_item)
        assert 'fullscreenUrl' not in video_item


class TestQuery(BaseFiltersTestCase):

    @pytest.fixture
    def mock_response(self):
        return {"took":6,"timed_out":False,"_shards":{"total":2,"successful":2,"skipped":0,"failed":0},"hits":{"total":{"value":2,"relation":"eq"},"max_score":0.0,"hits":[{"_index":"fgctsfya3pdk0oib","_type":"_doc","_id":"_V_suiBEd3QEPBWxMq6yW6SII8cCuP1U","_score":0.0,"_source":{"media":{},"files":[{"size":119497,"name":"image_744x1024.jpg","mimetype":"image/jpeg","id":"assets/_V_suiBEd3QEPBWxMq6yW6SII8cCuP1U/proxy/image_744x1024.jpg","category":"proxy","attrs":{"width":744,"height":1024}},{"size":119497,"name":"web-proxy.jpg","mimetype":"image/jpeg","id":"assets/_V_suiBEd3QEPBWxMq6yW6SII8cCuP1U/web-proxy/web-proxy.jpg","category":"web-proxy","attrs":{"width":744,"height":1024}},{"size":43062,"name":"image_372x512.jpg","mimetype":"image/jpeg","id":"assets/_V_suiBEd3QEPBWxMq6yW6SII8cCuP1U/proxy/image_372x512.jpg","category":"proxy","attrs":{"width":372,"height":512}},{"size":21318,"name":"image_232x320.jpg","mimetype":"image/jpeg","id":"assets/_V_suiBEd3QEPBWxMq6yW6SII8cCuP1U/proxy/image_232x320.jpg","category":"proxy","attrs":{"width":232,"height":320}}],"source":{"path":"gs://zorroa-dev-data/image/singlepage.tiff","extension":"tiff","filename":"singlepage.tiff","checksum":754419346,"mimetype":"image/tiff","filesize":11082}}},{"_index":"fgctsfya3pdk0oib","_type":"_doc","_id":"vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C","_score":0.0,"_source":{"media":{},"files":[{"size":89643,"name":"image_650x434.jpg","mimetype":"image/jpeg","id":"assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/proxy/image_650x434.jpg","category":"proxy","attrs":{"width":650,"height":434}},{"size":60713,"name":"image_512x341.jpg","mimetype":"image/jpeg","id":"assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/proxy/image_512x341.jpg","category":"proxy","attrs":{"width":512,"height":341}},{"size":30882,"name":"image_320x213.jpg","mimetype":"image/jpeg","id":"assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/proxy/image_320x213.jpg","category":"proxy","attrs":{"width":320,"height":213}}],"source":{"path":"gs://zorroa-dev-data/image/TIFF_1MB.tiff","extension":"tiff","filename":"TIFF_1MB.tiff","checksum":1867533868,"mimetype":"image/tiff","filesize":1131930}}}]}}  # noqa

    def test_get(self, login, api_client, project, monkeypatch, facet_query_qs, mock_response):
        def _response(*args, **kwargs):
            return mock_response

        monkeypatch.setattr(ZmlpClient, 'post', _response)
        response = api_client.get(reverse('search-query', kwargs={'project_pk': project.id}),
                                  {'query': facet_query_qs})

        content = check_response(response, status=status.HTTP_200_OK)
        assert content['count'] == 2
        assert 'next' in content
        assert 'previous' in content
        # Should only be the requested fields on this request
        assert list(content['results'][0]['metadata']) == ['source']

    def test_get_md_missing_media(self, login, api_client, project, monkeypatch, facet_query_qs):
        def _response(*args, **kwargs):
            return {"took":6,"timed_out":False,"_shards":{"total":2,"successful":2,"skipped":0,"failed":0},"hits":{"total":{"value":2,"relation":"eq"},"max_score":0.0,"hits":[{"_index":"fgctsfya3pdk0oib","_type":"_doc","_id":"_V_suiBEd3QEPBWxMq6yW6SII8cCuP1U","_score":0.0,"_source":{"files":[{"size":119497,"name":"image_744x1024.jpg","mimetype":"image/jpeg","id":"assets/_V_suiBEd3QEPBWxMq6yW6SII8cCuP1U/proxy/image_744x1024.jpg","category":"proxy","attrs":{"width":744,"height":1024}},{"size":119497,"name":"web-proxy.jpg","mimetype":"image/jpeg","id":"assets/_V_suiBEd3QEPBWxMq6yW6SII8cCuP1U/web-proxy/web-proxy.jpg","category":"web-proxy","attrs":{"width":744,"height":1024}},{"size":43062,"name":"image_372x512.jpg","mimetype":"image/jpeg","id":"assets/_V_suiBEd3QEPBWxMq6yW6SII8cCuP1U/proxy/image_372x512.jpg","category":"proxy","attrs":{"width":372,"height":512}},{"size":21318,"name":"image_232x320.jpg","mimetype":"image/jpeg","id":"assets/_V_suiBEd3QEPBWxMq6yW6SII8cCuP1U/proxy/image_232x320.jpg","category":"proxy","attrs":{"width":232,"height":320}}],"source":{"path":"gs://zorroa-dev-data/image/singlepage.tiff","extension":"tiff","filename":"singlepage.tiff","checksum":754419346,"mimetype":"image/tiff","filesize":11082}}},{"_index":"fgctsfya3pdk0oib","_type":"_doc","_id":"vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C","_score":0.0,"_source":{"media":{},"files":[{"size":89643,"name":"image_650x434.jpg","mimetype":"image/jpeg","id":"assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/proxy/image_650x434.jpg","category":"proxy","attrs":{"width":650,"height":434}},{"size":60713,"name":"image_512x341.jpg","mimetype":"image/jpeg","id":"assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/proxy/image_512x341.jpg","category":"proxy","attrs":{"width":512,"height":341}},{"size":30882,"name":"image_320x213.jpg","mimetype":"image/jpeg","id":"assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/proxy/image_320x213.jpg","category":"proxy","attrs":{"width":320,"height":213}}],"source":{"path":"gs://zorroa-dev-data/image/TIFF_1MB.tiff","extension":"tiff","filename":"TIFF_1MB.tiff","checksum":1867533868,"mimetype":"image/tiff","filesize":1131930}}}]}}  # noqa

        monkeypatch.setattr(ZmlpClient, 'post', _response)
        response = api_client.get(reverse('search-query', kwargs={'project_pk': project.id}),
                                  {'query': facet_query_qs})

        content = check_response(response, status=status.HTTP_200_OK)
        assert content['count'] == 2
        assert 'next' in content
        assert 'previous' in content
        # Should only be the requested fields on this request
        assert list(content['results'][0]['metadata']) == ['source']

    def test_get_empty_query(self, login, api_client, project, monkeypatch, mock_response):
        def _response(*args, **kwargs):
            return mock_response

        monkeypatch.setattr(ZmlpClient, 'post', _response)
        response = api_client.get(reverse('search-query', kwargs={'project_pk': project.id}))

        content = check_response(response, status=status.HTTP_200_OK)
        assert content['count'] == 2
        assert list(content['results'][0]['metadata']) == ['source']
        assert content['results'][0]['thumbnailUrl'] == 'http://testserver/api/v1/projects/6abc33f0-4acf-4196-95ff-4cbb7f640a06/assets/_V_suiBEd3QEPBWxMq6yW6SII8cCuP1U/files/category/web-proxy/name/web-proxy.jpg/'  # noqa
        assert content['results'][1]['thumbnailUrl'] == 'http://testserver/icons/fallback_3x.png'

    def test_get_bad_querystring_encoding(self, login, api_client, project, facet_query_qs):
        facet_query_qs = 'thisisnolongerencodedright' + facet_query_qs.decode('utf-8')
        response = api_client.get(reverse('search-query', kwargs={'project_pk': project.id}),
                                  {'query': facet_query_qs})
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == ['Unable to decode `query` query param.']

    def test_empty_query_sorts(self, login, api_client, project):
        def mock_list(*args, **kwargs):
            query = kwargs['search_filter']
            assert query == {'sort': {'system.timeCreated': {'order': 'desc'}},
                             '_source': ['id',
                                         'source*',
                                         'files*',
                                         'media*'],
                             'track_total_hits': True}
            return Response(status=status.HTTP_200_OK)

        path = reverse('search-query', kwargs={'project_pk': project.id})
        with patch.object(SearchViewSet, '_zmlp_list_from_es', mock_list):
            api_client.get(path)


class TestRawQuery(BaseFiltersTestCase):

    def test_get(self, login, api_client, project, facet_query_qs):
        path = reverse('search-raw-query', kwargs={'project_pk': project.id})
        response = api_client.get(path, {'query': facet_query_qs})
        assert response.json() == {
            'results': {
                'query': {
                    'bool': {
                        'filter': [
                            {'terms': {'source.extension': ['tiff']}}
                        ]
                    }
                }
            }
        }


class TestAggregate(BaseFiltersTestCase):

    def test_get(self, login, api_client, project, range_agg_qs, monkeypatch):
        def mock_response(*args, **kwargs):
            return {'took': 34,
                    'timed_out': False,
                    '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0},
                    'hits': {'total': {'value': 24, 'relation': 'eq'},
                             'max_score': None,
                             'hits': []},
                    'aggregations': {
                        'stats#0d2a26ea-1006-4e75-8626-b81809a7a021': {
                            'count': 24,
                            'min': 7555.0,
                            'max': 64657027.0,
                            'avg': 5725264.875,
                            'sum': 137406357.0,
                            'doc_count': 24}}}

        def mock_init(*args, **kwargs):
            # Need to override the internally created name so we can parse our fake response
            (self, data) = (args[0], args[1])
            self.data = data
            self.name = '0d2a26ea-1006-4e75-8626-b81809a7a021'
            self.errors = []

        monkeypatch.setattr(ZmlpClient, 'post', mock_response)
        monkeypatch.setattr(BaseFilter, '__init__', mock_init)
        response = api_client.get(reverse('search-aggregate', kwargs={'project_pk': project.id}),
                                  {'filter': range_agg_qs})
        content = check_response(response, status=status.HTTP_200_OK)
        assert content['count'] == 24
        assert content['results']['min'] == 7555.0
        assert content['results']['max'] == 64657027.0
        assert content['results']['docCount'] == 24

    def test_get_missing_querystring(self, login, api_client, project, range_agg_qs):
        response = api_client.get(reverse('search-aggregate', kwargs={'project_pk': project.id}))
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == ['No `filter` query param included.']

    def test_get_bad_querystring_encoding(self, login, api_client, project, range_agg_qs):
        range_agg_qs = 'thisisnolongerencodedright' + range_agg_qs.decode('utf-8')
        response = api_client.get(reverse('search-aggregate', kwargs={'project_pk': project.id}),
                                  {'filter': range_agg_qs})
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == ['Unable to decode `filter` query param.']

    def test_get_missing_filter_type(self, login, api_client, project, range_agg):
        del(range_agg['type'])
        encoded_filter = convert_json_to_base64(range_agg)
        response = api_client.get(reverse('search-aggregate', kwargs={'project_pk': project.id}),
                                  {'filter': encoded_filter})
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == ['Filter description is missing a `type`.']

    def test_get_missing_filter_type(self, login, api_client, project, range_agg):
        range_agg['type'] = 'fake_type'
        encoded_filter = convert_json_to_base64(range_agg)
        response = api_client.get(reverse('search-aggregate', kwargs={'project_pk': project.id}),
                                  {'filter': encoded_filter})
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == ['Unsupported filter `fake_type` given.']

    def test_bad_querystring_data_format(self, login, api_client, project):
        qs = [{'type': 'range',
               'attribute': 'source.filesize',
               'values': {'min': 4561, 'max': 2924820.9000000004}}]
        encoded_filter = convert_json_to_base64(qs)
        response = api_client.get(reverse('search-aggregate', kwargs={'project_pk': project.id}),
                                  {'filter': encoded_filter})
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == ['Filter format incorrect, did not receive a single JSON '
                                     'object for the Filter.']

    def test_unsupported_filter(self, login, api_client, project):
        qs = {'type': 'textContent', 'attribute': 'analysis.zvi-text-detection'}
        encoded_filter = convert_json_to_base64(qs)
        response = api_client.get(reverse('search-aggregate', kwargs={'project_pk': project.id}),
                                  {'filter': encoded_filter})
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == ['This Filter does not support aggregations.']


@pytest.mark.skip(reason='API is currently disabled due to bugs and security issues.')
class TestMetadataExportView:

    def test_get(self, login, api_client, monkeypatch, project):

        def mock_search_for_assets(*args, **kwargs):
            return [
                {'id': '1', 'metadata': {'resolution': {'width': 10, 'height': 10}}},
                {'id': '2', 'metadata': {'resolution': {'width': 20, 'height': 20}}},
                {'id': '4', 'metadata': {'resolution': {'width': 30, 'height': 30}, 'extra_field': True}},  # noqa
            ]

        monkeypatch.setattr(MetadataExportViewSet, '_search_for_assets', mock_search_for_assets)
        result = api_client.get(reverse('export-list', kwargs={'project_pk': project.id}), {})
        assert result.status_code == 200
        assert result.accepted_media_type == 'text/csv'
        assert result.content == b'extra_field,id,resolution.height,resolution.width\r\n,1,10,10\r\n,2,20,20\r\nTrue,4,30,30\r\n'  # noqa
        assert result.charset == 'utf-8'

    def test_get_empty_included_query(self, login, api_client, monkeypatch, project):

        def mock_search_for_assets(*args, **kwargs):
            return [
                {'id': '1', 'metadata': {'resolution': {'width': 10, 'height': 10}}},
                {'id': '2', 'metadata': {'resolution': {'width': 20, 'height': 20}}},
                {'id': '4', 'metadata': {'resolution': {'width': 30, 'height': 30}, 'extra_field': True}},  # noqa
            ]

        monkeypatch.setattr(MetadataExportViewSet, '_yield_all_items_from_es',
                            mock_search_for_assets)
        result = api_client.get(reverse('export-list', kwargs={'project_pk': project.id}),
                                {'query': ''})
        assert result.status_code == 200
        assert result.accepted_media_type == 'text/csv'
        assert result.content == b'extra_field,id,resolution.height,resolution.width\r\n,1,10,10\r\n,2,20,20\r\nTrue,4,30,30\r\n'  # noqa
        assert result.charset == 'utf-8'

    def test_get_large_export(self, login, api_client, monkeypatch, project):
        def yield_response(*args, **kwargs):
            for x in range(0, 10):
                m = Mock()
                m.id = "00n-Vs_Lb3299-v9EdxdO3dr2DJp2jzz"
                m.document = {
                    "source": {
                        "path": "gs://zmlp-private-test-data/zorroa-deploy-testdata/zorroa-cypress-testdata/cats/00001292_017.jpg",  # noqa
                        "extension": "jpg",
                        "filename": "00001292_017.jpg",
                        "mimetype": "image/jpeg",
                        "filesize": 103955,
                        "checksum": 618508013
                    },
                    "files": [
                        {
                            "id": "assets/00n-Vs_Lb3299-v9EdxdO3dr2DJp2jzz/web-proxy/web-proxy.jpg",
                            "name": "web-proxy.jpg",
                            "category": "web-proxy",
                            "mimetype": "image/jpeg",
                            "size": 37616,
                            "attrs": {
                                "width": 500,
                                "height": 400
                            }
                        }
                    ],
                    "media": {}
                }
                yield m

        path = reverse('export-list', kwargs={'project_pk': project.id})
        with patch('searches.views.AssetSearchScroller.scroll', yield_response):
            result = api_client.get(path, {})

        assert result.status_code == 200
        assert result.accepted_media_type == 'text/csv'
        # 10 items, 1 header and 1 extra line from the split
        assert len(result.content.decode('utf-8').split('\r\n')) == 12
