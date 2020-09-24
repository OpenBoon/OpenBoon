import pytest

from searches.schemas import (SimilarityAnalysisSchema, ContentAnalysisSchema,
                              LabelsAnalysisSchema, SingleLabelAnalysisSchema)


@pytest.fixture
def mock_response():
    return {
        "index": {
            "mappings": {
                "properties": {
                    "analysis": {
                        "dynamic": "true",
                        "properties": {
                            "zvi": {
                                "properties": {
                                    "tinyProxy": {
                                        "type": "text",
                                        "fields": {
                                            "keyword": {
                                                "type": "keyword",
                                                "ignore_above": 256
                                            }
                                        }
                                    }
                                }
                            },
                            "zvi-image-similarity": {
                                "properties": {
                                    "simhash": {
                                        "type": "keyword",
                                        "index": False
                                    },
                                    "type": {
                                        "type": "text",
                                        "fields": {
                                            "keyword": {
                                                "type": "keyword",
                                                "ignore_above": 256
                                            }
                                        }
                                    }
                                }
                            },
                            "zvi-text-detection": {
                                "properties": {
                                    "words": {
                                        "type": "long",
                                    },
                                    "type": {
                                        "type": "text",
                                        "fields": {
                                            "keyword": {
                                                "type": "keyword",
                                                "ignore_above": 256
                                            }
                                        }
                                    },
                                    "content": {
                                        "type": "keyword",
                                    }
                                }
                            },
                            "zvi-label-detection": {
                                "properties": {
                                    "count": {
                                        "type": "long",
                                    },
                                    "type": {
                                        "type": "text",
                                        "fields": {
                                            "keyword": {
                                                "type": "keyword",
                                                "ignore_above": 256
                                            }
                                        }
                                    },
                                    'predictions': {
                                        'properties': {
                                            'label': {
                                                'type': 'keyword',
                                                'fields': {
                                                    'fulltext': {
                                                        'type': 'text'}}},
                                            'score': {
                                                'type': 'float',
                                                'coerce': True}}}
                                }
                            },
                            "zvi-content-moderation": {
                                "properties": {
                                    "safe": {
                                        "type": "boolean",
                                    },
                                    "type": {
                                        "type": "text",
                                        "fields": {
                                            "keyword": {
                                                "type": "keyword",
                                                "ignore_above": 256
                                            }
                                        }
                                    },
                                    'predictions': {
                                         'properties': {
                                             'label': {
                                                 'type': 'keyword',
                                                 'fields': {
                                                     'fulltext': {
                                                         'type': 'text'}}},
                                             'score': {
                                                 'type': 'float',
                                                 'coerce': True}}}
                                }
                            },
                            'setting': {'properties': {'label': {'type': 'text',
                                                                 'fields': {
                                                                     'keyword': {'type': 'keyword',
                                                                                 'ignore_above': 256}}},
                                                       'score': {'type': 'float'},
                                                       'type': {'type': 'text',
                                                                'fields': {
                                                                    'keyword': {'type': 'keyword',
                                                                                'ignore_above': 256}}}}},
                        }
                    }
                }
            },
        }
    }


class TestSimilarityAnalysisSchema:

    @pytest.fixture
    def sim_data(self, mock_response):
        return mock_response['index']['mappings']['properties']['analysis']['properties']['zvi-image-similarity']  # noqa

    @pytest.fixture
    def schema(self, sim_data):
        return SimilarityAnalysisSchema('zvi-image-similarity', sim_data['properties'])

    def test_is_valid(self, schema):
        assert schema.is_valid()

    def test_get_field_type(self, schema):
        rep = schema.get_field_type_representation()
        assert rep == {'zvi-image-similarity': {'fieldType': 'similarity'}}


class TestContentAnalysisSchema:

    @pytest.fixture
    def content_data(self, mock_response):
        return mock_response['index']['mappings']['properties']['analysis']['properties']['zvi-text-detection']  # noqa

    @pytest.fixture
    def schema(self, content_data):
        return ContentAnalysisSchema('zvi-text-detection', content_data['properties'])

    def test_is_valid(self, schema):
        assert schema.is_valid()

    def test_get_field_type(self, schema):
        rep = schema.get_field_type_representation()
        assert rep == {'zvi-text-detection': {'fieldType': 'text_content'}}


class TestLabelsAnalysisSchema:

    @pytest.fixture
    def count_data(self, mock_response):
        return mock_response['index']['mappings']['properties']['analysis']['properties']['zvi-label-detection']  # noqa

    @pytest.fixture
    def safe_data(self, mock_response):
        return mock_response['index']['mappings']['properties']['analysis']['properties']['zvi-content-moderation']  # noqa

    @pytest.fixture
    def count_schema(self, count_data):
        return LabelsAnalysisSchema('zvi-label-detection', count_data['properties'])

    @pytest.fixture
    def safe_schema(self, safe_data):
        return LabelsAnalysisSchema('zvi-content-moderation', safe_data['properties'])

    def test_is_valid_count(self, count_schema):
        assert count_schema.is_valid()

    def test_is_valid_safe(self, safe_schema):
        assert safe_schema.is_valid()

    def test_get_field_type_with_count(self, count_schema):
        rep = count_schema.get_field_type_representation()
        assert rep == {'zvi-label-detection': {'fieldType': 'prediction'}}

    def test_get_field_type_with_safe(self, safe_schema):
        rep = safe_schema.get_field_type_representation()
        assert rep == {'zvi-content-moderation': {'fieldType': 'prediction'}}


class TestSingleLabelAnalysisSchema:

    @pytest.fixture
    def label_data(self, mock_response):
        return mock_response['index']['mappings']['properties']['analysis']['properties']['setting']

    @pytest.fixture
    def label_schema(self, label_data):
        return SingleLabelAnalysisSchema('setting', label_data['properties'])

    def test_is_valid(self, label_schema):
        assert label_schema.is_valid()

    def test_get_field_type(self, label_schema):
        rep = label_schema.get_field_type_representation()
        assert rep == {'setting': {'fieldType': 'single_label'}}
