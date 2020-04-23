import uuid

from rest_framework.exceptions import ValidationError


class BaseFilter(object):
    """Abstract Filter object all concrete Filters should inherit from.

    Class Variables:
        type: Identifier for the Filter
        required_agg_keys: The minimum key names that should exist to run an aggregation.
        required_query_keys: The additional key names needed to generate an ES query. These
            keys all need to live under a `values` key.
        optional_keys: Additional keys that may or may not be included. Purely informational.
        agg_prefix: The prefix ES adds to the aggregation name response keys.

        Example JSON Rep:

            {
                'type': 'foo',
                'required_agg_key_1': '$value',
                'required_agg_key_2': '$value,
                'values': {
                    'required_query_key_1': $value,
                    'required_query_key_2': $value
                }
            }
    """

    type = None
    required_agg_keys = []
    required_query_keys = []
    optional_keys = []
    agg_prefix = ''

    def __init__(self, data):
        self.data = data
        self.name = str(uuid.uuid4())

    def __eq__(self, other):
        if type(self) == type(other) and self.data == other.data:
            return True
        return False

    def is_valid(self, query=False, raise_exception=False):
        """Confirms the required keys for this filter have been given in the data.

        Raises:
            ValidationError: If the raise_exception flag is given, return a list of errors found.

        Returns:
            bool: Whether all the required data was given
        """
        errors = []

        for key in self.required_agg_keys:
            if key not in self.data:
                errors.append({key: 'This value is required.'})

        if query:
            if 'values' not in self.data:
                errors.append({'values': 'This value is required.'})
            for key in self.required_query_keys:
                if key not in self.data['values']:
                    errors.append({key: 'This value is required.'})

        if errors:
            if raise_exception:
                raise ValidationError(detail=errors)
            else:
                return False

        return True

    def get_es_agg(self):
        """Gives the Elasticsearch query for running the appropriate aggregation to load the UI"""
        raise NotImplementedError()

    def get_es_query(self):
        """Gives the Elasticsearch query for the current Filter values.

        All queries are assumed to be written as ES `bool` queries, but all supported
        `bool` clauses can be used.
        """
        raise NotImplementedError()

    def add_to_query(self, query):
        """Adds the given filters information to a pre-existing query.

        Adds this query to a prebuilt query. Every clause will be appended to the list
        of existing `bool` clauses if they exist, in an additive manner.
        """
        this_query = self.get_es_query()
        bool_clauses = this_query['query']['bool']

        if 'query' not in query:
            # If the query key doesn't exist at all, add this filters whole query to it
            query.update(this_query)
        elif 'bool' not in query['query']:
            # If this query is not setup as a bool, then add this filters bool section to it
            query['query']['bool'] = this_query['query']['bool']
        else:
            # Check that every clause (ex. 'filter', 'must_not', 'should', etc) in
            # this filter's query gets added if it's missing, or extends what is
            # already existing
            for clause in bool_clauses:
                if clause not in query['query']['bool']:
                    query['query']['bool'][clause] = this_query['query']['bool'][clause]
                else:
                    query['query']['bool'][clause].extend(this_query['query']['bool'][clause])

        return query

    def serialize_agg_response(self, response):
        """Serializes an aggregation query's response from ZMLP."""
        count = response['hits']['total']['value']
        agg_name = f'{self.agg_prefix}#{self.name}'
        data = response['aggregations'][agg_name]
        return {'count': count, 'results': data}


class ExistsFilter(BaseFilter):

    type = 'exists'
    required_agg_keys = ['attribute']
    required_query_keys = ['exists']

    # No agg needed to load the UI for this filter

    def get_es_query(self):
        clause = self._get_query_clause()
        attribute = self.data['attribute']
        return {
            'query': {
                'bool': {
                    clause: [{'exists': {'field': attribute}}]
                }
            }
        }

    def _get_query_clause(self):
        return 'filter' if self.data['values']['exists'] else 'must_not'


class RangeFilter(BaseFilter):

    type = 'range'
    required_agg_keys = ['attribute']
    required_query_keys = ['min', 'max']
    agg_prefix = 'stats'

    def get_es_agg(self):
        attribute = self.data['attribute']
        return {
            'size': 0,
            'aggs': {
                self.name: {
                    'stats': {
                        'field': attribute
                    }
                }
            }
        }

    def get_es_query(self):
        attribute = self.data['attribute']
        min = self.data['values']['min']
        max = self.data['values']['max']
        return {
            'query': {
                'bool': {
                    'filter': [
                        {'range': {attribute: {'gte': min, 'lte': max}}}
                    ]
                }
            }
        }


class FacetFilter(BaseFilter):

    type = 'facet'
    required_agg_keys = ['attribute']
    required_query_keys = ['facets']
    optional_keys = ['order', 'minimum_count']
    agg_prefix = 'sterms'

    def get_es_agg(self):
        attribute = self.data['attribute']
        order = self.data.get('order')
        minimum_count = self.data.get('minimum_count')
        agg = {
            'size': 0,
            'aggs': {
                self.name: {
                    'terms': {
                        'field': attribute,
                        'size': 1000
                    }
                }
            }}
        if order:
            agg['aggs'][self.name]['terms']['order'] = {'_count': order}
        if minimum_count:
            agg['aggs'][self.name]['terms']['min_doc_count'] = minimum_count
        return agg

    def get_es_query(self):
        attribute = self.data['attribute']
        facets = self.data['values']['facets']
        return {
            'query': {
                'bool': {
                    'filter': [
                        {'terms': {attribute: facets}}
                    ]
                }
            }
        }
