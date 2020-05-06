import pytest

from rest_framework.exceptions import ValidationError

from searches.filters import (BaseFilter, RangeFilter, ExistsFilter, FacetFilter,
                              LabelConfidenceFilter, TextContentFilter,
                              SimilarityFilter)


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
        mock_data['minimum_count'] = 10
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

    def test_get_es_agg(self, mock_data):
        _filter = LabelConfidenceFilter(mock_data)
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

    def test_get_es_query(self, mock_query_data):
        _filter = LabelConfidenceFilter(mock_query_data)
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'filter': [
                        {'script_score': {
                            'query': {
                                'terms': {
                                    'analysis.zvi-label-detection.predictions.label': ['value1', 'value2']  # noqa
                                }
                            },
                            'script': {
                                'source': 'kwconf',
                                'lang': 'zorroa-kwconf',
                                'params': {
                                    'field': 'analysis.zvi-label-detection.predictions',
                                    'labels': ['value1',
                                               'value2'],
                                    'range': [.5, .8]
                                }
                            },
                            'min_score': .5
                        }}]}}}

    def test_add_to_query(self, mock_query_data):
        _filter = LabelConfidenceFilter(mock_query_data)
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
        assert new_query == {
            'query': {
                'bool': {
                    'filter': [
                        {'terms': {'my_attr': ['value1', 'value2']}},
                        {'script_score': {
                            'query': {
                                'terms': {
                                    'analysis.zvi-label-detection.predictions.label': ['value1', 'value2']  # noqa
                                }
                            }, 'script': {
                                'source': 'kwconf',
                                'lang': 'zorroa-kwconf',
                                'params': {
                                    'field': 'analysis.zvi-label-detection.predictions',
                                    'labels': ['value1', 'value2'],
                                    'range': [.5, .8]
                                }
                            }, 'min_score': .5
                        }}]}}}

    def test_add_to_label_conf_query(self, mock_query_data):
        _filter = LabelConfidenceFilter(mock_query_data)
        _filter2 = LabelConfidenceFilter({
            'type': 'labelConfidence',
            'attribute': 'analysis.zvi-object-detection',
            'values': {
                'labels': ['dog', 'cat'],
                'min': 0.2,
                'max': 0.7
            }
        })
        query = _filter.get_es_query()
        query = _filter2.add_to_query(query)
        assert query == {
            'query': {
                'bool': {
                    'filter': [
                        {'script_score': {
                            'query': {
                                'terms': {
                                    'analysis.zvi-label-detection.predictions.label': ['value1', 'value2']}},  # noqa
                            'script': {
                                'source': 'kwconf',
                                'lang': 'zorroa-kwconf',
                                'params': {
                                    'field': 'analysis.zvi-label-detection.predictions',
                                    'labels': ['value1', 'value2'],
                                    'range': [0.5, 0.8]}},
                            'min_score': 0.5}},
                        {'script_score': {
                            'query': {
                                'terms': {
                                    'analysis.zvi-object-detection.predictions.label': ['dog', 'cat']}},  # noqa
                            'script': {
                                'source': 'kwconf',
                                'lang': 'zorroa-kwconf',
                                'params': {
                                    'field': 'analysis.zvi-object-detection.predictions',
                                    'labels': ['dog', 'cat'],
                                    'range': [0.2, 0.7]}},
                            'min_score': 0.2}}]}}}


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
                    'filter': [
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
                    'filter': [
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
                    'filter': [
                        {'simple_query_string': {
                            'query': 'test',
                            'fields': ['one.two']
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
        data['values'] = {'hashes': ['abcd']}
        return data

    def test_get_es_query(self, mock_query_data):
        _filter = self.Filter(mock_query_data)
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'filter': [
                        {'script_score': {
                            'query': {'match_all': {}},
                            'script': {
                                'source': 'similarity',
                                'lang': 'zorroa-similarity',
                                'params': {
                                    'minScore': 0.75,
                                    'field': 'analysis.zvi-image-similarity.simhash',
                                    'hashes': ['abcd']}},
                            'boost': 1.0,
                            'min_score': 0.75}}]}}}

    def test_get_query_with_optionals(self):
        _filter = self.Filter({
            'type': SimilarityFilter.type,
            'attribute': 'analysis.zvi-image-similarity',
            'values': {
                'hashes': ['abcd', 'defg'],
                'minScore': 0.50,
                'boost': 0.8
            }
        })
        query = _filter.get_es_query()
        assert query == {
            'query': {
                'bool': {
                    'filter': [
                        {'script_score': {
                            'query': {'match_all': {}},
                            'script': {
                                'source': 'similarity',
                                'lang': 'zorroa-similarity',
                                'params': {
                                    'minScore': 0.5,
                                    'field': 'analysis.zvi-image-similarity.simhash',
                                    'hashes': ['abcd', 'defg']}},
                            'boost': 0.8,
                            'min_score': 0.5}}]}}}
