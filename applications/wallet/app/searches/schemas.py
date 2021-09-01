from searches.filters import (RangeFilter, ExistsFilter, FacetFilter,
                              LabelConfidenceFilter, TextContentFilter,
                              SimilarityFilter, DateFilter, LabelFilter,
                              PredictionCountFilter, SimpleSortFilter)

# Applicable filter sets for an ES Field type
NUMBER_FILTERS = [RangeFilter.type, ExistsFilter.type, SimpleSortFilter.type]
KEYWORD_FILTERS = [FacetFilter.type, ExistsFilter.type, SimpleSortFilter.type]
SIMILARITY_FILTERS = [ExistsFilter.type, SimilarityFilter.type]
BOOLEAN_FILTERS = [ExistsFilter.type]
DEFAULT_FILTERS = [ExistsFilter.type]
TEXT_FILTERS = [ExistsFilter.type, SimpleSortFilter.type]
PREDICTION_FILTERS = [LabelConfidenceFilter.type, PredictionCountFilter.type, ExistsFilter.type]
TEXT_CONTENT_FILTERS = [TextContentFilter.type, ExistsFilter.type]
DATE_FILTERS = [ExistsFilter.type, DateFilter.type, SimpleSortFilter.type]
LABEL_FILTERS = [LabelFilter.type]


FIELD_TYPE_FILTER_MAPPING = {
    'integer': NUMBER_FILTERS,
    'keyword': KEYWORD_FILTERS,
    'text': TEXT_FILTERS,
    'object': DEFAULT_FILTERS,
    'double': NUMBER_FILTERS,
    'geo_point': DEFAULT_FILTERS,
    'float': NUMBER_FILTERS,
    'date': DATE_FILTERS,
    'nested': DEFAULT_FILTERS,
    'long': NUMBER_FILTERS,
    'similarity': SIMILARITY_FILTERS,
    'text_content': TEXT_CONTENT_FILTERS,
    'prediction': PREDICTION_FILTERS,
    'label': LABEL_FILTERS,
    'join': DEFAULT_FILTERS,
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

    def get_field_type_representation(self):
        """Returns the field type for a given schema"""
        raise NotImplementedError()


class SimilarityAnalysisSchema(AbstractAnalysisSchema):

    required_properties = ['type', 'simhash']

    def get_field_type_representation(self):
        return {f'{self.property_name}': {'fieldType': 'similarity'}}


class ContentAnalysisSchema(AbstractAnalysisSchema):

    required_properties = ['type', 'words', 'content']

    def get_field_type_representation(self):
        return {f'{self.property_name}': {'fieldType': 'text_content'}}


class LabelsAnalysisSchema(AbstractAnalysisSchema):

    required_properties = ['type', 'count']

    def get_field_type_representation(self):
        return {f'{self.property_name}': {'fieldType': 'prediction'}}
