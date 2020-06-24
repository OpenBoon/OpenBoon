from rest_framework.exceptions import ValidationError


class BaseVisualization(object):
    """Abstract Visualization object all concrete Visualizations should inherit from."""

    type = None
    required_keys = []
    required_option_keys = []
    agg_prefix = ''

    def __init__(self, data, zmlp_app=None):
        self.data = data
        self.zmlp_app = zmlp_app
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
        agg_name = f'{self.agg_prefix}#{self.id}'
        response = {'id': self.id,
                    'results': data['aggregations'][agg_name]}
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
        order = self.options.get('order', 'desc')
        minimum_count = self.options.get('minimum_count')
        size = self.options.get('size', 1000)
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
