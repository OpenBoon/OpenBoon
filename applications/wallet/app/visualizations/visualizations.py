import copy
from rest_framework.exceptions import ValidationError


class BaseVisualization(object):
    """Abstract Visualization object all concrete Visualizations should inherit from."""

    type = None
    required_keys = []
    required_option_keys = []
    agg_prefix = ''

    def __init__(self, data, request=None, query=None):
        self.data = data
        self.request = request
        self.query = query
        self.errors = []

    def __eq__(self, other):
        if type(self) == type(other) and self.data == other.data:
            return True
        return False

    @property
    def id(self):
        """Returns the unique ID for this visualization."""
        return self.data['id']

    @property
    def options(self):
        """Returns the options that were set on this visualization."""
        return self.data.get('options', {})

    def is_valid(self, raise_exception=False):
        """Validates that every required key and options key needed was included.

        Args:
            raise_exception (bool): If the visualization fails validation, raises an exception.

        Returns:
            (bool): Whether the visualization has the required data to operate.
        """
        for key in self.required_keys:
            if key not in self.data:
                self.errors.append({key: 'This value is required.'})

        for key in self.required_option_keys:
            if key not in self.data.get('options', {}):
                self.errors.append({key: 'This `options` value is required.'})

        if self.errors:
            if raise_exception:
                raise ValidationError(detail=self.errors)
            else:
                return False

        return True

    def serialize_response_data(self, data):
        """Pulls the associated response data out for this visualization and formats it.

        Since multiple aggregations run in a single query, this is a helper to look at
        a response from the api and determine which component of the response is specific
        to this visualizations aggregation, and then formats it correctly for the overall
        response to the frontend by keying it to it's ID.

        Args:
            data (dict): An entire response body from Elasticsearch.

        Returns:
            (dict): The response component specific to this visualization.
        """
        results = data['aggregations'][f'{self.agg_prefix}#{self.id}']
        response = {'id': self.id,
                    'results': results}
        return response

    def get_es_agg(self):
        """Return the aggregation used to load the data for this visualization.

        Currently, this assumes that each visualization only needs one aggregation. If
        more than one aggregation, or a different type of query altogether, is needed to
        retrieve the appropriate data for a visualization, we may need to rework how
        VizBuddy is building the overall query.

        Returns:
            (dict): The aggregation component to add to a single Elasticsearch query.
        """
        raise NotImplementedError()


class RangeVisualization(BaseVisualization):

    type = 'range'
    required_keys = ['id', 'attribute']
    agg_prefix = 'stats'

    def get_es_agg(self):
        attribute = self.data['attribute']
        return {'stats': {'field': attribute}}


class FacetVisualization(BaseVisualization):

    type = 'facet'
    required_keys = ['id', 'attribute']
    required_option_keys = []
    agg_prefix = 'sterms'

    def get_es_agg(self):
        attribute = self.data['attribute']
        field_type = self.data.get('fieldType', 'facet')
        order = self.options.get('order', 'desc')
        minimum_count = self.options.get('minimumCount')
        size = self.options.get('size', 1000)
        if field_type == 'labelConfidence':
            attribute = f'{attribute}.predictions.label'
        agg = {
            'terms': {
                'field': attribute,
                'size': size,
                'order': {'_count': order}

            }
        }
        if minimum_count:
            agg['terms']['min_doc_count'] = minimum_count
        return agg


class HistogramVisualization(BaseVisualization):

    type = 'histogram'
    required_keys = ['id', 'attribute', 'fieldType']
    required_option_keys = []
    agg_prefix = 'filter#labels.histogram#scores'

    def get_es_agg(self):
        attribute = self.data['attribute']
        field_type = self.data['fieldType']
        size = self.options.get('size', 10)
        if size < 1:
            size = 1
        if field_type == 'labelConfidence':
            interval, offset = self.get_interval_and_offset(f'{attribute}.predictions.score',
                                                            size)
            agg = {
                "nested": {
                    "path": f"{attribute}.predictions"
                },
                "aggs": {
                    "labels": {
                        'filter': {
                            'match_all': {}
                        },
                        "aggs": {
                            "scores": {
                                "histogram": {
                                    "field": f"{attribute}.predictions.score",
                                    "interval": interval,
                                    'offset': offset
                                }
                            }
                        }
                    }
                }
            }
            if self.query:
                labels = self.query['query']['bool']['filter'][0]['terms'][
                    f'{attribute}.predictions.label']
                agg['aggs']['labels']['filter'] = {"terms": {f"{attribute}.predictions.label": labels}
                }
            return agg
        else:
            interval, offset = self.get_interval_and_offset(attribute, size)
            return {
                "histogram": {
                    "field": attribute,
                    "interval": interval,
                    "offset": offset
                }
            }

    def serialize_response_data(self, data):
        if self.data['fieldType'] == 'labelConfidence':
            results = data['aggregations'][f'nested#{self.id}']['filter#labels']['histogram#scores']
        else:
            results = data['aggregations'][f'histogram#{self.id}']
        return {'id': self.id, 'results': results}

    def get_interval_and_offset(self, attribute, size):
        """Calculates the appropriate interval and offset for the selected # of buckets"""
        client = self.request.client
        field_type = self.data['fieldType']
        # Use the existing query if it exists
        if self.query:
            query = copy.deepcopy(self.query)
        else:
            query = {}
        # Add stat aggregation to the query to get the min and max values
        query['size'] = 0
        if field_type == 'labelConfidence':
            query['aggs'] = {
                self.id: {
                    'nested': {
                        'path': attribute.replace('.score', '')
                    },
                    'aggs': {
                        'stats': {
                            'extended_stats': {
                                'field': attribute
                            }

                        }
                    }
                }
            }
        else:
            query['aggs'] = {
                self.id: {
                    'stats': {
                        'extended_stats': {
                            'field': attribute
                        }

                    }
                }
            }
        response = client.post('api/v3/assets/_search', query)
        # Get the min and max from the response
        if field_type == 'labelConfidence':
            nested_agg_name = f'nested#{self.id}'
            nested_stats_agg_name = f'stats#nested_{self.id}'
            agg_data = response['aggregations'][nested_agg_name]
        else:
            agg_key = f'stats#{self.id}'
            agg_data = response['aggregations'][agg_key]
        min, max = agg_data['extended_stats#stats']['min'], agg_data['extended_stats#stats']['max']
        # Calculate correct interval to get the # of buckets we want
        if size == 1:
            interval = max - min
        else:
            interval = (max - min) / (size - 1)
        return interval or 0.1, min
