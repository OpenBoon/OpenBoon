# Applicable filter sets for an ES Field type
NUMBER_FILTERS = ['range', 'exists']
KEYWORD_FILTERS = ['facet', 'exists']
SIMILARITY_FILTERS = ['exists']
BOOLEAN_FILTERS = ['boolean', 'exists']
DEFAULT_FILTERS = ['exists']
TEXT_FILTERS = ['exists']


TYPE_FIELD_MAPPING = {
    'integer': NUMBER_FILTERS,
    'keyword': KEYWORD_FILTERS,
    'text': TEXT_FILTERS,
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
        """Identifies whether this schema is valid for the given structure.

        Supports dot-path notation for checking for nested attributes in the mapping.
        """
        for key in self.required_properties:
            # Convert dot path key names to their actual mapping dot path, if exist
            real_dot_path = '.properties.'.join(key.split('.'))
            current = self.child_properties
            for step in real_dot_path.split('.'):
                if step in current:
                    current = current[step]
                else:
                    return False

        return True

    def get_representation(self):
        """Returns the filterable fields with their appropriate filter list."""
        raise NotImplementedError()


class SimilarityAnalysisSchema(AbstractAnalysisSchema):

    required_properties = ['type', 'simhash']

    def get_representation(self):
        return {f'{self.property_name}': {'simhash': DEFAULT_FILTERS}}


class ContentAnalysisSchema(AbstractAnalysisSchema):

    required_properties = ['type', 'words', 'content']

    def get_representation(self):
        return {f'{self.property_name}': {'content': TEXT_FILTERS,
                                          'words': NUMBER_FILTERS}}


class LabelsAnalysisSchema(AbstractAnalysisSchema):

    required_properties = ['type', 'predictions.label', 'predictions.score']

    def get_representation(self):
        return {f'{self.property_name}': ['labelConfidence']}
