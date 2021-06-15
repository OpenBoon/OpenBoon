import uuid

from django.core.exceptions import ImproperlyConfigured
from rest_framework.exceptions import ValidationError, NotFound

from boonsdk.search import SimilarityQuery, LabelConfidenceQuery


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

    def __init__(self, data, request=None):
        """Initializes filter instance.

        Args:
            data (dict): The initial data
            request (Request): A DRF request instance, which should have the app and client.
        """
        self.data = data
        self.request = request
        self.name = str(uuid.uuid4())
        self.errors = []
        self._field_type = None
        from .utils import FieldUtility
        self.field_utility = FieldUtility()

    def __eq__(self, other):
        if type(self) == type(other) and self.data == other.data:
            return True
        return False

    @property
    def field_type(self):
        """Returns the field for the attribute associated with this filter."""
        if self._field_type is None:
            attribute = self.data.get('attribute')
            self._field_type = self.field_utility.get_attribute_field_type(attribute,
                                                                           self.request.client)
        return self._field_type

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

    def get_clip_query(self):
        """Gives the `clip` query for the current filter values."""
        return {}

    def add_to_query(self, query):
        """Adds the given filters information to a pre-existing query.

        Adds this query to a prebuilt query. Every clause will be appended to the list
        of existing `bool` clauses if they exist, in an additive manner.
        """
        this_query = self.get_es_query()
        if not this_query:
            # Catches the case where a filter doesn't have a relevant query to add
            return query

        bool_clauses = this_query['query'].get('bool', {})

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

    def add_to_clip_query(self, query):
        """Adds the given filter's clip query to a pre-existing query.

        Adds this clip query to a prebuilt query. This generates a query where each
        filter is added to the query under a `should` condition.
        """
        this_query = self.get_clip_query()
        if not this_query:
            return query

        if not query:
            # Setup the initial OR/should based query
            query = {'query': {'bool': {'should': []}}}

        # Append this queries bool to the list of should conditions
        query['query']['bool']['should'].append(this_query['query'])

        return query

    def serialize_agg_response(self, response):
        """Serializes an aggregation query's response from boonsdk."""
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

    @property
    def attribute(self):
        return self.data['attribute']

    def get_es_agg(self):
        return {
            'size': 0,
            'aggs': {
                self.name: {
                    'stats': {
                        'field': self.attribute
                    }
                }
            }
        }

    def get_es_query(self):
        min = self.data['values']['min']
        max = self.data['values']['max']
        return {
            'query': {
                'bool': {
                    'filter': [
                        {'range': {self.attribute: {'gte': min, 'lte': max}}}
                    ]
                }
            }
        }


class PredictionCountFilter(RangeFilter):

    type = 'predictionCount'

    @property
    def attribute(self):
        attribute = self.data['attribute']
        return f'{attribute}.count'


