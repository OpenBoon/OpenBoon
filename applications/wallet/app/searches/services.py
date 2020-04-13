from searches.schemas import (SimilarityAnalysisSchema, ContentAnalysisSchema,
                              LabelsAnalysisSchema, TYPE_FIELD_MAPPING)

ANALYSIS_SCHEMAS = [SimilarityAnalysisSchema, ContentAnalysisSchema, LabelsAnalysisSchema]


class FieldService(object):

    def get_fields_from_mappings(self, mappings):
        """Converts an ES Indexes mappings response into a field:filter map."""
        fields = {}
        properties = mappings['properties']
        for property in properties:
            fields.update(self.get_filters_for_child_fields(property, properties[property]))
        return fields

    def get_filters_for_child_fields(self, property_name, values):
        """Recursively build the dict structure for an attribute and retrieve it's filters.

        Returns:
            list<dict>: A list of dicts where the key is the attr dot path, and the
            value is the type
        """
        fields = {property_name: {}}
        if 'type' in values:
            # May need to add an override for type on similarity hash fields
            return {property_name: TYPE_FIELD_MAPPING[values['type']]}

        if 'properties' in values:
            child_properties = values['properties']

            # Identify special Analysis Schemas
            if 'type' in child_properties:
                schema = self.get_analysis_schema(property_name, child_properties)
                if schema:
                    return schema

            for property in child_properties:
                fields[property_name].update(
                    self.get_filters_for_child_fields(property, child_properties[property]))

        return fields

    def get_analysis_schema(self, property_name, child_properties):
        """Return the special schema for a ZMLP Analysis Schema"""
        for Klass in ANALYSIS_SCHEMAS:
            schema = Klass(property_name, child_properties)
            if schema.is_valid():
                return schema.get_representation()

        return None
