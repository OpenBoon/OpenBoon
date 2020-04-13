import pytest

from searches.services import FieldService


class TestFieldService:
    field_service = FieldService()

    @pytest.fixture
    def similarity_mapping(self):
        return {
            "mappings": {"properties": {"analysis": {"dynamic": "true", "properties": {
                "zvi": {
                "properties": {"tinyProxy": {"type": "text", "fields": {
                    "keyword": {"type": "keyword", "ignore_above": 256}}}}},
                "zvi-image-similarity": {
                    "properties": {"simhash": {"type": "keyword", "index": False},
                                   "type": {"type": "text",
                                            "fields": {
                                                "keyword": {
                                                    "type": "keyword",
                                                                                            "ignore_above": 256}}}}}}}}}}  # noqa

    def test_converts_similarity_blob(self, similarity_mapping):
        properties = similarity_mapping['mappings']['properties']
        result = self.field_service.get_fields_from_mappings(properties['analysis'])
        assert result['zvi-image-similarity'] == {'simhash': ['similarity', 'exists']}
