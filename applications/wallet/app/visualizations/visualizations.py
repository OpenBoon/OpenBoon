from rest_framework.exceptions import ValidationError


class BaseVisualization(object):
    """Abstract Visualization object all concrete Visualizations should inherit from.


    """

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
        return self.data['id']

    @property
    def options(self):
        return self.data.get('options', {})

    def is_valid(self, raise_exception=False):
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
        agg_name = f'{self.agg_prefix}#{self.id}'
        response = {'id': self.id,
                    'results': data['aggregations'][agg_name]}
        return response

    def get_es_agg(self):
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
