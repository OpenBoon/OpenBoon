import logging
import unittest
import os
from unittest.mock import patch

import pytest

from boonsdk import BoonClient, app_from_env, Asset, \
    SimilarityQuery, LabelConfidenceQuery, \
    FaceSimilarityQuery, AssetSearchCsvExporter, AssetSearchScroller, \
    AssetSearchResult, VideoClipContentMatchQuery

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class AssetSearchScrollerTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()
        self.mock_search_result = mock_search_result

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_iterate(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]
        del_patch.return_value = {}

        scroller = AssetSearchScroller(self.app,
                                       {"query": {"term": {"source.filename": "dog.jpg"}}})
        results = list(scroller)
        assert 2 == len(results)

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_iterate_batch(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]
        del_patch.return_value = {}

        scroller = AssetSearchScroller(self.app,
                                       {"query": {"term": {"source.filename": "dog.jpg"}}})
        results = list(scroller.batches_of(2))
        assert 1 == len(results)

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_iterate_raw_response(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]
        del_patch.return_value = {}

        scroller = AssetSearchScroller(self.app,
                                       {"query": {"term": {"source.filename": "dog.jpg"}}},
                                       raw_response=True)
        results = list(scroller)
        assert results[0] == self.mock_search_result

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_search_es_object(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]
        del_patch.return_value = {}

        scroller = AssetSearchScroller(self.app,
                                       MockEsDslSearch(),
                                       raw_response=True)
        assert scroller.search == MockEsDslSearch().to_dict()
        results = list(scroller)
        assert results[0] == self.mock_search_result


class AssetSearchCsvExporterTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()
        self.mock_search_result = mock_search_result

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_export(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]
        del_patch.return_value = {}

        path = "/tmp/exported.csv"
        try:
            os.unlink(path)
        except FileNotFoundError:
            pass

        exporter = AssetSearchCsvExporter(self.app, {})
        fields = [
            "source.path",
            "analysis.boonai.similarity.vector"
        ]
        assert 2 == exporter.export(fields, "/tmp/exported.csv")

        with open(path, "r") as fp:
            contents = fp.read()

        assert 'https://i.imgur.com/SSN26nN.jpg' in contents
        assert 'https://i.imgur.com/foo.jpg' in contents


class AssetSearchResultTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()
        self.mock_search_result = mock_search_result

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_properties(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]
        del_patch.return_value = {}

        results = AssetSearchResult(self.app, {"query": {"term": {"source.filename": "dog.jpg"}}})
        assert 2 == len(results.assets)
        assert 2 == results.size
        assert 100 == results.total_size
        assert results.raw_response == mock_search_result

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_next_page(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]
        del_patch.return_value = {}

        results = AssetSearchResult(self.app, {"query": {"term": {"source.filename": "dog.jpg"}}})
        next_page = results.next_page()
        assert next_page.raw_response == {"hits": {"hits": []}}

    @patch.object(BoonClient, 'post')
    def test_batches_of(self, post_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]

        results = AssetSearchResult(self.app, {"query": {"term": {"source.filename": "dog.jpg"}}})
        asserted = False
        for batch in results.batches_of(2):
            assert len(batch) == 2
            asserted = True
        assert asserted

    @patch.object(BoonClient, 'post')
    def test_batches_of_with_max(self, post_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]

        results = AssetSearchResult(self.app, {"query": {"term": {"source.filename": "dog.jpg"}}})
        asserted = False
        for batch in results.batches_of(2, max_assets=1):
            assert len(batch) == 1
            asserted = True
        assert asserted

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_aggegation(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result]
        del_patch.return_value = {}

        results = AssetSearchResult(self.app, {})
        agg = results.aggregation("file_types")
        assert 1 == agg["buckets"][0]["doc_count"]
        assert "jpg" == agg["buckets"][0]["key"]

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_aggegation_error_not_exist(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result]
        del_patch.return_value = {}

        results = AssetSearchResult(self.app, {})
        assert results.aggregation("bob") is None

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_aggegation_error_multiple(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result]
        del_patch.return_value = {}

        results = AssetSearchResult(self.app, {})
        with pytest.raises(ValueError):
            results.aggregation("dogs")

    @patch.object(BoonClient, 'delete')
    @patch.object(BoonClient, 'post')
    def test_aggegation_fully_qualified(self, post_patch, del_patch):
        post_patch.side_effect = [self.mock_search_result]
        del_patch.return_value = {}

        results = AssetSearchResult(self.app, {})
        assert results.aggregation("sterm#dogs") is not None

    @patch.object(BoonClient, 'post')
    def test_search_es_object(self, post_patch):
        post_patch.side_effect = [self.mock_search_result, {"hits": {"hits": []}}]

        search = AssetSearchScroller(self.app,
                                     MockEsDslSearch(),
                                     raw_response=True)
        assert search.search == MockEsDslSearch().to_dict()


