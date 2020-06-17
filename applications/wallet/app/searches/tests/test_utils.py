import pytest

from unittest.mock import Mock
from rest_framework.exceptions import ParseError

from wallet.exceptions import InvalidRequestError
from searches.utils import FieldUtility, FilterBoy
from searches.filters import RangeFilter, FacetFilter
from wallet.utils import convert_json_to_base64


class TestFieldUtility:
    field_service = FieldUtility()

    @pytest.fixture
    def similarity_mapping(self):
        return {
            "mappings": {
                "properties": {
                    "analysis": {
                        "dynamic": "true",
                        "properties": {
                            "zvi": {
                                "properties": {
                                    "tinyProxy": {
                                        "type": "text",
                                        "fields": {
                                            "keyword": {
                                                "type": "keyword",
                                                "ignore_above": 256}}}}},
                            "zvi-image-similarity": {
                                "properties": {
                                    "simhash": {
                                        "type": "keyword",
                                        "index": False},
                                    "type": {
                                        "type": "text",
                                        "fields": {
                                            "keyword": {
                                                "type": "keyword",
                                                "ignore_above": 256}}}}},
                            "aux": {}
                        }}}}}

    def test_converts_similarity_blob(self, similarity_mapping):
        properties = similarity_mapping['mappings']['properties']
        result = self.field_service.get_fields_from_mappings(properties['analysis'])
        assert result['zvi-image-similarity'] == ['similarity']

    @pytest.fixture
    def analysis_mappings(self):
        return {
            'mappings': {
                'properties': {
                    'analysis': {'dynamic': 'true',
                                 'properties': {
                                     'zvi-image-similarity': {
                                         'properties': {
                                             'simhash': {
                                                 'type': 'keyword',
                                                 'index': False},
                                             'type': {
                                                 'type': 'text',
                                                 'fields': {
                                                     'keyword': {
                                                         'type': 'keyword',
                                                         'ignore_above': 256}}}}},
                                     'zvi-label-detection': {
                                         'properties': {
                                             'count': {
                                                 'type': 'long'},
                                             'predictions': {
                                                 'properties': {
                                                     'label': {
                                                         'type': 'keyword',
                                                         'fields': {
                                                             'fulltext': {
                                                                 'type': 'text'}}},
                                                     'score': {
                                                         'type': 'float',
                                                         'coerce': True}}},
                                             'type': {
                                                 'type': 'text',
                                                 'fields': {
                                                     'keyword': {
                                                         'type': 'keyword',
                                                         'ignore_above': 256}}}}},
                                     'zvi-object-detection': {
                                         'properties': {
                                             'count': {
                                                 'type': 'long'},
                                             'predictions': {
                                                 'properties': {
                                                     'bbox': {
                                                         'type': 'float'},
                                                     'label': {
                                                         'type': 'keyword',
                                                         'fields': {
                                                             'fulltext': {
                                                                 'type': 'text'}}},
                                                     'score': {
                                                         'type': 'float',
                                                         'coerce': True}}},
                                             'type': {
                                                 'type': 'text',
                                                 'fields': {
                                                     'keyword': {
                                                         'type': 'keyword',
                                                         'ignore_above': 256}}}}},
                                     'zvi-text-detection': {
                                         'properties': {
                                             'content': {
                                                 'type': 'text'},
                                             'type': {
                                                 'type': 'text',
                                                 'fields': {
                                                     'keyword': {
                                                         'type': 'keyword',
                                                         'ignore_above': 256}}},
                                             'words': {
                                                 'type': 'long'}}}}},
                }},
        }

    def test_zvi_label_detection(self, analysis_mappings):
        properties = analysis_mappings['mappings']['properties']
        result = self.field_service.get_fields_from_mappings(properties['analysis'])
        assert result['zvi-label-detection'] == ['labelConfidence']


