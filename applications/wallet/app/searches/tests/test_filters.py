import pytest
from unittest.mock import Mock

from django.core.exceptions import ImproperlyConfigured
from rest_framework.exceptions import ValidationError
from boonsdk import BoonApp, BoonClient

from searches.filters import (BaseFilter, RangeFilter, ExistsFilter, FacetFilter,
                              LabelConfidenceFilter, TextContentFilter,
                              SimilarityFilter, LabelFilter, DateFilter)


class MockFilter(BaseFilter):
    type = 'danny'
    required_agg_keys = ['trucks', 'friends']
    required_query_keys = ['dogs']


class TestBaseFilterClass:

    @pytest.fixture
    def mock_load_data(self):
        return {'type': 'danny',
                'trucks': ['4runner', 'tacoma'],
                'friends': ['tizzle', 'john', 'zach']}

    @pytest.fixture
    def mock_query_data(self, mock_load_data):
        data = mock_load_data
        data['values'] = {'dogs': ['lemon', 'taylor', 'cookie']}
        return data

    def test_is_valid_with_raise_exception(self, mock_load_data):
        _filter = MockFilter(mock_load_data)
        assert _filter.is_valid()

    def test_is_valid_with_raise_exception(self, mock_load_data):
        _filter = MockFilter(mock_load_data)
        assert _filter.is_valid(raise_exception=True)

    def test_is_valid_no_query_no_exception_missing_key(self, mock_load_data):
        del (mock_load_data['trucks'])
        _filter = MockFilter(mock_load_data)
        assert not _filter.is_valid()

    def test_is_valid_no_query_raise_exception_missing_key(self, mock_load_data):
        del (mock_load_data['trucks'])
        _filter = MockFilter(mock_load_data)
        with pytest.raises(ValidationError):
            _filter.is_valid(raise_exception=True)

    def test_is_valid_query_no_exception(self, mock_query_data):
        _filter = MockFilter(mock_query_data)
        assert _filter.is_valid(query=True)

    def test_is_valid_query_with_raise_exception(self, mock_query_data):
        _filter = MockFilter(mock_query_data)
        assert _filter.is_valid(query=True, raise_exception=True)

    def test_is_valid_query_no_exception_missing_key(self, mock_query_data):
        del (mock_query_data['values']['dogs'])
        _filter = MockFilter(mock_query_data)
        assert not _filter.is_valid(query=True)

    def test_is_valid_query_raise_exception_missing_key(self, mock_query_data):
        del (mock_query_data['values']['dogs'])
        _filter = MockFilter(mock_query_data)
        with pytest.raises(ValidationError):
            _filter.is_valid(query=True, raise_exception=True)


class FilterBaseTestCase:
    """Set the Filter on the inherited test case and it will run these tests automatically,
    if you've setup the mock_data and mock_query_data fixtures correctly."""

    Filter = None

    @pytest.fixture
    def mock_data(self):
        return {}

    @pytest.fixture
    def mock_query_data(self, mock_data):
        return mock_data

    def test_is_valid(self, mock_data):
        if self.Filter:
            _filter = self.Filter(mock_data)
            assert _filter.is_valid()

    def test_is_valid_for_query(self, mock_query_data):
        if self.Filter:
            _filter = self.Filter(mock_query_data)
            assert _filter.is_valid(query=True)


class TestExistsFilter(FilterBaseTestCase):

    Filter = ExistsFilter

    @pytest.fixture
    def mock_data(self):
        return {'type': ExistsFilter.type,
                'attribute': 'name'}

    @pytest.fixture
    def mock_query_data(self, mock_data):
        data = mock_data
        data['values'] = {'exists': True}
        return data

    def test_get_es_agg(self, mock_data):
        _filter = ExistsFilter(mock_data)
        with pytest.raises(NotImplementedError):
            _filter.get_es_agg()

    def test_get_es_query_exists(self, mock_query_data):
        _filter = ExistsFilter(mock_query_data)
        query = _filter.get_es_query()
        assert query == {'query': {'bool': {'filter': [{'exists': {'field': 'name'}}]}}}

    def test_get_es_query_missing(self, mock_query_data):
        mock_query_data['values']['exists'] = False
        _filter = ExistsFilter(mock_query_data)
        query = _filter.get_es_query()
        assert query == {'query': {'bool': {'must_not': [{'exists': {'field': 'name'}}]}}}

    def test_add_to_empty_query(self, mock_query_data):
        _filter = ExistsFilter(mock_query_data)
        query = {}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'exists': {'field': 'name'}}]}}}

    def test_add_to_pagination_empty_query(self, mock_query_data):
        _filter = ExistsFilter(mock_query_data)
        query = {'from': 0, 'size': 10}
        query = _filter.add_to_query(query)
        assert query == {'from': 0, 'size': 10,
                         'query': {'bool': {'filter': [{'exists': {'field': 'name'}}]}}}

    def test_add_to_empty_query_2(self, mock_query_data):
        _filter = ExistsFilter(mock_query_data)
        query = {'query': {}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'exists': {'field': 'name'}}]}}}

    def test_add_to_empty_bool(self, mock_query_data):
        _filter = ExistsFilter(mock_query_data)
        query = {'query': {'bool': {}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'exists': {'field': 'name'}}]}}}

    def test_add_to_empty_other_query_type(self, mock_query_data):
        _filter = ExistsFilter(mock_query_data)
        query = {'query': {'stuff': {}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'stuff': {},
                                   'bool': {'filter': [{'exists': {'field': 'name'}}]}}}

    def test_add_to_existing_exists_query(self, mock_query_data):
        _filter = ExistsFilter(mock_query_data)
        query = {'query': {'bool': {'filter': [{'exists': {'field': 'foo'}}]}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'exists': {'field': 'foo'}},
                                                       {'exists': {'field': 'name'}}]}}}

    def test_add_to_existing_missing_query(self, mock_query_data):
        _filter = ExistsFilter(mock_query_data)
        query = {'query': {'bool': {'must_not': [{'exists': {'field': 'foo'}}]}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'exists': {'field': 'name'}}],
                                            'must_not': [{'exists': {'field': 'foo'}}]}}}

    def test_add_to_existing_other_query(self, mock_query_data):
        _filter = ExistsFilter(mock_query_data)
        query = {'query': {'bool': {'filter': [{'terms': ['foo', 'bar']}]}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'terms': ['foo', 'bar']},
                                                       {'exists': {'field': 'name'}}]}}}


class TestRangeFilter(FilterBaseTestCase):

    Filter = RangeFilter

    @pytest.fixture
    def mock_data(self):
        return {'type': RangeFilter.type,
                'attribute': 'my_attr'}

    @pytest.fixture
    def mock_query_data(self, mock_data):
        data = mock_data
        data['values'] = {'min': 1, 'max': 100}
        return data

    def test_get_es_agg(self, mock_data):
        _filter = RangeFilter(mock_data)
        agg = _filter.get_es_agg()
        name = list(agg['aggs'].keys())[0]
        assert agg == {
            'size': 0,
            'aggs': {
                name: {
                    'stats': {
                        'field': 'my_attr'
                    }
                }
            }
        }

    def test_get_es_query(self, mock_query_data):
        _filter = RangeFilter(mock_query_data)
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'filter': [
                        {
                            'range': {
                                'my_attr': {
                                    'gte': 1,
                                    'lte': 100
                                }
                            }
                        }
                    ]
                }
            }
        }

    def test_add_to_empty_query(self, mock_query_data):
        _filter = RangeFilter(mock_query_data)
        query = {}
        query = _filter.add_to_query(query)
        assert query == {
            'query': {'bool': {'filter': [{'range': {'my_attr': {'gte': 1, 'lte': 100}}}]}}}  # noqa

    def test_add_to_existing_range_query(self, mock_query_data):
        _filter = RangeFilter(mock_query_data)
        query = {'query': {'bool': {'filter': [{'range': {'foo': {'gte': 1, 'lte': 5}}}]}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'range': {'foo': {'gte': 1, 'lte': 5}}},
                                                       {'range': {'my_attr': {'gte': 1,
                                                                              'lte': 100}}}]}}}  # noqa

    def test_add_to_existing_range_query_same_attr(self, mock_query_data):
        _filter = RangeFilter(mock_query_data)
        query = {'query': {'bool': {'filter': [{'range': {'my_attr': {'gte': 1, 'lte': 5}}}]}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'range': {'my_attr': {'gte': 1, 'lte': 5}}},
                                                       {'range': {'my_attr': {'gte': 1,
                                                                              'lte': 100}}}]}}}  # noqa

    def test_add_to_other_query(self, mock_query_data):
        _filter = RangeFilter(mock_query_data)
        query = {'query': {'bool': {'filter': [{'terms': ['foo', 'bar']}]}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'terms': ['foo', 'bar']},
                                                       {'range': {'my_attr': {'gte': 1,
                                                                              'lte': 100}}}]}}}  # noqa