class FacetFilter(BaseFilter):

    type = 'facet'
    required_agg_keys = ['attribute']
    required_query_keys = ['facets']
    optional_keys = ['order', 'minimumCount']
    agg_prefix = 'sterms'

    def get_es_agg(self):
        attribute = self.data['attribute']
        order = self.data.get('order')
        minimumCount = self.data.get('minimumCount')
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
        if minimumCount:
            agg['aggs'][self.name]['terms']['min_doc_count'] = minimumCount
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
        if self.field_type == 'prediction':
            attribute = f'{attribute}.predictions.label'
        elif self.field_type == 'single_label':
            attribute = f'{attribute}.label'

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
        return agg

    def get_es_query(self):
        attribute = self.data['attribute']
        labels = self.data['values']['labels']
        min = self.data['values']['min']
        max = self.data['values']['max']
        if self.field_type == 'prediction':
            return self.get_prediction_query(attribute, labels, min, max)
        else:
            return self.get_single_label_query(attribute, labels, min, max)

    def get_prediction_query(self, attribute, labels, min, max):
        """Query to return when querying against a prediction analysis schema."""
        namespace = attribute.split('.')[1]
        confidence_query = LabelConfidenceQuery(namespace=namespace,
                                                labels=labels,
                                                min_score=min,
                                                max_score=max)
        return {
            "query": confidence_query.for_json()
        }

    def get_single_label_query(self, attribute, labels, min, max):
        """Query to return when querying against a Single Label analysis schema."""
        return {
            'query': {
                'bool': {
                    'filter': [
                        {
                            'terms': {
                                f'{attribute}.label': labels
                            }
                        },
                        {
                            'range': {
                                f'{attribute}.score': {
                                    'from': min,
                                    'to': max
                                }
                            }
                        }
                    ]
                }
            }
        }

    def get_clip_query(self):
        attribute = self.data['attribute']
        labels = self.data['values']['labels']
        min = self.data['values']['min']
        max = self.data['values']['max']
        if self.field_type == 'prediction' and 'video' in attribute:
            timeline = attribute.replace('analysis.', '')
            return {
                'query': {
                    'bool': {
                        'filter': [
                            {
                                'terms': {
                                    'clip.track': labels
                                }
                            },
                            {
                                'term': {
                                    'clip.timeline': timeline
                                }
                            },
                            {
                                'range': {
                                    'clip.score': {
                                        'from': min,
                                        'to': max
                                    }
                                }
                            }
                        ]
                    }
                }
            }
        else:
            return {}


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
                    'must': [
                        simple_query_string
                    ]
                }
            }
        }

    def get_clip_query(self):
        # Regardless of what the attribute is, search all content fields
        query = self.data['values']['query']
        simple_query_string = {
            'simple_query_string': {
                'query': query,
                'fields': ['clip.content']
            }
        }
        return {
            'query': {
                'bool': {
                    'must': [
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
                    'must': [
                        query.for_json()
                    ]
                }
            }
        }

    def _get_hashes(self):
        """Returns all of the simhashes for the assets given to the filter."""
        if self.request is None:
            raise ImproperlyConfigured()

        ids = self.data['values']['ids']
        assets = self.request.app.assets.search({'query': {'terms': {'_id': ids}}})

        # Some validation that we got all the ids back
        if len(set(assets)) != len(set(ids)):
            raise NotFound(detail={'ids': 'One of the specified assets for '
                                          'the similarity filter was not found.'})

        hashes = []
        for asset in assets:
            simhash = asset.get_attr(f'{self.data["attribute"]}.simhash')
            if simhash:
                hashes.append(simhash)
        return hashes


class LabelFilter(BaseFilter):

    type = 'label'
    required_agg_keys = ['datasetId']
    required_query_keys = ['labels']
    optional_keys = ['order', 'minimumCount']
    agg_prefix = ''

    def get_es_agg(self):
        dataset_id = self.data['datasetId']
        order = self.data.get('order')
        minimumCount = self.data.get('minimumCount')
        agg = {
            "size": 0,
            "query": {
                "nested": {
                    "path": "labels",
                    "query": {
                        "bool": {
                            "filter": [
                                {"term": {"labels.datasetId": dataset_id}}]
                        }
                    }
                }
            },
            "aggs": {
                self.name: {
                    "nested": {
                        "path": "labels"
                    },
                    "aggs": {
                        "datasetId": {
                            "filter": {
                                "term": {
                                    "labels.datasetId": dataset_id
                                }
                            },
                            "aggs": {
                                f'nested_{self.name}': {
                                    "terms": {
                                        "field": "labels.label",
                                        "size": 1000
                                    }
                                }
                            }
                        }
                    }
                }
            }}
        if order:
            agg['aggs'][self.name]['aggs']['datasetId']['aggs'][f'nested_{self.name}']['terms']['order'] = {'_count': order}  # noqa
        if minimumCount:
            agg['aggs'][self.name]['aggs']['datasetId']['aggs'][f'nested_{self.name}']['terms']['min_doc_count'] = minimumCount  # noqa
        return agg

    def get_es_query(self):
        dataset_id = self.data['datasetId']
        labels = self.data['values']['labels']
        # Cheat, in case they forget the scope look for all of them
        scope = self.data['values'].get('scope', 'all')
        if scope.lower() == 'all':
            scope = ['TRAIN', 'TEST']
        else:
            # Assume they get it right
            scope = [scope.upper()]
        query = {
            'query': {
                "bool": {
                    "must": [{
                        "nested": {
                            "path": "labels",
                            "query": {
                                "bool": {
                                    "filter": [
                                        {"terms": {"labels.datasetId": [dataset_id]}},
                                        {"terms": {'labels.label': labels}},
                                        {"terms": {'labels.scope': scope}}
                                    ]
                                }
                            }
                        }
                    }]
                }
            }
        }
        return query

    def serialize_agg_response(self, response):
        """Serializes an aggregation query's response from boonsdk."""
        count = response['hits']['total']['value']
        nested_agg_name = f'nested#{self.name}'
        terms_agg_name = f'sterms#nested_{self.name}'
        data = response['aggregations'][nested_agg_name]['filter#datasetId'][terms_agg_name]
        return {'count': count, 'results': data}


class DateFilter(BaseFilter):

    type = 'date'
    required_agg_keys = ['attribute']
    required_query_keys = ['min', 'max']
    optional_keys = []
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
