import pytest

from rest_framework.exceptions import ValidationError

from searches.filters import BaseFilter, RangeFilter, ExistsFilter, FacetFilter


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
        del(mock_load_data['trucks'])
        _filter = MockFilter(mock_load_data)
        assert not _filter.is_valid()

    def test_is_valid_no_query_raise_exception_missing_key(self, mock_load_data):
        del(mock_load_data['trucks'])
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
        del(mock_query_data['values']['dogs'])
        _filter = MockFilter(mock_query_data)
        assert not _filter.is_valid(query=True)

    def test_is_valid_query_raise_exception_missing_key(self, mock_query_data):
        del(mock_query_data['values']['dogs'])
        _filter = MockFilter(mock_query_data)
        with pytest.raises(ValidationError):
            _filter.is_valid(query=True, raise_exception=True)


class TestExistsFilter:

    @pytest.fixture
    def mock_data(self):
        return {'type': 'exists',
                'attribute': 'name'}

    @pytest.fixture
    def mock_query_data(self, mock_data):
        data = mock_data
        data['values'] = {'exists': True}
        return data

    def test_is_valid(self, mock_data):
        _filter = ExistsFilter(mock_data)
        assert _filter.is_valid()

    def test_is_valid_for_query(self, mock_query_data):
        _filter = ExistsFilter(mock_query_data)
        assert _filter.is_valid(query=True)

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


class TestRangeFilter:

    @pytest.fixture
    def mock_data(self):
        return {'type': 'range',
                'attribute': 'my_attr'}

    @pytest.fixture
    def mock_query_data(self, mock_data):
        data = mock_data
        data['values'] = {'min': 1, 'max': 100}
        return data

    def test_is_valid(self, mock_data):
        _filter = RangeFilter(mock_data)
        assert _filter.is_valid()

    def test_is_valid_for_query(self, mock_query_data):
        _filter = RangeFilter(mock_query_data)
        assert _filter.is_valid(query=True)

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
        assert query == {'query': {'bool': {'filter': [{'range': {'my_attr': {'gte': 1, 'lte': 100}}}]}}}  # noqa

    def test_add_to_existing_range_query(self, mock_query_data):
        _filter = RangeFilter(mock_query_data)
        query = {'query': {'bool': {'filter': [{'range': {'foo': {'gte': 1, 'lte': 5}}}]}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'range': {'foo': {'gte': 1, 'lte': 5}}},
                                                       {'range': {'my_attr': {'gte': 1, 'lte': 100}}}]}}}  # noqa

    def test_add_to_existing_range_query_same_attr(self, mock_query_data):
        _filter = RangeFilter(mock_query_data)
        query = {'query': {'bool': {'filter': [{'range': {'my_attr': {'gte': 1, 'lte': 5}}}]}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'range': {'my_attr': {'gte': 1, 'lte': 5}}},
                                                       {'range': {'my_attr': {'gte': 1, 'lte': 100}}}]}}}  # noqa

    def test_add_to_other_query(self, mock_query_data):
        _filter = RangeFilter(mock_query_data)
        query = {'query': {'bool': {'filter': [{'terms': ['foo', 'bar']}]}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'terms': ['foo', 'bar']},
                                                       {'range': {'my_attr': {'gte': 1, 'lte': 100}}}]}}}  # noqa


class TestFacetFilter:

    @pytest.fixture
    def mock_data(self):
        return {'type': 'facet',
                'attribute': 'my_attr'}

    @pytest.fixture
    def mock_query_data(self, mock_data):
        data = mock_data
        data['values'] = {'facets': ['value1', 'value2']}
        return data

    def test_is_valid(self, mock_data):
        _filter = FacetFilter(mock_data)
        assert _filter.is_valid()

    def test_is_valid_for_query(self, mock_query_data):
        _filter = FacetFilter(mock_query_data)
        assert _filter.is_valid(query=True)

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
        assert query == {'query': {'bool': {'filter': [{'terms': {'my_attr': ['value1', 'value2']}}]}}}  # noqa

    def test_add_to_existing_facet_query(self, mock_query_data):
        _filter = FacetFilter(mock_query_data)
        query = {'query': {'bool': {'filter': [{'terms': {'my_attr': ['foo', 'bar']}}]}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'terms': {'my_attr': ['foo', 'bar']}},
                                                       {'terms': {'my_attr': ['value1', 'value2']}}]}}}  # noqa

    def test_add_to_other_query(self, mock_query_data):
        _filter = FacetFilter(mock_query_data)
        query = {'query': {'bool': {'filter': [{'range': {'my_attr': {'gte': 1, 'lte': 100}}}]}}}
        query = _filter.add_to_query(query)
        assert query == {'query': {'bool': {'filter': [{'range': {'my_attr': {'gte': 1, 'lte': 100}}},  # noqa
                                                       {'terms': {'my_attr': ['value1', 'value2']}}]}}}  # noqa
