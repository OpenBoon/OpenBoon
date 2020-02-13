import copy

from .asset import Asset
from .exception import ZmlpException


class AssetSearchScroller(object):
    """
    The AssetSearchScroller can iterate over large amounts of assets without incurring paging
    overhead by utilizing a server side cursor.  The cursor is held open for the specified
    timeout time unless it is refreshed before the timeout occurs.  In this sense, it's important
    to complete whatever operation you're taking on each asset within the timeout time.  For example
    if your page size is 32 and your timeout is 1m, you have 1 minute to handles 32 assets.  If that
    is not enough time, consider increasing the timeout or lowering your page size.

    """
    def __init__(self, app, search, timeout="1m", raw_response=False):
        """
        Create a new AssetSearchScroller instance.

        Args:
            app (ZmlpApp): A ZmlpApp instance.
            search: (dict): The ES search
            timeout (str): The maximum amount of time the ES scroll will be active unless it's
                refreshed.
            raw_response (bool): Yield the raw ES response rather than assets. The raw
                response will contain the entire page, not individual assets.
        """
        self.app = app
        self.search = copy.deepcopy(search or {})
        self.timeout = timeout
        self.raw_response = raw_response

    def scroll(self):
        """
        A generator function capable of efficiently scrolling through large
        results.

        Examples:
            for asset in AssetSearchScroller({"query": {"term": { "source.extension": "jpg"}}}):
                do_something(asset)

        Yields:
            Asset: Assets that matched the search
        """
        result = self.app.client.post(
            "api/v3/assets/_search?scroll={}".format(self.timeout), self.search)
        scroll_id = result.get("_scroll_id")
        if not scroll_id:
            raise ZmlpException("No scroll ID returned with scroll search, has it timed out?")
        try:
            while True:
                hits = result.get("hits")
                if not hits:
                    return
                if self.raw_response:
                    yield result
                else:
                    for hit in hits['hits']:
                        yield Asset({'id': hit['_id'], 'document': hit['_source']})

                scroll_id = result.get("_scroll_id")
                if not scroll_id:
                    raise ZmlpException(
                        "No scroll ID returned with scroll search, has it timed out?")
                result = self.app.client.post("api/v3/assets/_search/scroll", {
                    "scroll": self.timeout,
                    "scroll_id": scroll_id
                })
                if not result["hits"]["hits"]:
                    return
        finally:
            self.app.client.delete("api/v3/assets/_search/scroll", {
                "scroll_id": scroll_id
            })

    def __iter__(self):
        return self.scroll()


class AssetSearchResult(object):
    """
    Stores a search result from ElasticSearch and provides some convenience methods
    for accessing the data.

    """
    def __init__(self, app, search):
        """
        Create a new AssetSearchResult.

        Args:
            app (ZmlpApp): A ZmlpApp instance.
            search (dict): An ElasticSearch query.
        """
        self.app = app
        self.search = search
        self.result = self.app.client.post("api/v3/assets/_search", self.search)

    @property
    def assets(self):
        """
        A list of assets returned by the query. This is not all of the matches,
        just a single page of results.

        Returns:
            list: The list of assets for this page.

        """
        hits = self.result.get("hits")
        if not hits:
            return []
        return [Asset({'id': hit['_id'], 'document': hit['_source']}) for hit in hits['hits']]

    def agg(self, name):
        raise NotImplementedError("TODO")

    @property
    def size(self):
        """
        The number assets in this page.  See "total_size" for the total number of assets matched.

        Returns:
            int: The number of assets in this page.

        """
        return len(self.result["hits"]["hits"])

    @property
    def total_size(self):
        """
        The total number of assets matched by the query.

        Returns:
            long: The total number of assets matched.

        """
        return self.result["hits"]["total"]["value"]

    @property
    def raw_response(self):
        """
        The raw ES response.
        Returns:
            (dict) The raw SearchResponse returned by ElasticSearch

        """
        return self.result

    def next_page(self):
        """
        Return an AssetSearchResult containing the next page.

        Returns:
            AssetSearchResult: The next page

        """
        search = copy.deepcopy(self.search or {})
        search['from'] = search.get('from', 0) + search.get('size', 32)
        return AssetSearchResult(self.app, search)

    def __iter__(self):
        return iter(self.assets)

    def __getitem__(self, item):
        return self.assets[item]


class SimilarityQuery:
    """
    A helper class for building a similarity search.  You can embed this class anywhere
    in a ES query dict, for example:

    Examples:
        {
            "query": {
                "bool": {
                    "must": [
                        SimilarityQuery("analysis.zmlp.similarity.vector", 0.85, hash_string)
                    ]
                }
            }
        }
    """
    def __init__(self, field, min_score=0.75, *hashes):
        self.field = field
        self.min_score = min_score
        self.hashes = hashes

    def add_hash(self, simhash):
        """
        Add a new hash to the search.

        Args:
            simhash (str): A similarity hash.

        Returns:
            SimilarityQuery: return this instance of SimilarityQuery
        """
        self.hashes.append(simhash)
        return self

    def for_json(self):
        return {
            "function_score": {
                "functions": [
                    {
                        "script_score": {
                            "script": {
                                "source": "similarity",
                                "lang": "zorroa-similarity",
                                "params": {
                                    "minScore": self.min_score,
                                    "field": self.field,
                                    "hashes":  self.hashes
                                }
                            }
                        }
                    }
                ],
                "score_mode": "multiply",
                "boost_mode": "replace",
                "max_boost": 1000,
                "min_score": self.min_score,
                "boost": 1.0
            }
        }
