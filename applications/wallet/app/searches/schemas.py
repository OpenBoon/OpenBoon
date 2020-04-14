# Applicable filter sets for an ES Field type
NUMBER_FILTERS = ['range', 'exists']
STRING_FILTERS = ['facet', 'text', 'exists']
SIMILARITY_FILTERS = ['similarity', 'exists']
BOOLEAN_FILTERS = ['boolean', 'exists']
DEFAULT_FILTERS = ['exists']


TYPE_FIELD_MAPPING = {
    'integer': NUMBER_FILTERS,
    'keyword': STRING_FILTERS,
    'text': STRING_FILTERS,
    'object': DEFAULT_FILTERS,
    'double': NUMBER_FILTERS,
    'geo_point': DEFAULT_FILTERS,
    'float': NUMBER_FILTERS,
    'date': DEFAULT_FILTERS,
    'nested': DEFAULT_FILTERS,
    'long': NUMBER_FILTERS,
}


class AbstractAnalysisSchema(object):
    """Custom Serializers to identify Analysis schemas and return their appropriate filters.

    The `required_properties` c-var identifies which properties should exist to indicate
    a matching schema.
    """

    required_properties = []

    def __init__(self, property_name, child_properties):
        self.property_name = property_name
        self.child_properties = child_properties

    def is_valid(self):
        """Identifies whether this schema is valid for the given structure."""
        for key in self.required_properties:
            if key not in self.child_properties:
                return False
        return True

    def get_representation(self):
        """Returns the filterable fields with their appropriate filter list."""
        raise NotImplementedError()


class SimilarityAnalysisSchema(AbstractAnalysisSchema):

    required_properties = ['type', 'simhash']

    def get_representation(self):
        return {f'{self.property_name}': {'simhash': SIMILARITY_FILTERS}}


class ContentAnalysisSchema(AbstractAnalysisSchema):

    required_properties = ['type', 'count', 'content']

    def get_representation(self):
        return {f'{self.property_name}': {'content': STRING_FILTERS,
                                          'count': NUMBER_FILTERS}}


class LabelsAnalysisSchema(AbstractAnalysisSchema):

    required_properties = ['type', 'predictions']

    def get_representation(self):
        # Not sure how to filter predictions yet, so return the empty string for now
        repr = {f'{self.property_name}': {'predictions': []}}
        if 'count' in self.child_properties:
            repr[f'{self.property_name}']['count'] = NUMBER_FILTERS
        if 'safe' in self.child_properties:
            repr[f'{self.property_name}']['safe'] = BOOLEAN_FILTERS
        return repr
