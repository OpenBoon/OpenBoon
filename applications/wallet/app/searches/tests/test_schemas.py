import pytest

from searches.schemas import (SimilarityAnalysisSchema, ContentAnalysisSchema,
                              LabelsAnalysisSchema)


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
                            "zvi-label-detection-incomplete": {
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
                                    }
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
    def incomplete_data(self, mock_response):
        return mock_response['index']['mappings']['properties']['analysis']['properties']['zvi-label-detection-incomplete']  # noqa

    @pytest.fixture
    def count_schema(self, count_data):
        return LabelsAnalysisSchema('zvi-label-detection', count_data['properties'])

    @pytest.fixture
    def incomplete_schema(self, incomplete_data):
        return LabelsAnalysisSchema('zvi-label-detection-incomplete', incomplete_data['properties'])

    def test_is_valid_count(self, count_schema):
        assert count_schema.is_valid()

    def test_is_valid_incomplete(self, incomplete_schema):
        assert incomplete_schema.is_valid()

    def test_get_field_type_with_count(self, count_schema):
        rep = count_schema.get_field_type_representation()
        assert rep == {'zvi-label-detection': {'fieldType': 'prediction'}}

    def test_get_field_type_with_incomplete(self, incomplete_schema):
        rep = incomplete_schema.get_field_type_representation()
        assert rep == {'zvi-label-detection-incomplete': {'fieldType': 'prediction'}}
