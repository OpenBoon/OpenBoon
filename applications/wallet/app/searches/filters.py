import uuid

from rest_framework.exceptions import ValidationError


class BaseFilter(object):
    """Abstract Filter object all concrete Filters should inherit from.

    Class Variables:
        type: Identifier for the Filter
        required_agg_keys: The minimum key names that should exist to run an aggregation.
        required_query_keys: The additional key names needed to generate an ES query.
        optional_keys: Additional keys that may or may not be included. Purely informational.
        agg_prefix: The prefix ES adds to the aggregation name response keys.
    """

    type = None
    required_agg_keys = []
    required_query_keys = []
    optional_keys = []
    agg_prefix = ''

    def __init__(self, data):
        self.data = data
        self.name = str(uuid.uuid4())

    def is_valid(self, query=False, raise_exception=False):
        """Confirms the required keys for this filter have been given in the data.

        Raises:
            ValidationError: If the raise_exception flag is given, return a list of errors found.

        Returns:
            bool: Whether all the required data was given
        """
        errors = []
        required_keys = self.required_agg_keys
        if query:
            required_keys.extend(self.required_query_keys)

        for key in required_keys:
            if key not in self.data:
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
        """Gives the Elasticsearch query for the current Filter values."""
        raise NotImplementedError()

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
        action = 'exists' if self.data['exists'] else 'missing'
        return {'filter': {action: [self.data['attribute']]}}


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
        return {
            'filter': {
                'terms': self.data['facets']
            }
        }


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
        return {
            'filter': {
                'range': {
                    attribute: {
                        'gte': self.data['min'],
                        'lte': self.data['max']
                    }
                }
            }
        }
