import uuid

from django.core.exceptions import ImproperlyConfigured
from rest_framework.exceptions import ValidationError, NotFound

from zmlp.search import SimilarityQuery


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

    def __init__(self, data, zmlp_app=None):
        """Initializes filter instance.

        Args:
            data (dict): The initial data
            zmlp_app (ZmlpApp): A ZMLP App instance, in case connecting to ZMLP is required.
        """
        self.data = data
        self.zmlp_app = zmlp_app
        self.name = str(uuid.uuid4())
        self.errors = []

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

        for key in self.required_agg_keys:
            if key not in self.data:
                self.errors.append({key: 'This value is required.'})

        if query:
            if 'values' not in self.data:
                self.errors.append({'values': 'This value is required.'})
            for key in self.required_query_keys:
                if key not in self.data['values']:
                    self.errors.append({key: 'This value is required.'})

        if self.errors:
            if raise_exception:
                raise ValidationError(detail=self.errors)
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


class LabelConfidenceFilter(BaseFilter):

    type = 'labelConfidence'
    required_agg_keys = ['attribute']
    required_query_keys = ['labels', 'min', 'max']
    agg_prefix = 'sterms'

    def get_es_agg(self):
        attribute = self.data['attribute']
        agg = {
            'size': 0,
            'aggs': {
                self.name: {
                    'terms': {
                        'field': f'{attribute}.predictions.label',
                        'size': 1000
                    }
                }
            }}
        return agg

    def get_es_query(self):
        attribute = self.data['attribute']
        labels = self.data['values']['labels']
        min = self.data['values']['min']
        max = self.data['values']['max']
        return {
            'query': {
                'bool': {
                    'filter': [{
                        'script_score': {
                            'query': {
                                'terms': {f'{attribute}.predictions.label': labels}
                            },
                            'script': {
                                'source': 'kwconf',
                                'lang': 'zorroa-kwconf',
                                'params': {
                                    'field': f'{attribute}.predictions',
                                    'labels': labels,
                                    'range': [min, max]
                                }
                            },
                            'min_score': min
                        }
                    }]
                }
            }
        }


class TextContentFilter(BaseFilter):

    type = 'textContent'
    required_agg_keys = []
    required_query_keys = ['query']

    # No aggregations needed for this

    def get_es_query(self):
        # if no attribute, no fields
        # if attribute that's only two levels without a content field, add content field
        # if attribute use attribute
        query = self.data['values']['query']
        simple_query_string = {
            'simple_query_string': {
                'query': query
            }
        }
        attr = self.data.get('attribute')
        if attr:
            # if this is coming from a TextContent Analysis Module
            attr_split = attr.split('.')
            if (len(attr_split) == 2
                    and attr_split[0] == 'analysis'
                    and not attr.endswith('content')):
                # add the field to search over
                attr = f'{attr}.content'
            simple_query_string['simple_query_string']['fields'] = [attr]

        return {
            'query': {
                'bool': {
                    'filter': [
                        simple_query_string
                    ]
                }
            }
        }


class SimilarityFilter(BaseFilter):

    type = 'similarity'
    required_agg_keys = ['attribute']
    required_query_keys = ['ids']
    optional_keys = ['values.minScore', 'values.boost']

    # No Aggregations needed for this

    def get_es_query(self):
        hashes = self._get_hashes()
        min_score = self.data['values'].get('minScore', 0.75)
        boost = self.data['values'].get('boost', 1.0)
        attribute = f'''{self.data['attribute']}.simhash'''
        query = SimilarityQuery(hashes, min_score=min_score, boost=boost,
                                field=attribute)
        return {
            'query': {
                'bool': {
                    'filter': [
                        query.for_json()
                    ]
                }
            }
        }

    def _get_hashes(self):
        """Returns all of the simhashes for the assets given to the filter."""
        if self.zmlp_app is None:
            raise ImproperlyConfigured()

        ids = self.data['values']['ids']
        assets = self.zmlp_app.assets.search({'query': {'terms': {'_id': ids}}})

        # Some validation that we got all the ids back
        if len(set(assets)) != len(set(ids)):
            raise NotFound(detail={'ids': 'One of the specified assets for '
                                          'the similarity filter was not found.'})

        hashes = []
        for asset in assets:
            simhash = asset.get_attr('analysis.zvi-image-similarity.simhash')
            if simhash:
                hashes.append(simhash)
        return hashes
