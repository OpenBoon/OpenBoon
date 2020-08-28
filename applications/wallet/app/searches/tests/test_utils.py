import pytest

from django.test import override_settings
from unittest.mock import Mock, patch
from rest_framework.exceptions import ParseError

from wallet.exceptions import InvalidRequestError
from searches.utils import FieldUtility, FilterBuddy
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
        assert result['zvi-image-similarity'] == ['exists', 'similarity']

    @pytest.fixture
    def analysis_mappings(self):
        return {
            'mappings': {
                'properties': {
                    'analysis': {
                        'dynamic': 'true',
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
                    "labels": {
                        "type": "nested",
                        "dynamic": "strict",
                        "properties": {
                            "bbox": {
                                "type": "float"
                            },
                            "dataSetId": {
                                "type": "keyword"
                            },
                            "label": {
                                "type": "keyword",
                                "fields": {
                                    "fulltext": {
                                        "type": "text"
                                    }
                                }
                            },
                            "modelId": {
                                "type": "keyword"
                            },
                            "scope": {
                                "type": "keyword"
                            },
                            "simhash": {
                                "type": "keyword",
                                "index": False
                            }
                        }
                    },
                }},
        }

    def test_zvi_label_detection(self, analysis_mappings):
        properties = analysis_mappings['mappings']['properties']
        result = self.field_service.get_fields_from_mappings(properties['analysis'])
        assert result['zvi-label-detection'] == ['labelConfidence', 'exists']

    @patch.object(FieldUtility, '_get_all_model_ids', return_value=['A1', 'B1'])
    @patch.object(FieldUtility, '_get_all_model_names', return_value=['console', 'testing'])
    def test_labels(self, model_mock, ids_mock, analysis_mappings):
        client = Mock()
        result = self.field_service.get_fields_from_mappings(analysis_mappings['mappings'], client)
        assert result['labels'] == {'console': ['label'], 'testing': ['label']}

    @override_settings(FEATURE_FLAGS={'USE_MODEL_IDS_FOR_LABEL_FILTERS': True})
    @patch.object(FieldUtility, '_get_all_model_ids', return_value=['A1', 'B1'])
    @patch.object(FieldUtility, '_get_all_model_names', return_value=['console', 'testing'])
    def test_labels_with_feature_flag(self, model_mock, ids_mock, analysis_mappings):
        client = Mock()
        result = self.field_service.get_fields_from_mappings(analysis_mappings['mappings'], client)
        assert result['labels'] == {'A1': ['label'], 'B1': ['label']}

    @override_settings(FEATURE_FLAGS={'USE_MODEL_IDS_FOR_LABEL_FILTERS': 'anything'})
    @patch.object(FieldUtility, '_get_all_model_ids', return_value=['A1', 'B1'])
    @patch.object(FieldUtility, '_get_all_model_names', return_value=['console', 'testing'])
    def test_labels_with_feature_flag_string(self, model_mock, ids_mock, analysis_mappings):
        client = Mock()
        result = self.field_service.get_fields_from_mappings(analysis_mappings['mappings'],
                                                             client)
        assert result['labels'] == {'A1': ['label'], 'B1': ['label']}

    @patch.object(FieldUtility, '_get_all_model_names', return_value=['console', 'testing'])
    def test_labels_no_client(self, model_mock, analysis_mappings):
        result = self.field_service.get_fields_from_mappings(analysis_mappings['mappings'])
        assert result['labels'] == {}

    def test_get_all_model_names_no_models(self):
        client = Mock(post=Mock(return_value={'list': []}))
        result = self.field_service._get_all_model_names(client)
        assert result == []


class TestFilterBoy:

    @pytest.fixture
    def filter_boy(self):
        return FilterBuddy()

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

        assert str(e.value.detail['detail'][0]) == 'No `filter` query param included.'

    def test_get_filter_from_request_raises_parse_error(self, filter_boy):
        _filter = {'type': 'range', 'attribute': 'source.filesize'}
        encoded_qs = 'adsfasdfasdfasf' + str(convert_json_to_base64(_filter))
        request = Mock()
        request.query_params = {'filter': encoded_qs}

        with pytest.raises(ParseError) as e:
            filter_boy.get_filter_from_request(request)

        assert str(e.value.detail['detail'][0]) == 'Unable to decode `filter` query param.'

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

        assert str(e.value.detail['detail'][0]) == 'Unable to decode `query` query param.'

    def test_get_filter_from_json(self, filter_boy):
        raw_filter = {'type': 'range', 'attribute': 'source.filesize'}
        _filter = filter_boy.get_filter_from_json(raw_filter, None)

        assert _filter == RangeFilter(raw_filter)

    def test_get_filter_from_json_raises_parse_error(self, filter_boy):
        raw_filter = {'attribute': 'source.filesize'}

        with pytest.raises(ParseError) as e:
            filter_boy.get_filter_from_json(raw_filter)
        assert str(e.value.detail['detail'][0]) == 'Filter description is missing a `type`.'

    def test_get_filter_from_json_raises_parse_error_unknown_type(self, filter_boy):
        raw_filter = {'type': 'foo', 'attribute': 'source.filesize'}

        with pytest.raises(ParseError) as e:
            filter_boy.get_filter_from_json(raw_filter)
        assert str(e.value.detail['detail'][0]) == 'Unsupported filter `foo` given.'

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