class TestFilterBoy:

    @pytest.fixture
    def filter_boy(self):
        return FilterBoy()

    def test_is_it_a_good_boy(self, filter_boy):
        assert True

    def test_encoded_querystring(self, api_factory, filter_boy):
        encoded = 'eyJ0eXBlIjoiZmFjZXQiLCJhdHRyaWJ1dGUiOiJhbmFseXNpcy56dmkudGlueVByb3h5In0='
        request = Mock()
        request.query_params = {'filter': encoded}

        response_filter = filter_boy.get_filter_from_request(request)
        assert FacetFilter({"type": "facet",
                            "attribute": "analysis.zvi.tinyProxy"
                            }) == response_filter

    def test_get_filter_from_request_flow(self, api_factory, filter_boy):
        _filter = {'type': 'range', 'attribute': 'source.filesize'}
        encoded_qs = convert_json_to_base64(_filter)
        request = Mock()
        request.query_params = {'filter': encoded_qs}

        response_filter = filter_boy.get_filter_from_request(request)
        assert RangeFilter(_filter) == response_filter

    def test_get_filter_from_request_raises_invalid_request(self, filter_boy):
        request = Mock()
        request.query_params = {}

        with pytest.raises(InvalidRequestError) as e:
            filter_boy.get_filter_from_request(request)

        assert str(e.value.detail) == 'No `filter` querystring included.'

    def test_get_filter_from_request_raises_parse_error(self, filter_boy):
        _filter = {'type': 'range', 'attribute': 'source.filesize'}
        encoded_qs = 'adsfasdfasdfasf' + str(convert_json_to_base64(_filter))
        request = Mock()
        request.query_params = {'filter': encoded_qs}

        with pytest.raises(ParseError) as e:
            filter_boy.get_filter_from_request(request)

        assert str(e.value.detail) == 'Unable to decode `filter` querystring.'

    def test_get_filters_from_request(self, filter_boy):
        _filters = [{'type': 'range', 'attribute': 'source.filesize'},
                    {'type': 'facet', 'attribute': 'source.extension'}]
        encoded_qs = convert_json_to_base64(_filters)
        request = Mock()
        request.query_params = {'query': encoded_qs}

        expected = [RangeFilter(_filters[0]), FacetFilter(_filters[1])]
        filters = filter_boy.get_filters_from_request(request)
        assert filters == expected

    def test_get_filters_from_request_raises_parse_error(self, filter_boy):
        _filters = [{'type': 'range', 'attribute': 'source.filesize'},
                    {'type': 'facet', 'attribute': 'source.extension'}]
        encoded_qs = 'adsfasdfasdfasf' + str(convert_json_to_base64(_filters))
        request = Mock()
        request.query_params = {'query': encoded_qs}

        with pytest.raises(ParseError) as e:
            filter_boy.get_filters_from_request(request)

        assert str(e.value.detail) == 'Unable to decode `query` querystring.'

    def test_get_filter_from_json(self, filter_boy):
        raw_filter = {'type': 'range', 'attribute': 'source.filesize'}
        _filter = filter_boy.get_filter_from_json(raw_filter, None)

        assert _filter == RangeFilter(raw_filter)

    def test_get_filter_from_json_raises_parse_error(self, filter_boy):
        raw_filter = {'attribute': 'source.filesize'}

        with pytest.raises(ParseError) as e:
            filter_boy.get_filter_from_json(raw_filter)
        assert str(e.value.detail) == 'Filter description is missing a `type`.'

    def test_get_filter_from_json_raises_parse_error_unknown_type(self, filter_boy):
        raw_filter = {'type': 'foo', 'attribute': 'source.filesize'}

        with pytest.raises(ParseError) as e:
            filter_boy.get_filter_from_json(raw_filter)
        assert str(e.value.detail) == 'Unsupported filter `foo` given.'

    def test_reduce_filters_to_query_single_filter(self, filter_boy):
        _filter = RangeFilter({'type': 'range',
                               'attribute': 'source.filesize',
                               'values': {'min': 1, 'max': 100}})
        expected_query = {'query': {'bool': {'filter': [{'range': {'source.filesize': {'gte': 1, 'lte': 100}}}]}}}  # noqa
        response_query = filter_boy.reduce_filters_to_query([_filter])
        assert expected_query == response_query

    def test_reduce_filters_to_query_multiple_filters(self, filter_boy):
        _filters = [RangeFilter({'type': 'range',
                                 'attribute': 'source.filesize',
                                 'values': {'min': 1, 'max': 100}}),
                    FacetFilter({'type': 'facet',
                                 'attribute': 'source.extension',
                                 'values': {'facets': ['jpeg', 'tiff']}})]
        expected_query = {
            'query': {'bool': {'filter': [{'range': {'source.filesize': {'gte': 1, 'lte': 100}}},
                                          {'terms': {'source.extension': ['jpeg', 'tiff']}}]}}}
        response_query = filter_boy.reduce_filters_to_query(_filters)
        assert expected_query == response_query
