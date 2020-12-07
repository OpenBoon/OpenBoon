import unittest

from zmlp import Asset, DataSource
from zmlp.client import SearchResult, to_json


class TestClientFunctions(unittest.TestCase):

    def test_to_json(self):
        asset = Asset({"id": "abc123", "document": {"foo": "bar"}})
        value = to_json(asset)
        assert "{\"id\": \"abc123\", \"uri\": null, " \
               "\"document\": {\"foo\": \"bar\"}, \"page\": null}" == value


class SearchResultTests(unittest.TestCase):

    def test_search_result(self):
        search_result = {
            "list": [
                {"id": "abc123", "name": "cats"}
            ],
            "page": {
                "size": 1,
                "totalCount": 55,
                "from": 10
            }
        }
        sr = SearchResult(search_result, DataSource)
        assert sr.size == 1
        assert sr.total == 55
        assert sr.offset == 10
        assert sr[0].id == "abc123"
        assert len(list(sr)) == 1