class TestFacetFilter(FilterBaseTestCase):

    Filter = FacetFilter

    @pytest.fixture
    def mock_data(self):
        return {'type': FacetFilter.type,
                'attribute': 'my_attr'}

    @pytest.fixture
    def mock_query_data(self, mock_data):
        data = mock_data
        data['values'] = {'facets': ['value1', 'value2']}
        return data

    def test_get_es_agg(self, mock_data):
        _filter = FacetFilter(mock_data)
        agg = _filter.get_es_agg()
        name = list(agg['aggs'].keys())[0]
        assert agg == {
            'size': 0,
            'aggs': {
                name: {
                    'terms': {
                        'field': 'my_attr',
                        'size': 1000
                    }
                }
            }
        }

    def test_get_es_agg_with_order(self, mock_data):
        mock_data['order'] = 'desc'
        _filter = FacetFilter(mock_data)
        agg = _filter.get_es_agg()
        name = list(agg['aggs'].keys())[0]
        assert agg == {
            'size': 0,
            'aggs': {
                name: {
                    'terms': {
                        'field': 'my_attr',
                        'size': 1000,
                        'order': {
                            '_count': 'desc'
                        }
                    }
                }
            }
        }

    def test_get_es_agg_with_count(self, mock_data):
        mock_data['minimumCount'] = 10
        _filter = FacetFilter(mock_data)
        agg = _filter.get_es_agg()
        name = list(agg['aggs'].keys())[0]
        assert agg == {
            'size': 0,
            'aggs': {
                name: {
                    'terms': {
                        'field': 'my_attr',
                        'size': 1000,
                        'min_doc_count': 10
                    }
                }
            }
        }

    def test_get_es_query(self, mock_query_data):
        _filter = FacetFilter(mock_query_data)
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'filter': [
                        {'terms': {'my_attr': ['value1', 'value2']}}
                    ]
                }
            }
        }

    def test_add_to_empty_query(self, mock_query_data):
        _filter = FacetFilter(mock_query_data)
        query = {}
        query = _filter.add_to_query(query)
        assert query == {
            'query': {'bool': {'filter': [{'terms': {'my_attr': ['value1', 'value2']}}]}}}  # noqa

    def test_add_to_existing_facet_query(self, mock_query_data):
        _filter = FacetFilter(mock_query_data)
        query = {'query': {'bool': {'filter': [{'terms': {'my_attr': ['foo', 'bar']}}]}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'terms': {'my_attr': ['foo', 'bar']}},
                                                       {'terms': {'my_attr': ['value1',
                                                                              'value2']}}]}}}  # noqa

    def test_add_to_other_query(self, mock_query_data):
        _filter = FacetFilter(mock_query_data)
        query = {'query': {'bool': {'filter': [{'range': {'my_attr': {'gte': 1, 'lte': 100}}}]}}}
        query = _filter.add_to_query(query)
        assert query == {
            'query': {'bool': {'filter': [{'range': {'my_attr': {'gte': 1, 'lte': 100}}},  # noqa
                                          {'terms': {'my_attr': ['value1', 'value2']}}]}}}  # noqa


class TestLabelConfidenceFilter(FilterBaseTestCase):

    Filter = LabelConfidenceFilter

    @pytest.fixture
    def mock_data(self):
        return {'type': LabelConfidenceFilter.type,
                'attribute': 'analysis.zvi-label-detection'}

    @pytest.fixture
    def mock_query_data(self, mock_data):
        data = mock_data
        data['values'] = {'labels': ['value1', 'value2'],
                          'min': .5, 'max': .8}
        return data

    def test_get_es_agg_prediction(self, mock_data):
        _filter = LabelConfidenceFilter(mock_data)
        _filter._field_type = 'prediction'
        agg = _filter.get_es_agg()
        name = list(agg['aggs'].keys())[0]
        assert agg == {
            'size': 0,
            'aggs': {
                name: {
                    'terms': {
                        'field': 'analysis.zvi-label-detection.predictions.label',
                        'size': 1000
                    }
                }
            }
        }

    def test_get_es_agg_single_label(self, mock_data):
        _filter = LabelConfidenceFilter(mock_data)
        _filter._field_type = 'single_label'
        agg = _filter.get_es_agg()
        name = list(agg['aggs'].keys())[0]
        assert agg == {
            'size': 0,
            'aggs': {
                name: {
                    'terms': {
                        'field': 'analysis.zvi-label-detection.label',
                        'size': 1000
                    }
                }
            }
        }

    def test_get_es_query_prediction(self, mock_query_data):
        _filter = LabelConfidenceFilter(mock_query_data)
        _filter._field_type = 'prediction'
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'filter': [
                        {
                            'terms': {
                                'analysis.zvi-label-detection.predictions.label': [
                                    'value1',
                                    'value2'
                                ]}}],
                    'must': [
                        {
                            'nested': {
                                'path': 'analysis.zvi-label-detection.predictions',
                                'query': {
                                    'function_score': {
                                        'boost_mode': 'sum',
                                        'field_value_factor': {
                                            'field': 'analysis.zvi-label-detection.predictions.score',
                                            'missing': 0
                                        },
                                        'query': {
                                            'bool': {
                                                'filter': [
                                                    {
                                                        'terms': {
                                                            'analysis.zvi-label-detection.predictions.label': [
                                                                'value1',
                                                                'value2'
                                                            ]}},
                                                    {
                                                        'range': {
                                                            'analysis.zvi-label-detection.predictions.score': {
                                                                'gte': 0.5,
                                                                'lte': 0.8
                                                            }}}]}}}}}}]}}}

    def test_get_es_query_single_label(self, mock_query_data):
        _filter = LabelConfidenceFilter(mock_query_data)
        _filter._field_type = 'single_label'
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'filter': [
                        {'terms': {'analysis.zvi-label-detection.label': ['value1',
                                                                          'value2']}},
                        {'range': {'analysis.zvi-label-detection.score': {'from': 0.5,
                                                                          'to': 0.8}}}]}}}

    def test_add_to_query(self, mock_query_data):
        _filter = LabelConfidenceFilter(mock_query_data)
        _filter._field_type = 'prediction'
        query = {
            'query': {
                'bool': {
                    'filter': [
                        {'terms': {'my_attr': ['value1', 'value2']}}
                    ]
                }
            }
        }
        new_query = _filter.add_to_query(query)
        assert new_query == {'query': {'bool': {
            'filter': [{'terms': {'my_attr': ['value1', 'value2']}}, {
                'terms': {'analysis.zvi-label-detection.predictions.label': ['value1', 'value2']}}],
            'must': [{'nested': {'path': 'analysis.zvi-label-detection.predictions', 'query': {
                'function_score': {'boost_mode': 'sum', 'field_value_factor': {
                    'field': 'analysis.zvi-label-detection.predictions.score', 'missing': 0},
                                   'query': {'bool': {'filter': [{'terms': {
                                       'analysis.zvi-label-detection.predictions.label': ['value1',
                                                                                          'value2']}},
                                                                 {'range': {
                                                                     'analysis.zvi-label-detection.predictions.score': {
                                                                         'gte': 0.5,
                                                                         'lte': 0.8}}}]}}}}}}]}}}

    def test_add_to_label_conf_query(self, mock_query_data):
        _filter = LabelConfidenceFilter(mock_query_data)
        _filter._field_type = 'prediction'
        _filter2 = LabelConfidenceFilter({
            'type': 'labelConfidence',
            'attribute': 'analysis.zvi-object-detection',
            'values': {
                'labels': ['dog', 'cat'],
                'min': 0.2,
                'max': 0.7
            }
        })
        _filter2._field_type = 'prediction'
        query = _filter.get_es_query()
        query = _filter2.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [
            {'terms': {'analysis.zvi-label-detection.predictions.label': ['value1', 'value2']}},
            {'terms': {'analysis.zvi-object-detection.predictions.label': ['dog', 'cat']}}],
                                            'must': [{'nested': {
                                                'path': 'analysis.zvi-label-detection.predictions',
                                                'query': {'function_score': {'boost_mode': 'sum',
                                                                             'field_value_factor': {
                                                                                 'field': 'analysis.zvi-label-detection.predictions.score',
                                                                                 'missing': 0},
                                                                             'query': {'bool': {
                                                                                 'filter': [{
                                                                                                'terms': {
                                                                                                    'analysis.zvi-label-detection.predictions.label': [
                                                                                                        'value1',
                                                                                                        'value2']}},
                                                                                            {
                                                                                                'range': {
                                                                                                    'analysis.zvi-label-detection.predictions.score': {
                                                                                                        'gte': 0.5,
                                                                                                        'lte': 0.8}}}]}}}}}},
                                                     {'nested': {
                                                         'path': 'analysis.zvi-object-detection.predictions',
                                                         'query': {
                                                             'function_score': {'boost_mode': 'sum',
                                                                                'field_value_factor': {
                                                                                    'field': 'analysis.zvi-object-detection.predictions.score',
                                                                                    'missing': 0},
                                                                                'query': {'bool': {
                                                                                    'filter': [{
                                                                                                   'terms': {
                                                                                                       'analysis.zvi-object-detection.predictions.label': [
                                                                                                           'dog',
                                                                                                           'cat']}},
                                                                                               {
                                                                                                   'range': {
                                                                                                       'analysis.zvi-object-detection.predictions.score': {
                                                                                                           'gte': 0.2,
                                                                                                           'lte': 0.7}}}]}}}}}}]}}}

    def test_get_clip_query_non_video(self, mock_query_data):
        _filter = LabelConfidenceFilter(mock_query_data)
        _filter._field_type = 'prediction'
        query = _filter.get_clip_query()
        assert query == {}

    def test_get_clip_query_non_video(self, mock_query_data):
        mock_query_data['attribute'] = 'analysis.zvi-video-label-detection'
        _filter = LabelConfidenceFilter(mock_query_data)
        _filter._field_type = 'prediction'
        query = _filter.get_clip_query()
        assert query == {
            'query': {
                'bool': {
                    'filter': [
                        {
                            'terms': {
                                'clip.track': ['value1', 'value2']
                            }
                        },
                        {
                            'term': {
                                'clip.timeline': 'zvi-video-label-detection'
                            }
                        },
                        {
                            'range': {
                                'clip.score': {'from': 0.5, 'to': 0.8}
                            }
                        }
                    ]
                }
            }
        }


