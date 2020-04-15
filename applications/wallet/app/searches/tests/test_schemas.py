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
                                    "predictions": {
                                        "type": "nested",
                                    }
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
                                    "predictions": {
                                        "type": "nested",
                                    }
                                }
                            }
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

    def test_get_representation(self, schema):
        rep = schema.get_representation()
        assert rep == {'zvi-image-similarity': {'simhash': ['similarity', 'exists']}}


class TestContentAnalysisSchema:

    @pytest.fixture
    def content_data(self, mock_response):
        return mock_response['index']['mappings']['properties']['analysis']['properties']['zvi-text-detection']  # noqa

    @pytest.fixture
    def schema(self, content_data):
        return ContentAnalysisSchema('zvi-text-detection', content_data['properties'])

    def test_is_valid(self, schema):
        assert schema.is_valid()

    def test_get_represention(self, schema):
        rep = schema.get_representation()
        assert rep == {'zvi-text-detection': {'content': ['facet', 'text', 'exists'],
                                              'count': ['range', 'exists']}}


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

    def test_get_represention_with_count(self, count_schema):
        rep = count_schema.get_representation()
        assert rep == {'zvi-label-detection': {'predictions': [], 'count': ['range', 'exists']}}

    def test_get_representation_with_safe(self, safe_schema):
        rep = safe_schema.get_representation()
        assert rep == {'zvi-content-moderation': {'predictions': [], 'safe': ['boolean', 'exists']}}
