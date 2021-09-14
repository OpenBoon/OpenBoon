import pytest

from unittest.mock import Mock
from rest_framework.exceptions import ParseError, ValidationError

from wallet.exceptions import InvalidRequestError
from searches.utils import FieldUtility, FilterBuddy
from searches.filters import RangeFilter, FacetFilter, LimitFilter, SimpleSortFilter
from wallet.utils import convert_json_to_base64


class TestFieldUtility:

    field_utility = FieldUtility()

    @pytest.fixture
    def mapping_response(self):
        return {'gel9ztp97ysjzoll': {'aliases': {}, 'mappings': {'dynamic': 'strict',
                                                                 'dynamic_templates': [{
                                                                                           'analysis_label': {
                                                                                               'path_match': 'analysis.*.predictions',
                                                                                               'match_mapping_type': 'object',
                                                                                               'mapping': {
                                                                                                   'dynamic': False,
                                                                                                   'include_in_root': True,
                                                                                                   'properties': {
                                                                                                       'occurrences': {
                                                                                                           'type': 'integer'},
                                                                                                       'score': {
                                                                                                           'type': 'float'},
                                                                                                       'bbox': {
                                                                                                           'type': 'float'},
                                                                                                       'label': {
                                                                                                           'type': 'keyword',
                                                                                                           'fields': {
                                                                                                               'fulltext': {
                                                                                                                   'analyzer': 'default',
                                                                                                                   'type': 'text'}}},
                                                                                                       'point': {
                                                                                                           'type': 'geo_point'},
                                                                                                       'simhash': {
                                                                                                           'index': False,
                                                                                                           'type': 'keyword'},
                                                                                                       'tags': {
                                                                                                           'type': 'keyword',
                                                                                                           'fields': {
                                                                                                               'fulltext': {
                                                                                                                   'analyzer': 'default',
                                                                                                                   'type': 'text'}}}},
                                                                                                   'type': 'nested'}}},
                                                                                       {'simhash': {
                                                                                           'match': 'simhash',
                                                                                           'match_mapping_type': 'string',
                                                                                           'match_pattern': 'regex',
                                                                                           'mapping': {
                                                                                               'index': False,
                                                                                               'type': 'keyword'}}},
                                                                                       {'content': {
                                                                                           'match': 'content',
                                                                                           'match_mapping_type': 'string',
                                                                                           'mapping': {
                                                                                               'analyzer': 'default',
                                                                                               'type': 'text'}}}],
                                                                 'properties': {
                                                                     'analysis': {'dynamic': 'true',
                                                                                  'properties': {
                                                                                      'zvi-face-detection': {
                                                                                          'properties': {
                                                                                              'count': {
                                                                                                  'type': 'long'},
                                                                                              'predictions': {
                                                                                                  'type': 'nested',
                                                                                                  'include_in_root': True,
                                                                                                  'dynamic': 'false',
                                                                                                  'properties': {
                                                                                                      'bbox': {
                                                                                                          'type': 'float'},
                                                                                                      'label': {
                                                                                                          'type': 'keyword',
                                                                                                          'fields': {
                                                                                                              'fulltext': {
                                                                                                                  'type': 'text'}}},
                                                                                                      'occurrences': {
                                                                                                          'type': 'integer'},
                                                                                                      'point': {
                                                                                                          'type': 'geo_point'},
                                                                                                      'score': {
                                                                                                          'type': 'float'},
                                                                                                      'simhash': {
                                                                                                          'type': 'keyword',
                                                                                                          'index': False},
                                                                                                      'tags': {
                                                                                                          'type': 'keyword',
                                                                                                          'fields': {
                                                                                                              'fulltext': {
                                                                                                                  'type': 'text'}}}}},
                                                                                              'type': {
                                                                                                  'type': 'text',
                                                                                                  'fields': {
                                                                                                      'keyword': {
                                                                                                          'type': 'keyword',
                                                                                                          'ignore_above': 256}}}}},
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
                                                                                                  'type': 'nested',
                                                                                                  'include_in_root': True,
                                                                                                  'dynamic': 'false',
                                                                                                  'properties': {
                                                                                                      'bbox': {
                                                                                                          'type': 'float'},
                                                                                                      'label': {
                                                                                                          'type': 'keyword',
                                                                                                          'fields': {
                                                                                                              'fulltext': {
                                                                                                                  'type': 'text'}}},
                                                                                                      'occurrences': {
                                                                                                          'type': 'integer'},
                                                                                                      'point': {
                                                                                                          'type': 'geo_point'},
                                                                                                      'score': {
                                                                                                          'type': 'float'},
                                                                                                      'simhash': {
                                                                                                          'type': 'keyword',
                                                                                                          'index': False},
                                                                                                      'tags': {
                                                                                                          'type': 'keyword',
                                                                                                          'fields': {
                                                                                                              'fulltext': {
                                                                                                                  'type': 'text'}}}}},
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
                                                                     'aux': {'type': 'object',
                                                                             'enabled': False},
                                                                     'clip': {'dynamic': 'strict',
                                                                              'properties': {
                                                                                  'length': {
                                                                                      'type': 'double'},
                                                                                  'pile': {
                                                                                      'type': 'keyword'},
                                                                                  'sourceAssetId': {
                                                                                      'type': 'keyword'},
                                                                                  'start': {
                                                                                      'type': 'double'},
                                                                                  'stop': {
                                                                                      'type': 'double'},
                                                                                  'track': {
                                                                                      'type': 'keyword'},
                                                                                  'type': {
                                                                                      'type': 'keyword'}}},
                                                                     'files': {'type': 'object',
                                                                               'enabled': False},
                                                                     'labels': {'type': 'nested',
                                                                                'dynamic': 'false',
                                                                                'properties': {
                                                                                    'bbox': {
                                                                                        'type': 'float'},
                                                                                    'label': {
                                                                                        'type': 'keyword',
                                                                                        'fields': {
                                                                                            'fulltext': {
                                                                                                'type': 'text'}}},
                                                                                    'modelId': {
                                                                                        'type': 'keyword'},
                                                                                    'scope': {
                                                                                        'type': 'keyword'},
                                                                                    'simhash': {
                                                                                        'type': 'keyword',
                                                                                        'index': False}}},
                                                                     'location': {
                                                                         'dynamic': 'strict',
                                                                         'properties': {'city': {
                                                                             'type': 'keyword'},
                                                                                        'code': {
                                                                                            'type': 'keyword'},
                                                                                        'country': {
                                                                                            'type': 'keyword'},
                                                                                        'point': {
                                                                                            'type': 'geo_point'}}},
                                                                     'media': {'dynamic': 'strict',
                                                                               'properties': {
                                                                                   'aspect': {
                                                                                       'type': 'float'},
                                                                                   'author': {
                                                                                       'type': 'keyword',
                                                                                       'fields': {
                                                                                           'fulltext': {
                                                                                               'type': 'text'}}},
                                                                                   'content': {
                                                                                       'type': 'text'},
                                                                                   'description': {
                                                                                       'type': 'keyword',
                                                                                       'fields': {
                                                                                           'fulltext': {
                                                                                               'type': 'text'}}},
                                                                                   'height': {
                                                                                       'type': 'float'},
                                                                                   'keywords': {
                                                                                       'type': 'keyword',
                                                                                       'fields': {
                                                                                           'fulltext': {
                                                                                               'type': 'text'}}},
                                                                                   'length': {
                                                                                       'type': 'float'},
                                                                                   'orientation': {
                                                                                       'type': 'keyword'},
                                                                                   'timeCreated': {
                                                                                       'type': 'date'},
                                                                                   'title': {
                                                                                       'type': 'keyword',
                                                                                       'fields': {
                                                                                           'fulltext': {
                                                                                               'type': 'text'}}},
                                                                                   'type': {
                                                                                       'type': 'keyword'},
                                                                                   'videoCodec': {
                                                                                       'type': 'keyword'},
                                                                                   'width': {
                                                                                       'type': 'float'}}},
                                                                     'metrics': {
                                                                         'dynamic': 'strict',
                                                                         'properties': {
                                                                             'pipeline': {
                                                                                 'type': 'nested',
                                                                                 'dynamic': 'strict',
                                                                                 'properties': {
                                                                                     'checksum': {
                                                                                         'type': 'long'},
                                                                                     'error': {
                                                                                         'type': 'keyword'},
                                                                                     'executionDate': {
                                                                                         'type': 'date'},
                                                                                     'executionTime': {
                                                                                         'type': 'double'},
                                                                                     'module': {
                                                                                         'type': 'keyword',
                                                                                         'fields': {
                                                                                             'fulltext': {
                                                                                                 'type': 'text'}}},
                                                                                     'processor': {
                                                                                         'type': 'keyword',
                                                                                         'fields': {
                                                                                             'fulltext': {
                                                                                                 'type': 'text'}}}}}}},
                                                                     'source': {'dynamic': 'strict',
                                                                                'properties': {
                                                                                    'checksum': {
                                                                                        'type': 'long'},
                                                                                    'extension': {
                                                                                        'type': 'keyword',
                                                                                        'fields': {
                                                                                            'fulltext': {
                                                                                                'type': 'text'}}},
                                                                                    'filename': {
                                                                                        'type': 'keyword',
                                                                                        'fields': {
                                                                                            'fulltext': {
                                                                                                'type': 'text'}}},
                                                                                    'filesize': {
                                                                                        'type': 'long'},
                                                                                    'mimetype': {
                                                                                        'type': 'keyword',
                                                                                        'fields': {
                                                                                            'fulltext': {
                                                                                                'type': 'text'}}},
                                                                                    'path': {
                                                                                        'type': 'keyword',
                                                                                        'fields': {
                                                                                            'fulltext': {
                                                                                                'type': 'text'},
                                                                                            'path': {
                                                                                                'type': 'text',
                                                                                                'analyzer': 'path_analyzer',
                                                                                                'fielddata': True}}}}},
                                                                     'system': {'dynamic': 'strict',
                                                                                'properties': {
                                                                                    'dataSourceId': {
                                                                                        'type': 'keyword'},
                                                                                    'jobId': {
                                                                                        'type': 'keyword'},
                                                                                    'projectId': {
                                                                                        'type': 'keyword'},
                                                                                    'state': {
                                                                                        'type': 'keyword'},
                                                                                    'taskId': {
                                                                                        'type': 'keyword'},
                                                                                    'timeCreated': {
                                                                                        'type': 'date'},
                                                                                    'timeModified': {
                                                                                        'type': 'date'}}},
                                                                     'tmp': {'type': 'object',
                                                                             'enabled': False}}},
                                     'settings': {'index': {'mapping': {'coerce': 'false',
                                                                        'ignore_malformed': 'false'},
                                                            'number_of_shards': '2',
                                                            'provided_name': 'gel9ztp97ysjzoll',
                                                            'creation_date': '1598988916327',
                                                            'analysis': {'filter': {
                                                                'delimiter_filter': {
                                                                    'type': 'word_delimiter',
                                                                    'preserve_original': 'true'},
                                                                'stemmer_english': {
                                                                    'type': 'stemmer',
                                                                    'language': 'english'}},
                                                                         'analyzer': {'default': {
                                                                             'filter': ['trim',
                                                                                        'stop',
                                                                                        'lowercase',
                                                                                        'delimiter_filter',
                                                                                        'stemmer_english'],
                                                                             'tokenizer': 'standard'},
                                                                                      'path_analyzer': {
                                                                                          'type': 'custom',
                                                                                          'tokenizer': 'path_tokenizer'}},
                                                                         'tokenizer': {
                                                                             'path_tokenizer': {
                                                                                 'type': 'path_hierarchy',
                                                                                 'delimiter': '/'}}},
                                                            'number_of_replicas': '1',
                                                            'uuid': 'T5t7z3FPTg-kbhmABb8g9A',
                                                            'version': {'created': '7060299'}}}}}

    @pytest.fixture
    def model_response(self):
        return {'list': [{'id': '3500f84e-26f2-1505-9aa6-0242ac13000b',
                          'projectId': '00000000-0000-0000-0000-000000000000',
                          'type': 'ZVI_KNN_CLASSIFIER', 'name': 'Settings',
                          'moduleName': 'settings',
                          'fileId': 'models/3500f84e-26f2-1505-9aa6-0242ac13000b/model/model.zip',
                          'trainingJobName': 'Training model: Settings - [Label Detection]',
                          'ready': False, 'deploySearch': {'query': {'match_all': {}}},
                          'timeCreated': 1598999201703, 'timeModified': 1598999201703,
                          'actorCreated': 'd4c9b0f8-fedc-4d22-9cff-01a0e8e1eee9/Admin Console Generated Key - a8aae7b3-4fa5-4cde-8fa5-d05a9a9700fc - software@zorroa.com_00000000-0000-0000-0000-000000000000',
                          'actorModified': 'd4c9b0f8-fedc-4d22-9cff-01a0e8e1eee9/Admin Console Generated Key - a8aae7b3-4fa5-4cde-8fa5-d05a9a9700fc - software@zorroa.com_00000000-0000-0000-0000-000000000000'}],
                'page': {'from': 0, 'size': 50, 'disabled': False, 'totalCount': 1}}

    @pytest.fixture
    def mock_zmlp_client(self, mapping_response, model_response):
        return Mock(get=Mock(return_value=mapping_response),
                    post=Mock(return_value=model_response))

    def test_converts_similarity_blob(self, mock_zmlp_client):
        result = self.field_utility.get_filter_map(mock_zmlp_client)
        assert result['analysis']['zvi-image-similarity'] == ['exists', 'similarity']

    def test_zvi_label_detection(self, mock_zmlp_client):
        result = self.field_utility.get_filter_map(mock_zmlp_client)
        assert result['analysis']['zvi-label-detection'] == ['labelConfidence', 'predictionCount',
                                                             'exists']

    def test_labels(self, mock_zmlp_client):
        result = self.field_utility.get_filter_map(mock_zmlp_client)
        assert result['labels'] == {'3500f84e-26f2-1505-9aa6-0242ac13000b': ['label', 'labelsExist']}

    def test_utility_fields(self, mock_zmlp_client):
        result = self.field_utility.get_filter_map(mock_zmlp_client)
        assert result['utility'] == {'Search Results Limit': ['limit']}

    def test_get_all_dataset_ids_no_models(self):
        client = Mock(post=Mock(return_value={'list': []}))
        result = self.field_utility._get_all_dataset_ids(client)
        assert result == []

    @pytest.mark.parametrize('attr,expected', [
        ('analysis.zvi-face-detection', 'prediction'),
        ('analysis.zvi-label-detection', 'prediction'),
        ('analysis.zvi-image-similarity', 'similarity'),
        ('analysis.zvi-text-detection', 'text_content'),
        ('aux', 'object'),
        ('media.timeCreated', 'date'),
        ('source.checksum', 'long'),
        ('source.extension', 'keyword'),
        ('labels.3500f84e-26f2-1505-9aa6-0242ac13000b', 'label')
    ])
    def test_get_attribute_field_type(self, attr, expected, mock_zmlp_client):
        result = self.field_utility.get_attribute_field_type(attr, mock_zmlp_client)
        assert result == expected

    def test_get_attribute_field_type_nonexistant_attr(self, mock_zmlp_client):
        with pytest.raises(ParseError) as e:
            self.field_utility.get_attribute_field_type('howdy', mock_zmlp_client)
        assert e.value.detail[0] == 'Given attribute could not be found in field mapping.'

    def test_get_attribute_field_type_non_leaf_attr(self, mock_zmlp_client):
        with pytest.raises(ParseError) as e:
            self.field_utility.get_attribute_field_type('source', mock_zmlp_client)
        assert e.value.detail[0] == ('Attribute given is not a valid '
                                     'filterable or visualizable field.')


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
        expected_query = {'query': {
            'bool': {'filter': [{'range': {'source.filesize': {'gte': 1, 'lte': 100}}}]}}}  # noqa
        response_query = filter_boy.reduce_filters_to_query([_filter], None)
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
        response_query = filter_boy.reduce_filters_to_query(_filters, None)
        assert expected_query == response_query

    def test_validate_filters(self, filter_boy):
        _filters = [RangeFilter({'type': 'range',
                                 'attribute': 'source.filesize',
                                 'values': {'min': 1, 'max': 100}}),
                    FacetFilter({'type': 'facet',
                                 'attribute': 'source.extension',
                                 'values': {'facets': ['jpeg', 'tiff']}})]
        filter_boy.validate_filters(_filters)
        # If we got here everything was valid

    def test_validate_filters_raises(self, filter_boy):
        _filters = [RangeFilter({'type': 'range',
                                 'attribute': 'source.filesize',
                                 'values': {}}),
                    FacetFilter({'type': 'facet',
                                 'attribute': 'source.extension',
                                 'values': {'facets': ['jpeg', 'tiff']}})]
        with pytest.raises(ValidationError):
            filter_boy.validate_filters(_filters)

    def test_finalize_query_normal_filters(self, filter_boy, snapshot):
        _filters = [RangeFilter({'type': 'range',
                                 'attribute': 'source.filesize',
                                 'values': {'min': 1, 'max': 100}}),
                    FacetFilter({'type': 'facet',
                                 'attribute': 'source.extension',
                                 'values': {'facets': ['jpeg', 'tiff']}}),
                    LimitFilter({'type': 'limit',
                                 'values': {'maxAssets': 20}})]
        request = Mock()
        query = filter_boy.finalize_query_from_filters_and_request(_filters, request)
        snapshot.assert_match(query)
        assert request.max_assets == 20

    def test_finalize_query_only_limit_filter(self, filter_boy, snapshot):
        _filters = [LimitFilter({'type': 'limit',
                                 'values': {'maxAssets': 20}})]
        request = Mock()
        query = filter_boy.finalize_query_from_filters_and_request(_filters, request)
        snapshot.assert_match(query)
        assert request.max_assets == 20

    def test_finalize_query_with_multiple_simple_sort(self, filter_boy, snapshot):
        _filters = [LimitFilter({'type': 'limit',
                                 'values': {'maxAssets': 20}}),
                    SimpleSortFilter({'type': 'simpleSort',
                                      'attribute': 'media.length',
                                      'values': {'order': 'asc'}}),
                    SimpleSortFilter({'type': 'simpleSort',
                                      'attribute': 'media.size',
                                      'values': {'order': 'desc'}}),
                    SimpleSortFilter({'type': 'simpleSort',
                                      'attribute': 'system.name',
                                      'values': {'order': 'asc'}})]
        request = Mock()
        query = filter_boy.finalize_query_from_filters_and_request(_filters, request)
        snapshot.assert_match(query)
        assert request.max_assets == 20