class TestLabelConfidenceQuery(unittest.TestCase):
    def test_for_json(self):
        s = LabelConfidenceQuery("foo", "dog", 0.5)
        qjson = s.for_json()

        assert qjson['bool']['filter'][0]['terms']['analysis.foo.predictions.label'] == ['dog']
        nested = qjson['bool']['must'][0]['nested']
        assert nested['path'] == 'analysis.foo.predictions'


class TestVideoClipContentMatchQuery(unittest.TestCase):
    def test_for_json(self):
        s = VideoClipContentMatchQuery("foo", 0.1, 0.5)
        qjson = s.for_json()
        function_score = qjson['bool']['must'][0]['function_score']
        assert function_score['field_value_factor']['field'] == 'clip.score'


class TestImageSimilarityQuery(unittest.TestCase):
    def test_for_json(self):
        s = SimilarityQuery("ABC123", 0.50, boost=5.0, field="foo.vector")
        qjson = s.for_json()

        params = qjson["script_score"]["script"]["params"]
        assert 0.50 == params["minScore"]
        assert "foo.vector" == params["field"]
        assert "ABC123" in params["hashes"]

    def test_add_asset(self):
        asset = Asset({"id": "123"})
        asset.set_attr("foo.vector", "OVER9000")

        s = SimilarityQuery(None, 0.50, field="foo.vector")
        s.add_hash(asset)
        assert ['OVER9000'] == s.hashes

    def test_plus_asset(self):
        asset = Asset({"id": "123"})
        asset.set_attr("foo.vector", "OVER9000")

        s = SimilarityQuery(None, 0.50, field="foo.vector")
        s = s + asset
        assert ['OVER9000'] == s.hashes

    def test_plus_hash(self):
        s = SimilarityQuery(None, 0.50, field="foo.vector")
        s = s + "OVER9000"
        assert ['OVER9000'] == s.hashes

    def test_add_hash(self):
        s = SimilarityQuery(None, 0.50, field="foo.vector")
        s.add_hash("OVER9000")
        assert ['OVER9000'] == s.hashes
        assert "foo.vector" == s.field
        assert 0.50 == s.min_score


class TestFaceSimilarityQuery(unittest.TestCase):

    def test_create_with_hash(self):
        query = FaceSimilarityQuery("abc123")
        esq = query.for_json()
        print(esq)

        params = esq['script_score']['script']['params']
        assert 'analysis.boonai-face-detection.predictions.simhash' == params['field']
        assert ['abc123'] == params['hashes']
        assert 0.90 == params['minScore']

    def test_create_with_prediction(self):
        pred = [
            {"simhash": "QWERTY"}
        ]
        esq = FaceSimilarityQuery(pred, min_score=1.0).for_json()

        params = esq['script_score']['script']['params']
        assert 'analysis.boonai-face-detection.predictions.simhash' == params['field']
        assert ['QWERTY'] == params['hashes']
        assert 1.0 == params['minScore']


class MockEsDslSearch:
    """Mock ElasticSearch DSL search class."""
    def to_dict(self):
        return {"query": {"term": {"foo": {"bar"}}}}


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
                    },
                    "analysis": {
                        "boonai": {
                            "similarity": {
                                "vector": "OVER9000"
                            }
                        }
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