class TestTextContentFilter(FilterBaseTestCase):

    Filter = TextContentFilter

    @pytest.fixture
    def mock_data(self):
        return {'type': TextContentFilter.type,
                'attribute': 'analysis.zvi-text-detection'}

    @pytest.fixture
    def mock_query_data(self, mock_data):
        data = mock_data
        data['values'] = {'query': 'possibility'}
        return data

    def test_is_valid_no_attr(self):
        _filter = self.Filter({'type': self.Filter.type,
                               'values': {'query': 'test'}})
        assert _filter.is_valid()
        assert _filter.is_valid(query=True)

    def test_is_valid_non_analysis_attr(self):
        _filter = self.Filter({'type': self.Filter.type,
                               'attribute': 'one.two.three',
                               'values': {'query': 'test'}})
        assert _filter.is_valid()
        assert _filter.is_valid(query=True)

    # No ES Agg to check

    def test_get_es_query(self, mock_query_data):
        _filter = self.Filter(mock_query_data)
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'must': [
                        {'simple_query_string': {
                            'query': 'possibility',
                            'fields': ['analysis.zvi-text-detection.content']}}
                        ]}}}

    def test_get_es_query_no_attr(self):
        _filter = self.Filter({'type': self.Filter.type,
                               'values': {'query': 'test'}})
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'must': [
                        {'simple_query_string': {'query': 'test'}}
                    ]}}}

    def test_get_es_query_non_analysis_attr(self):
        _filter = self.Filter({'type': self.Filter.type,
                               'attribute': 'one.two',
                               'values': {'query': 'test'}})
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'must': [
                        {'simple_query_string': {
                            'query': 'test',
                            'fields': ['one.two']
                        }}
                    ]}}}

    def test_get_clip_query(self):
        _filter = self.Filter({'type': self.Filter.type,
                               'values': {'query': 'Jack Hodgens'}})
        query = _filter.get_clip_query()
        assert query == {
            'query': {
                'bool': {
                    'must': [
                        {'simple_query_string': {
                            'query': 'Jack Hodgens',
                            'fields': ['clip.content']
                        }}
                    ]}}}


