from rest_framework.exceptions import ValidationError


class BaseVisualization(object):
    """Abstract Visualization object all concrete Visualizations should inherit from.


    """

    type = None
    required_keys = []
    required_option_keys = []

    def __init__(self, data, zmlp_app=None):
        self.data = data
        self.zmlp_app = zmlp_app
        self.errors = []

    def __eq__(self, other):
        if type(self) == type(other) and self.data == other.data:
            return True
        return False

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
        return data

    def get_query(self):
        raise NotImplementedError()


class RangeVisualization(BaseVisualization):

    type = 'range'
    required_keys = ['id', 'attribute']

    def get_query(self):
        query = {}

