import logging
import unittest
from unittest.mock import patch

import pytest

from zmlp import ZmlpClient, app_from_env
from zmlp.search import AssetSearchScroller, AssetSearchResult, SimilarityQuery

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class AssetSearchScrollerTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()
        self.mock_search_result = mock_search_result

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_iterate(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]
        del_patch.return_value = {}

        scroller = AssetSearchScroller(self.app,
                                       {"query": {"term": {"source.filename": "dog.jpg"}}})
        results = list(scroller)
        assert 2 == len(results)

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_iterate_raw_response(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]
        del_patch.return_value = {}

        scroller = AssetSearchScroller(self.app,
                                       {"query": {"term": {"source.filename": "dog.jpg"}}},
                                       raw_response=True)
        results = list(scroller)
        assert results[0] == self.mock_search_result


class AssetSearchResultTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()
        self.mock_search_result = mock_search_result

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_properties(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]
        del_patch.return_value = {}

        results = AssetSearchResult(self.app, {"query": {"term": {"source.filename": "dog.jpg"}}})
        assert 2 == len(results.assets)
        assert 2 == results.size
        assert 100 == results.total_size
        assert results.raw_response == mock_search_result

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_next_page(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]
        del_patch.return_value = {}

        results = AssetSearchResult(self.app, {"query": {"term": {"source.filename": "dog.jpg"}}})
        next_page = results.next_page()
        assert next_page.raw_response == {"hits": {"hits": []}}

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_aggegation(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result]
        del_patch.return_value = {}

        results = AssetSearchResult(self.app, {})
        agg = results.aggregation("file_types")
        print(agg)
        assert 1 == agg["buckets"][0]["doc_count"]
        assert "jpg" == agg["buckets"][0]["key"]

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_aggegation_error_not_exist(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result]
        del_patch.return_value = {}

        results = AssetSearchResult(self.app, {})
        assert results.aggregation("bob") is None

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_aggegation_error_multiple(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result]
        del_patch.return_value = {}

        results = AssetSearchResult(self.app, {})
        with pytest.raises(ValueError):
            results.aggregation("dogs")

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_aggegation_fully_qualified(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result]
        del_patch.return_value = {}

        results = AssetSearchResult(self.app, {})
        assert results.aggregation("sterm#dogs") is not None


class TestImageSimilarityQuery(unittest.TestCase):
    def test_for_json(self):
        s = SimilarityQuery("foo.vector", 0.50, "ABC123")
        qjson = s.for_json()

        params = qjson["function_score"]["functions"][0]["script_score"]["script"]["params"]
        assert 0.50 == params["minScore"]
        assert "foo.vector" == params["field"]
        assert "ABC123" in params["hashes"]


mock_search_result = {
    "took": 4,
    "timed_out": False,
    "_scroll_id": "bob",
    "aggregations": {
        "sterm#file_types": {
            "doc_count_error_upper_bound": 0,
            "sum_other_doc_count": 0,
            "buckets": [{"key": "jpg", "doc_count": 1}]
        },
        "sterm#dogs": {},
        "metrics#dogs": {}
    },
    "hits": {
        "total": {"value": 100},
        "max_score": 0.2876821,
        "hits": [
            {
                "_index": "litvqrkus86sna2w",
                "_type": "asset",
                "_id": "dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg",
                "_score": 0.2876821,
                "_source": {
                    "source": {
                        "path": "https://i.imgur.com/SSN26nN.jpg"
                    }
                }
            },
            {
                "_index": "litvqrkus86sna2w",
                "_type": "asset",
                "_id": "aabbccddec48n1q1fginVMV5yllhRRGx2WKyKLjDphg",
                "_score": 0.2876821,
                "_source": {
                    "source": {
                        "path": "https://i.imgur.com/foo.jpg"
                    }
                }
            }
        ]
    }
}