class TestSimilarityFilter(FilterBaseTestCase):

    Filter = SimilarityFilter

    @pytest.fixture
    def mock_data(self):
        return {'type': SimilarityFilter.type,
                'attribute': 'analysis.zvi-image-similarity'}

    @pytest.fixture
    def mock_query_data(self, mock_data):
        data = mock_data
        data['values'] = {'ids': ['GxjzTXpJvTdGf14dJ-IfejTgUJE0FlZB',
                                  'zQpe80PC2dcqMCGmXIGpSKHMShnckiYZ']}
        return data

    @pytest.fixture
    def zmlp_app(self):
        return BoonApp({'accessKey': 'access', 'secretKey': 'secret'})

    @pytest.fixture
    def mock_request(self, zmlp_app):
        return Mock(app=zmlp_app)

    @staticmethod
    def mock_post_return(*args, **kwargs):
        return {'took': 4, 'timed_out': False, '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0}, 'hits': {'total': {'value': 2, 'relation': 'eq'}, 'max_score': 1.0, 'hits': [{'_index': '441topfgan3xuxg2', '_type': '_doc', '_id': 'GxjzTXpJvTdGf14dJ-IfejTgUJE0FlZB', '_score': 1.0, '_source': {'system': {'jobId': 'ac349441-0b36-1ad0-b0b3-b25968f5b7ae', 'dataSourceId': '4facb80c-0981-11e0-b755-363f4581d984', 'timeCreated': '2020-06-10T20:16:56.100034Z', 'state': 'Analyzed', 'projectId': '564de5f9-4027-4314-997f-a23d4e879e81', 'taskId': 'ac349442-0b36-1ad0-b0b3-b25968f5b7ae', 'timeModified': '2020-06-10T20:47:31.550642Z'}, 'source': {'path': 'gs://zorroa-dev-data/zifar/truck/8.https%3A%2F%2Fblogs-images.forbes.com%2Fsebastianblanco%2Ffiles%2F2019%2F04%2Ftoyota-hydrogen-semi-1200x679.jpg', 'extension': 'jpg', 'filename': '8.https%3A%2F%2Fblogs-images.forbes.com%2Fsebastianblanco%2Ffiles%2F2019%2F04%2Ftoyota-hydrogen-semi-1200x679.jpg', 'mimetype': 'image/jpeg', 'filesize': 86834, 'checksum': 1789416729}, 'metrics': {'pipeline': [{'processor': 'boonai_core.core.PreCacheSourceFileProcessor', 'module': 'standard', 'checksum': 2178814325, 'executionTime': 0.26, 'executionDate': '2020-06-10T20:47:04.684568'}, {'processor': 'boonai_core.core.FileImportProcessor', 'module': 'standard', 'checksum': 117837444, 'executionTime': 0.26, 'executionDate': '2020-06-10T20:47:05.483410'}, {'processor': 'boonai_core.proxy.ImageProxyProcessor', 'module': 'standard', 'checksum': 457707303, 'executionTime': 3.36, 'executionDate': '2020-06-10T20:47:09.331851'}, {'processor': 'boonai_core.proxy.VideoProxyProcessor', 'module': 'standard', 'checksum': 482873147, 'executionTime': 0}, {'processor': 'boonai_analysis.zvi.ZviSimilarityProcessor', 'module': 'standard', 'checksum': 1879445844, 'executionTime': 0.7, 'executionDate': '2020-06-10T20:47:24.832412'}]}, 'media': {'width': 960, 'height': 543, 'aspect': 1.77, 'orientation': 'landscape', 'type': 'image', 'length': 1}, 'clip': {'type': 'page', 'start': 1.0, 'stop': 1.0, 'length': 1.0, 'pile': 'vQlzx9MA-tjYb6TWOY4_hMJMrR4', 'sourceAssetId': 'GxjzTXpJvTdGf14dJ-IfejTgUJE0FlZB'}, 'files': [{'id': 'assets/GxjzTXpJvTdGf14dJ-IfejTgUJE0FlZB/proxy/image_960x543.jpg', 'name': 'image_960x543.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 252738, 'attrs': {'width': 960, 'height': 543}}, {'id': 'assets/GxjzTXpJvTdGf14dJ-IfejTgUJE0FlZB/proxy/image_512x289.jpg', 'name': 'image_512x289.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 117008, 'attrs': {'width': 512, 'height': 289}}, {'id': 'assets/GxjzTXpJvTdGf14dJ-IfejTgUJE0FlZB/web-proxy/web-proxy.jpg', 'name': 'web-proxy.jpg', 'category': 'web-proxy', 'mimetype': 'image/jpeg', 'size': 92565, 'attrs': {'width': 960, 'height': 543}}], 'analysis': {'zvi-image-similarity': {'type': 'similarity', 'simhash': 'GHPCCPPPBONADNPPIPKGNACPPPPEOBPPCPFOBJFBPHPPPPIPDEDHPPIPLPPFLBFPIBPIPLPPBPPNJPPGIMIBPHDPOLHNPPPPCDNHAPPAIJBPECBHPPOJFAPIPGPNBPGDNOFPPPAOJMPPIFDICPPPBPPPNBFJEHFNFPPBPGAPBPEAPOCPPKGPPPJPECDPPIECKPPIAPGBNPPJKPNPHHMOPNNPHAMJPCPKPPDMPNJMBPDDDOPGPPFBPPPPPPNPGPDJHCKPLPPPPPBBPFKINADPPJNCHEHKAPPPPPCOPPPPPGJIPAPJPMFGPPMEPOEMKPPMPHPPLLIPPPCLONCLPJPPPKPPAPNJPPFPAPPPPPPPCNLPCOGPAFPPFPGCEPPEPOJPPPFPKEEPFGPDPPPPPAAFOPPEPIPBNCPIPPPPLEPPCPLJMAPBKEFPHPJPFMPMBCLPMBBBFPPPJPPPKPMBPPICPDDPHPHLPPPBEPIHBPPKGPJPLPPPPGKDFDPPPKFPFCIPMPCPGAEOPNCPPPMJLPKFIHHOLPPJICGPPNPAIFFPCOBLPPNPPPPCPPPPPGPPPNHPOPLPPPPKMPAHPPGFPJHPBPPAPGDKPPOOFDKGPPJGHPDPNPPAHFPPHBAEKPKPPEKPPMPJPPPLCHEABPCPPPPPPPHPPPIKPJAPBOMCPPPAKLPNNGOGMEADPIGMCPPDCPDDKNAJGLPMPPPPEGGGDCFPHBPKNPPPHHIINPPHIFIOPPPPFAPAGLPBPDPAPPPDPOBMMPJGCFOFLFBHEHBJPEFAPFPPMPFPPPIPPPPDECHBGDMEDAKIPMKJOPIPOPKFPJFOGPLKKIPPPPPBPPJPJGPPKOFPGCEPMJOCCPAPMAPPCPLNPIFGPKCIBPGMPPAHPPAGGLPPJPLPGPECIAHAPECPAPPFEEGPPAPKDBPDPLPGAPHPPPNPPEPCPPIMAEBPPOPKPPHAPPBMAPKPHPPECGPGKPJEPOFHPDPPCGPPPMAJPPPBABCPPPOLIPKPJPFIAOPKPPAAPPAPPDIPJGDPPDCHAPAFAPKDDDPPBPHPKOPJPMPPPPPPPPBKPNIPPDPILPPPDKMGPPPFDFCDDOLNBPPCFFPHFIPPDCPNPKPPPFLNBOPPCPPPEBBCPBOPBDPPMPPEPPPPENBPLGOPKPOPMPAPIPPPDJPPPCJPEPFEPAPNPPPPPPPENBFANEPPPHPPPPCPPPLADKLCAKPPDIFHNPFDBIJPAPPPPPPIPNGDIPBPJOAPPPPEPBPNNPPPIBDJPJFDHPHJAEJPBBDPIPPPLPKNADKEPGHEIPKHPOPPPAPBPIPAPPPDLPGPPEPPNPPCCPEBDADPHDBPBBCPCPGOPPDFBFLPPAKBAPPPBPPPEOHLOJBPOBBIMEJPMKODDCGBPHPPPPPPDPPKPPPPGCDPPPANMDCPNPDPBPPPDKPPHGDIJPPCPAGDCHPPPIIPBPIKPNAJCEMAPJPMKFLNPPFEBPGPHIFPAPNLPKLODBPPPHPPPPLPGHPNODIFAFPBPPCPPFGPPBPPDPPAPIBPGOPDNPPFPPPLAPICPNPFGPDNPPCPPJPGPPNPCOPGPBMLPDNPPCPPPPPDPKPPLIDMPCPLPFEIPPPBHGLPMHPPPPPJEKPPBBBHPGAALNPGPPDHNBPCBPOFPCPPKLPPOPHPBBHDGDPECCPGFPPPBGPMFPPNPPHEKPPPELPPGPPPJFPGPMBPPHFNNPPPPKEPJAECPPPFDPPIIFDLMPEOLNDCAGLPAPPEIPPPPIPEAEPPPNPONPMFPHPPDFPIHFKMPPCPICPECBEDPPPPMDPPPPAPOGPPIGBPKPLPKFPEBAFPAEPPPEPPPLPPPDPGOFPBJAPOPPDPPFCPOPAFBJGGLPMPPJBJDIPEEHJIFBDDMLJLCPAPMABPDPBPHOPFCPCPOIHEPPIBPMIPEPIBACBFOAPPBHCEGPAKDPFPEAPKPPBPIMPPPJPPPJPPIPGHMIFFPGPPPHPPOCAKAMGPPFAJPAIPPGPPGEPHPLPPPDADPCNPPPPNPGCAPAPP'}}}}, {'_index': '441topfgan3xuxg2', '_type': '_doc', '_id': 'zQpe80PC2dcqMCGmXIGpSKHMShnckiYZ', '_score': 1.0, '_source': {'system': {'jobId': 'ac349441-0b36-1ad0-b0b3-b25968f5b7ae', 'dataSourceId': '4facb80c-0981-11e0-b755-363f4581d984', 'timeCreated': '2020-06-10T20:16:56.100450Z', 'state': 'Analyzed', 'projectId': '564de5f9-4027-4314-997f-a23d4e879e81', 'taskId': 'ac349442-0b36-1ad0-b0b3-b25968f5b7ae', 'timeModified': '2020-06-10T20:47:31.550697Z'}, 'source': {'path': 'gs://zorroa-dev-data/zifar/truck/84.Bollinger-B2-3_4-Front-1280x720.jpg', 'extension': 'jpg', 'filename': '84.Bollinger-B2-3_4-Front-1280x720.jpg', 'mimetype': 'image/jpeg', 'filesize': 64138, 'checksum': 2499279810}, 'metrics': {'pipeline': [{'processor': 'boonai_core.core.PreCacheSourceFileProcessor', 'module': 'standard', 'checksum': 2178814325, 'executionTime': 0.23, 'executionDate': '2020-06-10T20:47:04.653384'}, {'processor': 'boonai_core.core.FileImportProcessor', 'module': 'standard', 'checksum': 117837444, 'executionTime': 0.28, 'executionDate': '2020-06-10T20:47:05.507262'}, {'processor': 'boonai_core.proxy.ImageProxyProcessor', 'module': 'standard', 'checksum': 457707303, 'executionTime': 3.52, 'executionDate': '2020-06-10T20:47:09.500716'}, {'processor': 'boonai_core.proxy.VideoProxyProcessor', 'module': 'standard', 'checksum': 482873147, 'executionTime': 0}, {'processor': 'boonai_analysis.zvi.ZviSimilarityProcessor', 'module': 'standard', 'checksum': 1879445844, 'executionTime': 0.73, 'executionDate': '2020-06-10T20:47:25.565146'}]}, 'media': {'width': 1280, 'height': 720, 'aspect': 1.78, 'orientation': 'landscape', 'type': 'image', 'length': 1}, 'clip': {'type': 'page', 'start': 1.0, 'stop': 1.0, 'length': 1.0, 'pile': 'iWEg85I1kQ003hBDpSVU7nLbWj0', 'sourceAssetId': 'zQpe80PC2dcqMCGmXIGpSKHMShnckiYZ'}, 'files': [{'id': 'assets/zQpe80PC2dcqMCGmXIGpSKHMShnckiYZ/proxy/image_1024x576.jpg', 'name': 'image_1024x576.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 223590, 'attrs': {'width': 1024, 'height': 576}}, {'id': 'assets/zQpe80PC2dcqMCGmXIGpSKHMShnckiYZ/proxy/image_512x288.jpg', 'name': 'image_512x288.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 71762, 'attrs': {'width': 512, 'height': 288}}, {'id': 'assets/zQpe80PC2dcqMCGmXIGpSKHMShnckiYZ/web-proxy/web-proxy.jpg', 'name': 'web-proxy.jpg', 'category': 'web-proxy', 'mimetype': 'image/jpeg', 'size': 66190, 'attrs': {'width': 1024, 'height': 576}}], 'analysis': {'zvi-image-similarity': {'type': 'similarity', 'simhash': 'FPPDBHPPCPAAPCCJBBFDPAGIPPPHCPPPCOBLBBPBNNPPJPJPALLIGBPPMAEHAJBFJPOPPPAPABPEPPPJJPFHGGCPPDKECDPPCOIJPIHCIOEPJPCPBFFIAGPJOFGMCPDAGJMPKFHJDPPPPEDBPPPCKOPPOBALDAPOJPPPPEBPANPAPPGPPPPIPPNPEIPPPPHIDPHHBOPDCKJFPPPKPNJHPJGPFPAGKIKIKPODFGAPGPHPPAJPJPGDCAPPIOCPPPJJEEPPPDPFPKJKJKPDPCEDPJFJPLPBPPGPPNPGLPPMCNCEPDPFFHPAOFBEMPPCLPPIMPMDNNMPOBPCPIMJKDKPPPPPCPPPPPGPPPPPDDCODIPPAAKLEPPPAPGFOPBCMPKEFKBPPBJJFPPPGPIPPBBAPDPBJFPBBPPFDFNDIAPPBGDPJAAPNINPFGPDHPPGPAPCLPAFLPPPPPPPPBPCPFCPFKJNPMBFPPJKJLECLNPPEPEPPPNMPLPCEBPFBPAOELLHICPPMAIHEFNLGPAPPPPFPAPODPGLAFPPPPLPPBDPPCFPPKKNFNANCPEPPJPPPCBLPEPPPDPEMOOGPKPBPPCPMJEOJPDJPMPIEJEIMPBGADPOPPCPBPCPDPONIGPOKGMPJPPBFPEFOAPEFPIHPODPPPGBPAFPPFJCBPAPPKPAAPPDLLHPFEBLPOIMPPDPCPPDAPCPFEPPBOPNGPPHGPPPPPJPNHPIBEPPPPEPAPBDPPPBPIPPGAOKKALEPFMBMPJBPPAPMKMPEPPFPKNJPMPBPEOEPLPPLMDAPPPLAPNPDAPCHPHHPAPPFPPMFPKCBPHBECPDPNPPAPCGPDDPPDPPEPAPNPCNCPFBCKFMPJPFGONPBILPFPFPKGKPPPJIPFBFKEBDCGDIPEBPEPHICPFPPPIHMBOPHFGPPEPANPPPMCPKPPPPPBCPPJPMAFAHBPLEPBIAFPPPABPEPPGAJPPPCPJAFMPDHCPBAPPPPCBCPPPDPHJNAPAPCFMPPPIPEPDPBGOONBPMPMONPONPPPMPFPNPEFCGJBPADHCMPEKPPELKHOPPPDHMFHMPGPLAIPBEDPPPPKBPPFINPFPMPPPPPBPODKPJDBPPGPBKEFLHPPPPCLPBPIPOLPPPAEPAOKPEHOPBPPIDPHEPPPAMPPBPPEHKFGPMPBPPFPHPPPEAPMPLPPNIPPBGBAIPMKKPPLDGPPBAPEPCGPPPPKAMAACFDPENHJPMPPHPAPPMPKABDPBBPPHLJJPAFDEMBLFPPAPGPPPPNAOPBBBPALNPPPPMAIKPOACEGPIPPDMGDAPBLCPPDDPEPPPBGPGAPPMFPFBPPCBKPNBPFCAOBBNPPNEDCPKPOINNBPPPEADPBPPDIBDPPCACBPJPHIBDHICJPHPPCEKPLGPBGKPPJPIPPACPACPNPPPPPPPHKFPJEKPKFFNPPBOFNKPPPDIBPPDPPBEHFCPPPDPIPDMPPGLAPBPHEPBPHCCDHOBEACPBPIPGPPPHBPPCGMPPCCEMPPHFPEDPDHPPGLPAJFNDKPPPPPPNDNOEDPDPDMPIBPOKDPPIPPLCKPMGBKPPPGPPPEMPIPPPDINCPCEFPCJPPGPJPCNAAIPGPKDPJACPHPEAEDPPPPBJJPEIJDFIPLPPCABGPPEDDPFDFBPPPPEDPPLJGPPCPPCPFKPPADPFPPMPPPGNIPBPPGPDPPPPDKIFPPCPGHFEBIGGPPPJCPEDPEPPMPPIPPPGHPPDKHPPDPOPPGPCPPGPAMOHFKFMPOPPJOLFPFFPCJIMPCPPPEAPCHPHMCPDOCDPPPOLJPPPLPNNDGFGCHJEPCPIAPCJNPPPFPCPPPPPPDPDADPJKDCGPPHPPEFPJHCDEPCGAJHGPPOKLPPLPPEGPKPJJPEPGPKIFDJOCPJNPPNGBPBHMBIPPGPPBPDEJBIPJGNCPOPJAOJGPKGJELHCHBFFLPNIBPAINPPOGNPCLCKEPFPPPPPPPPGPBCCPGPDPDBAIEHBEAOCPPCENEADIBFAGMIPPMFPMAPELLBOP'}}}}]}}  # noqa

    def test_get_es_query(self, mock_query_data, monkeypatch, mock_request):
        monkeypatch.setattr(BoonClient, 'post', self.mock_post_return)
        _filter = self.Filter(mock_query_data, mock_request)
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'must': [
                        {'script_score': {
                            'query': {'match_all': {}},
                            'script': {
                                'source': 'similarity',
                                'lang': 'zorroa-similarity',
                                'params': {
                                    'minScore': 0.75,
                                    'field': 'analysis.zvi-image-similarity.simhash',
                                    'hashes': ['GHPCCPPPBONADNPPIPKGNACPPPPEOBPPCPFOBJFBPHPPPPIPDEDHPPIPLPPFLBFPIBPIPLPPBPPNJPPGIMIBPHDPOLHNPPPPCDNHAPPAIJBPECBHPPOJFAPIPGPNBPGDNOFPPPAOJMPPIFDICPPPBPPPNBFJEHFNFPPBPGAPBPEAPOCPPKGPPPJPECDPPIECKPPIAPGBNPPJKPNPHHMOPNNPHAMJPCPKPPDMPNJMBPDDDOPGPPFBPPPPPPNPGPDJHCKPLPPPPPBBPFKINADPPJNCHEHKAPPPPPCOPPPPPGJIPAPJPMFGPPMEPOEMKPPMPHPPLLIPPPCLONCLPJPPPKPPAPNJPPFPAPPPPPPPCNLPCOGPAFPPFPGCEPPEPOJPPPFPKEEPFGPDPPPPPAAFOPPEPIPBNCPIPPPPLEPPCPLJMAPBKEFPHPJPFMPMBCLPMBBBFPPPJPPPKPMBPPICPDDPHPHLPPPBEPIHBPPKGPJPLPPPPGKDFDPPPKFPFCIPMPCPGAEOPNCPPPMJLPKFIHHOLPPJICGPPNPAIFFPCOBLPPNPPPPCPPPPPGPPPNHPOPLPPPPKMPAHPPGFPJHPBPPAPGDKPPOOFDKGPPJGHPDPNPPAHFPPHBAEKPKPPEKPPMPJPPPLCHEABPCPPPPPPPHPPPIKPJAPBOMCPPPAKLPNNGOGMEADPIGMCPPDCPDDKNAJGLPMPPPPEGGGDCFPHBPKNPPPHHIINPPHIFIOPPPPFAPAGLPBPDPAPPPDPOBMMPJGCFOFLFBHEHBJPEFAPFPPMPFPPPIPPPPDECHBGDMEDAKIPMKJOPIPOPKFPJFOGPLKKIPPPPPBPPJPJGPPKOFPGCEPMJOCCPAPMAPPCPLNPIFGPKCIBPGMPPAHPPAGGLPPJPLPGPECIAHAPECPAPPFEEGPPAPKDBPDPLPGAPHPPPNPPEPCPPIMAEBPPOPKPPHAPPBMAPKPHPPECGPGKPJEPOFHPDPPCGPPPMAJPPPBABCPPPOLIPKPJPFIAOPKPPAAPPAPPDIPJGDPPDCHAPAFAPKDDDPPBPHPKOPJPMPPPPPPPPBKPNIPPDPILPPPDKMGPPPFDFCDDOLNBPPCFFPHFIPPDCPNPKPPPFLNBOPPCPPPEBBCPBOPBDPPMPPEPPPPENBPLGOPKPOPMPAPIPPPDJPPPCJPEPFEPAPNPPPPPPPENBFANEPPPHPPPPCPPPLADKLCAKPPDIFHNPFDBIJPAPPPPPPIPNGDIPBPJOAPPPPEPBPNNPPPIBDJPJFDHPHJAEJPBBDPIPPPLPKNADKEPGHEIPKHPOPPPAPBPIPAPPPDLPGPPEPPNPPCCPEBDADPHDBPBBCPCPGOPPDFBFLPPAKBAPPPBPPPEOHLOJBPOBBIMEJPMKODDCGBPHPPPPPPDPPKPPPPGCDPPPANMDCPNPDPBPPPDKPPHGDIJPPCPAGDCHPPPIIPBPIKPNAJCEMAPJPMKFLNPPFEBPGPHIFPAPNLPKLODBPPPHPPPPLPGHPNODIFAFPBPPCPPFGPPBPPDPPAPIBPGOPDNPPFPPPLAPICPNPFGPDNPPCPPJPGPPNPCOPGPBMLPDNPPCPPPPPDPKPPLIDMPCPLPFEIPPPBHGLPMHPPPPPJEKPPBBBHPGAALNPGPPDHNBPCBPOFPCPPKLPPOPHPBBHDGDPECCPGFPPPBGPMFPPNPPHEKPPPELPPGPPPJFPGPMBPPHFNNPPPPKEPJAECPPPFDPPIIFDLMPEOLNDCAGLPAPPEIPPPPIPEAEPPPNPONPMFPHPPDFPIHFKMPPCPICPECBEDPPPPMDPPPPAPOGPPIGBPKPLPKFPEBAFPAEPPPEPPPLPPPDPGOFPBJAPOPPDPPFCPOPAFBJGGLPMPPJBJDIPEEHJIFBDDMLJLCPAPMABPDPBPHOPFCPCPOIHEPPIBPMIPEPIBACBFOAPPBHCEGPAKDPFPEAPKPPBPIMPPPJPPPJPPIPGHMIFFPGPPPHPPOCAKAMGPPFAJPAIPPGPPGEPHPLPPPDADPCNPPPPNPGCAPAPP',  # noqa
                                               'FPPDBHPPCPAAPCCJBBFDPAGIPPPHCPPPCOBLBBPBNNPPJPJPALLIGBPPMAEHAJBFJPOPPPAPABPEPPPJJPFHGGCPPDKECDPPCOIJPIHCIOEPJPCPBFFIAGPJOFGMCPDAGJMPKFHJDPPPPEDBPPPCKOPPOBALDAPOJPPPPEBPANPAPPGPPPPIPPNPEIPPPPHIDPHHBOPDCKJFPPPKPNJHPJGPFPAGKIKIKPODFGAPGPHPPAJPJPGDCAPPIOCPPPJJEEPPPDPFPKJKJKPDPCEDPJFJPLPBPPGPPNPGLPPMCNCEPDPFFHPAOFBEMPPCLPPIMPMDNNMPOBPCPIMJKDKPPPPPCPPPPPGPPPPPDDCODIPPAAKLEPPPAPGFOPBCMPKEFKBPPBJJFPPPGPIPPBBAPDPBJFPBBPPFDFNDIAPPBGDPJAAPNINPFGPDHPPGPAPCLPAFLPPPPPPPPBPCPFCPFKJNPMBFPPJKJLECLNPPEPEPPPNMPLPCEBPFBPAOELLHICPPMAIHEFNLGPAPPPPFPAPODPGLAFPPPPLPPBDPPCFPPKKNFNANCPEPPJPPPCBLPEPPPDPEMOOGPKPBPPCPMJEOJPDJPMPIEJEIMPBGADPOPPCPBPCPDPONIGPOKGMPJPPBFPEFOAPEFPIHPODPPPGBPAFPPFJCBPAPPKPAAPPDLLHPFEBLPOIMPPDPCPPDAPCPFEPPBOPNGPPHGPPPPPJPNHPIBEPPPPEPAPBDPPPBPIPPGAOKKALEPFMBMPJBPPAPMKMPEPPFPKNJPMPBPEOEPLPPLMDAPPPLAPNPDAPCHPHHPAPPFPPMFPKCBPHBECPDPNPPAPCGPDDPPDPPEPAPNPCNCPFBCKFMPJPFGONPBILPFPFPKGKPPPJIPFBFKEBDCGDIPEBPEPHICPFPPPIHMBOPHFGPPEPANPPPMCPKPPPPPBCPPJPMAFAHBPLEPBIAFPPPABPEPPGAJPPPCPJAFMPDHCPBAPPPPCBCPPPDPHJNAPAPCFMPPPIPEPDPBGOONBPMPMONPONPPPMPFPNPEFCGJBPADHCMPEKPPELKHOPPPDHMFHMPGPLAIPBEDPPPPKBPPFINPFPMPPPPPBPODKPJDBPPGPBKEFLHPPPPCLPBPIPOLPPPAEPAOKPEHOPBPPIDPHEPPPAMPPBPPEHKFGPMPBPPFPHPPPEAPMPLPPNIPPBGBAIPMKKPPLDGPPBAPEPCGPPPPKAMAACFDPENHJPMPPHPAPPMPKABDPBBPPHLJJPAFDEMBLFPPAPGPPPPNAOPBBBPALNPPPPMAIKPOACEGPIPPDMGDAPBLCPPDDPEPPPBGPGAPPMFPFBPPCBKPNBPFCAOBBNPPNEDCPKPOINNBPPPEADPBPPDIBDPPCACBPJPHIBDHICJPHPPCEKPLGPBGKPPJPIPPACPACPNPPPPPPPHKFPJEKPKFFNPPBOFNKPPPDIBPPDPPBEHFCPPPDPIPDMPPGLAPBPHEPBPHCCDHOBEACPBPIPGPPPHBPPCGMPPCCEMPPHFPEDPDHPPGLPAJFNDKPPPPPPNDNOEDPDPDMPIBPOKDPPIPPLCKPMGBKPPPGPPPEMPIPPPDINCPCEFPCJPPGPJPCNAAIPGPKDPJACPHPEAEDPPPPBJJPEIJDFIPLPPCABGPPEDDPFDFBPPPPEDPPLJGPPCPPCPFKPPADPFPPMPPPGNIPBPPGPDPPPPDKIFPPCPGHFEBIGGPPPJCPEDPEPPMPPIPPPGHPPDKHPPDPOPPGPCPPGPAMOHFKFMPOPPJOLFPFFPCJIMPCPPPEAPCHPHMCPDOCDPPPOLJPPPLPNNDGFGCHJEPCPIAPCJNPPPFPCPPPPPPDPDADPJKDCGPPHPPEFPJHCDEPCGAJHGPPOKLPPLPPEGPKPJJPEPGPKIFDJOCPJNPPNGBPBHMBIPPGPPBPDEJBIPJGNCPOPJAOJGPKGJELHCHBFFLPNIBPAINPPOGNPCLCKEPFPPPPPPPPGPBCCPGPDPDBAIEHBEAOCPPCENEADIBFAGMIPPMFPMAPELLBOP']}},  # noqa
                            'boost': 1.0,
                            'min_score': 0.75}}]}}}

    def test_get_es_query_no_app(self, mock_query_data, monkeypatch):
        _filter = self.Filter(mock_query_data)
        with pytest.raises(ImproperlyConfigured):
            _filter.get_es_query()

    def test_get_query_with_optionals(self, monkeypatch, mock_request):
        monkeypatch.setattr(BoonClient, 'post', self.mock_post_return)
        _filter = self.Filter({
            'type': SimilarityFilter.type,
            'attribute': 'analysis.zvi-image-similarity',
            'values': {
                'ids': ['GxjzTXpJvTdGf14dJ-IfejTgUJE0FlZB',
                        'zQpe80PC2dcqMCGmXIGpSKHMShnckiYZ'],
                'minScore': 0.50,
                'boost': 0.8
            }
        }, mock_request)
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'must': [
                        {'script_score': {
                            'query': {'match_all': {}},
                            'script': {
                                'source': 'similarity',
                                'lang': 'zorroa-similarity',
                                'params': {
                                    'minScore': 0.5,
                                    'field': 'analysis.zvi-image-similarity.simhash',
                                    'hashes': ['GHPCCPPPBONADNPPIPKGNACPPPPEOBPPCPFOBJFBPHPPPPIPDEDHPPIPLPPFLBFPIBPIPLPPBPPNJPPGIMIBPHDPOLHNPPPPCDNHAPPAIJBPECBHPPOJFAPIPGPNBPGDNOFPPPAOJMPPIFDICPPPBPPPNBFJEHFNFPPBPGAPBPEAPOCPPKGPPPJPECDPPIECKPPIAPGBNPPJKPNPHHMOPNNPHAMJPCPKPPDMPNJMBPDDDOPGPPFBPPPPPPNPGPDJHCKPLPPPPPBBPFKINADPPJNCHEHKAPPPPPCOPPPPPGJIPAPJPMFGPPMEPOEMKPPMPHPPLLIPPPCLONCLPJPPPKPPAPNJPPFPAPPPPPPPCNLPCOGPAFPPFPGCEPPEPOJPPPFPKEEPFGPDPPPPPAAFOPPEPIPBNCPIPPPPLEPPCPLJMAPBKEFPHPJPFMPMBCLPMBBBFPPPJPPPKPMBPPICPDDPHPHLPPPBEPIHBPPKGPJPLPPPPGKDFDPPPKFPFCIPMPCPGAEOPNCPPPMJLPKFIHHOLPPJICGPPNPAIFFPCOBLPPNPPPPCPPPPPGPPPNHPOPLPPPPKMPAHPPGFPJHPBPPAPGDKPPOOFDKGPPJGHPDPNPPAHFPPHBAEKPKPPEKPPMPJPPPLCHEABPCPPPPPPPHPPPIKPJAPBOMCPPPAKLPNNGOGMEADPIGMCPPDCPDDKNAJGLPMPPPPEGGGDCFPHBPKNPPPHHIINPPHIFIOPPPPFAPAGLPBPDPAPPPDPOBMMPJGCFOFLFBHEHBJPEFAPFPPMPFPPPIPPPPDECHBGDMEDAKIPMKJOPIPOPKFPJFOGPLKKIPPPPPBPPJPJGPPKOFPGCEPMJOCCPAPMAPPCPLNPIFGPKCIBPGMPPAHPPAGGLPPJPLPGPECIAHAPECPAPPFEEGPPAPKDBPDPLPGAPHPPPNPPEPCPPIMAEBPPOPKPPHAPPBMAPKPHPPECGPGKPJEPOFHPDPPCGPPPMAJPPPBABCPPPOLIPKPJPFIAOPKPPAAPPAPPDIPJGDPPDCHAPAFAPKDDDPPBPHPKOPJPMPPPPPPPPBKPNIPPDPILPPPDKMGPPPFDFCDDOLNBPPCFFPHFIPPDCPNPKPPPFLNBOPPCPPPEBBCPBOPBDPPMPPEPPPPENBPLGOPKPOPMPAPIPPPDJPPPCJPEPFEPAPNPPPPPPPENBFANEPPPHPPPPCPPPLADKLCAKPPDIFHNPFDBIJPAPPPPPPIPNGDIPBPJOAPPPPEPBPNNPPPIBDJPJFDHPHJAEJPBBDPIPPPLPKNADKEPGHEIPKHPOPPPAPBPIPAPPPDLPGPPEPPNPPCCPEBDADPHDBPBBCPCPGOPPDFBFLPPAKBAPPPBPPPEOHLOJBPOBBIMEJPMKODDCGBPHPPPPPPDPPKPPPPGCDPPPANMDCPNPDPBPPPDKPPHGDIJPPCPAGDCHPPPIIPBPIKPNAJCEMAPJPMKFLNPPFEBPGPHIFPAPNLPKLODBPPPHPPPPLPGHPNODIFAFPBPPCPPFGPPBPPDPPAPIBPGOPDNPPFPPPLAPICPNPFGPDNPPCPPJPGPPNPCOPGPBMLPDNPPCPPPPPDPKPPLIDMPCPLPFEIPPPBHGLPMHPPPPPJEKPPBBBHPGAALNPGPPDHNBPCBPOFPCPPKLPPOPHPBBHDGDPECCPGFPPPBGPMFPPNPPHEKPPPELPPGPPPJFPGPMBPPHFNNPPPPKEPJAECPPPFDPPIIFDLMPEOLNDCAGLPAPPEIPPPPIPEAEPPPNPONPMFPHPPDFPIHFKMPPCPICPECBEDPPPPMDPPPPAPOGPPIGBPKPLPKFPEBAFPAEPPPEPPPLPPPDPGOFPBJAPOPPDPPFCPOPAFBJGGLPMPPJBJDIPEEHJIFBDDMLJLCPAPMABPDPBPHOPFCPCPOIHEPPIBPMIPEPIBACBFOAPPBHCEGPAKDPFPEAPKPPBPIMPPPJPPPJPPIPGHMIFFPGPPPHPPOCAKAMGPPFAJPAIPPGPPGEPHPLPPPDADPCNPPPPNPGCAPAPP',  # noqa
                                               'FPPDBHPPCPAAPCCJBBFDPAGIPPPHCPPPCOBLBBPBNNPPJPJPALLIGBPPMAEHAJBFJPOPPPAPABPEPPPJJPFHGGCPPDKECDPPCOIJPIHCIOEPJPCPBFFIAGPJOFGMCPDAGJMPKFHJDPPPPEDBPPPCKOPPOBALDAPOJPPPPEBPANPAPPGPPPPIPPNPEIPPPPHIDPHHBOPDCKJFPPPKPNJHPJGPFPAGKIKIKPODFGAPGPHPPAJPJPGDCAPPIOCPPPJJEEPPPDPFPKJKJKPDPCEDPJFJPLPBPPGPPNPGLPPMCNCEPDPFFHPAOFBEMPPCLPPIMPMDNNMPOBPCPIMJKDKPPPPPCPPPPPGPPPPPDDCODIPPAAKLEPPPAPGFOPBCMPKEFKBPPBJJFPPPGPIPPBBAPDPBJFPBBPPFDFNDIAPPBGDPJAAPNINPFGPDHPPGPAPCLPAFLPPPPPPPPBPCPFCPFKJNPMBFPPJKJLECLNPPEPEPPPNMPLPCEBPFBPAOELLHICPPMAIHEFNLGPAPPPPFPAPODPGLAFPPPPLPPBDPPCFPPKKNFNANCPEPPJPPPCBLPEPPPDPEMOOGPKPBPPCPMJEOJPDJPMPIEJEIMPBGADPOPPCPBPCPDPONIGPOKGMPJPPBFPEFOAPEFPIHPODPPPGBPAFPPFJCBPAPPKPAAPPDLLHPFEBLPOIMPPDPCPPDAPCPFEPPBOPNGPPHGPPPPPJPNHPIBEPPPPEPAPBDPPPBPIPPGAOKKALEPFMBMPJBPPAPMKMPEPPFPKNJPMPBPEOEPLPPLMDAPPPLAPNPDAPCHPHHPAPPFPPMFPKCBPHBECPDPNPPAPCGPDDPPDPPEPAPNPCNCPFBCKFMPJPFGONPBILPFPFPKGKPPPJIPFBFKEBDCGDIPEBPEPHICPFPPPIHMBOPHFGPPEPANPPPMCPKPPPPPBCPPJPMAFAHBPLEPBIAFPPPABPEPPGAJPPPCPJAFMPDHCPBAPPPPCBCPPPDPHJNAPAPCFMPPPIPEPDPBGOONBPMPMONPONPPPMPFPNPEFCGJBPADHCMPEKPPELKHOPPPDHMFHMPGPLAIPBEDPPPPKBPPFINPFPMPPPPPBPODKPJDBPPGPBKEFLHPPPPCLPBPIPOLPPPAEPAOKPEHOPBPPIDPHEPPPAMPPBPPEHKFGPMPBPPFPHPPPEAPMPLPPNIPPBGBAIPMKKPPLDGPPBAPEPCGPPPPKAMAACFDPENHJPMPPHPAPPMPKABDPBBPPHLJJPAFDEMBLFPPAPGPPPPNAOPBBBPALNPPPPMAIKPOACEGPIPPDMGDAPBLCPPDDPEPPPBGPGAPPMFPFBPPCBKPNBPFCAOBBNPPNEDCPKPOINNBPPPEADPBPPDIBDPPCACBPJPHIBDHICJPHPPCEKPLGPBGKPPJPIPPACPACPNPPPPPPPHKFPJEKPKFFNPPBOFNKPPPDIBPPDPPBEHFCPPPDPIPDMPPGLAPBPHEPBPHCCDHOBEACPBPIPGPPPHBPPCGMPPCCEMPPHFPEDPDHPPGLPAJFNDKPPPPPPNDNOEDPDPDMPIBPOKDPPIPPLCKPMGBKPPPGPPPEMPIPPPDINCPCEFPCJPPGPJPCNAAIPGPKDPJACPHPEAEDPPPPBJJPEIJDFIPLPPCABGPPEDDPFDFBPPPPEDPPLJGPPCPPCPFKPPADPFPPMPPPGNIPBPPGPDPPPPDKIFPPCPGHFEBIGGPPPJCPEDPEPPMPPIPPPGHPPDKHPPDPOPPGPCPPGPAMOHFKFMPOPPJOLFPFFPCJIMPCPPPEAPCHPHMCPDOCDPPPOLJPPPLPNNDGFGCHJEPCPIAPCJNPPPFPCPPPPPPDPDADPJKDCGPPHPPEFPJHCDEPCGAJHGPPOKLPPLPPEGPKPJJPEPGPKIFDJOCPJNPPNGBPBHMBIPPGPPBPDEJBIPJGNCPOPJAOJGPKGJELHCHBFFLPNIBPAINPPOGNPCLCKEPFPPPPPPPPGPBCCPGPDPDBAIEHBEAOCPPCENEADIBFAGMIPPMFPMAPELLBOP']}},  # noqa
                            'boost': 0.8,
                            'min_score': 0.5}}]}}}


