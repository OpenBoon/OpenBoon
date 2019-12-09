import unittest

from pixml import Asset
from pixml.datasource import DataSource
from pixml.rest import SearchResult


class SearchResultTests(unittest.TestCase):

    def test_search_result_asset(self):
        search_result = {
            "took": 4,
            "timed_out": False,
            "hits": {
                "total": 100,
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
                    }
                ]
            }
        }
        sr = SearchResult(search_result, Asset)
        assert sr.size == 1
        assert sr.total == 100
        assert sr.offset == 0
        assert sr[0].id == "dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg"
        assert sr[0].get_attr("source.path") == "https://i.imgur.com/SSN26nN.jpg"
        assert len(list(sr)) == 1

    def test_search_result_other(self):
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