class TestLabelsFilter(FilterBaseTestCase):

    Filter = LabelFilter

    @pytest.fixture
    def mock_data(self):
        return {'type': LabelFilter.type,
                'modelId': 'bc28213f-cf3a-16a2-9f21-0242ac130003'}

    @pytest.fixture
    def mock_query_data(self, mock_data):
        data = mock_data
        data['values'] = {'labels': ['Celeste', 'David']}
        return data

    def test_get_es_agg(self, mock_data):
        _filter = self.Filter(mock_data)
        agg_query = _filter.get_es_agg()
        name = _filter.name
        assert agg_query == {
            'size': 0,
            'query': {
                'nested': {
                    'path': 'labels',
                    'query': {
                        'bool': {
                            'filter': [
                                {'term': {'labels.modelId': 'bc28213f-cf3a-16a2-9f21-0242ac130003'}}]}}}},  # noqa
            'aggs': {
                name: {
                    'nested': {
                        'path': 'labels'
                    },
                    'aggs': {
                        'modelId': {
                            'filter': {
                                'term': {
                                    'labels.modelId': 'bc28213f-cf3a-16a2-9f21-0242ac130003'
                                }
                            },
                            'aggs': {
                                f'nested_{name}': {
                                    'terms': {
                                        'field': 'labels.label',
                                        'size': 1000}}}}}}}}

    def test_get_es_agg_with_options(self, mock_data):
        mock_data['order'] = 'asc'
        mock_data['minimumCount'] = 2
        _filter = self.Filter(mock_data)
        agg_query = _filter.get_es_agg()
        name = _filter.name
        assert agg_query == {
            'size': 0,
            'query': {'nested': {'path': 'labels',
                                 'query': {'bool': {'filter': [{'term': {
                                     'labels.modelId': 'bc28213f-cf3a-16a2-9f21-0242ac130003'}}]}}}},  # noqa
            'aggs': {name: {'nested': {'path': 'labels'},
                            'aggs': {
                                'modelId': {
                                    'filter': {'term': {'labels.modelId': 'bc28213f-cf3a-16a2-9f21-0242ac130003'}},  # noqa
                                    'aggs': {f'nested_{name}': {
                                        'terms': {
                                            'field': 'labels.label',
                                            'size': 1000,
                                            'order': {'_count': 'asc'},
                                            'min_doc_count': 2}}}}}}}}

    def test_get_query(self, mock_query_data):
        _filter = self.Filter(mock_query_data)
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'must': [{
                        'nested': {
                            'path': 'labels',
                            'query': {
                                'bool': {
                                    'filter': [
                                        {'terms': {'labels.modelId': [
                                            'bc28213f-cf3a-16a2-9f21-0242ac130003']}},
                                        {'terms': {'labels.label': ['Celeste', 'David']}},
                                        {'terms': {'labels.scope': ['TRAIN', 'TEST']}}
                                    ]}}}}]}
            }
        }

    def test_get_query_set_scope(self, mock_query_data):
        mock_query_data['values']['scope'] = 'train'
        _filter = self.Filter(mock_query_data)
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'must': [{
                        'nested': {
                            'path': 'labels',
                            'query': {
                                'bool': {
                                    'filter': [
                                        {'terms': {'labels.modelId': [
                                            'bc28213f-cf3a-16a2-9f21-0242ac130003']}},
                                        {'terms': {'labels.label': ['Celeste', 'David']}},
                                        {'terms': {'labels.scope': ['TRAIN']}}
                                    ]}}}}]}
            }
        }


class TestDateFilter(FilterBaseTestCase):

    Filter = DateFilter

    @pytest.fixture
    def mock_data(self):
        return {
            'type': 'date',
            'attribute': 'system.timeCreated'
        }

    @pytest.fixture
    def mock_query_data(self, mock_data):
        mock_data['values'] = {'min': 123, 'max': 234}
        return mock_data

    def test_get_agg(self, mock_data):
        _filter = self.Filter(mock_data)
        agg = _filter.get_es_agg()
        assert agg['aggs'][_filter.name] == {
            'stats': {
                'field': 'system.timeCreated'
            }
        }

    def test_get_es_query(self, mock_query_data):
        _filter = self.Filter(mock_query_data)
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'filter': [
                        {'range': {'system.timeCreated': {'gte': 123, 'lte': 234}}}
                    ]
                }}}
