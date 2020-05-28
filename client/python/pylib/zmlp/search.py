import copy

from .entity import Asset, ZmlpException
from .util import as_collection

__all__ = [
    'AssetSearchScroller',
    'AssetSearchResult',
    'LabelConfidenceQuery',
    'SimilarityQuery'
]


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
        if search and getattr(search, "to_dict", None):
            search = search.to_dict()
        self.search = copy.deepcopy(search or {})
        self.timeout = timeout
        self.raw_response = raw_response

    def batches_of(self, batch_size=50):
        """
        A generator function capable of efficiently scrolling through
        large numbers of assets, returning them in batches of
        the given batch size.

        Args:
            batch_size (int): The size of the batch.

        Returns:
            generator: A generator that yields batches of Assets.

        """
        batch = []
        for asset in self.scroll():
            batch.append(asset)
            if len(batch) >= batch_size:
                yield batch
                batch = []
        if batch:
            yield batch

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
                        yield Asset({'id': hit['_id'],
                                     'document': hit['_source'],
                                     'score': hit['_score']})

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
        if search and getattr(search, "to_dict", None):
            search = search.to_dict()
        self.search = search
        self.result = None

        self._execute_search()

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
        return [Asset.from_hit(hit) for hit in hits['hits']]

    def batches_of(self, batch_size, max_assets=None):
        """
        A generator function which returns batches of assets in the
        given batch size.  This method will optionally page through
        N pages, yielding arrays of assets as it goes.

        This method is preferred to scrolling for Assets when
        multiple pages of Assets need to be processed.

        Args:
            batch_size (int): The size of the batch.
            max_assets (int): The max number of assets to return, max is 10k

        Returns:
            generator: A generator that yields batches of Assets.

        """
        # The maximum we can page through is 10k
        asset_countdown = max_assets or 10000

        batch = []
        while True:
            assets = self.assets
            if not assets:
                break

            for asset in assets:
                batch.append(asset)
                asset_countdown -= 1
                if asset_countdown <= 0:
                    break
                if len(batch) >= batch_size:
                    yield batch
                    batch = []

            if asset_countdown <= 0:
                break

            self.search['from'] = self.search.get('from', 0) + len(assets)
            self._execute_search()

        if batch:
            yield batch

    def aggregation(self, name):
        """
        Return an aggregation dict with the given name.

        Args:
            name (str): The agg name

        Returns:
            dict: the agg dict or None if no agg exists.
        """
        aggs = self.result.get("aggregations")
        if not aggs:
            return None

        if "#" in name:
            key = [name]
        else:
            key = [k for k in
                   self.result.get("aggregations", {}) if k.endswith("#{}".format(name))]

        if len(key) > 1:
            raise ValueError(
                "Aggs with the same name must be qualified by type (pick 1):  {}".format(key))
        elif not key:
            return None
        try:
            return aggs[key[0]]
        except KeyError:
            return None

    def aggregations(self):
        """
        Return a dictionary of all aggregations.

        Returns:
            dict: A dict of aggregations keyed on name.
        """
        return self.result.get("aggregations", {})

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
        search['from'] = search.get('from', 0) + len(self.result.get("hits"))
        return AssetSearchResult(self.app, search)

    def _execute_search(self):
        self.result = self.app.client.post("api/v3/assets/_search", self.search)

    def __iter__(self):
        return iter(self.assets)

    def __getitem__(self, item):
        return self.assets[item]


class LabelConfidenceQuery(object):
    """
    A helper class for building a label confidence score query.  This query must point
    at label confidence structure:  For example: analysis.zvi.label-detection.

    References:
        "labels": [
                {"label": "dog", "score": 0.97 },
                {"label": "fox", "score": 0.63 }
        ]
    """

    def __init__(self, namespace, labels, min_score, max_score=1.0):
        """
        Create a new LabelConfidenceScoreQuery.

        Args:
            namespace (str): The analysis namespace with predications. (zvi-label-detection)
            labels (list): A list of labels to filter.
            min_score (float): The minimum label score.
            max_score (float): The maximum score, defaults to 1.0 which is highest
        """
        self.namespace = namespace
        self.field = "analysis.{}.predictions".format(namespace)
        self.labels = as_collection(labels)
        self.score = [min_score, max_score]

    def for_json(self):
        return {
            "script_score": {
                "query": {
                    "terms": {
                        self.field + ".label": self.labels
                    }
                },
                "script": {
                    "source": "kwconf",
                    "lang": "zorroa-kwconf",
                    "params": {
                        "field": self.field,
                        "labels": self.labels,
                        "range": self.score
                    }
                },
                "min_score": self.score[0]
            }
        }


class SimilarityQuery:
    """
    A helper class for building a similarity search.  You can embed this class anywhere
    in a ES query dict, for example:

    References:
        {
            "query": {
                "bool": {
                    "must": [
                        SimilarityQuery(hash_string)
                    ]
                }
            }
        }
    """
    def __init__(self, hashes, min_score=0.75, boost=1.0,
                 field="analysis.zvi-image-similarity.simhash"):
        self.hashes = []
        self.add_hash(hashes)
        self.min_score = min_score
        self.boost = boost
        self.field = field

    def add_hash(self, hashes):
        """
        Add a new hash to the search.

        Args:
            hashes (mixed): A similarity hash string or an asset.

        Returns:
            SimilarityQuery: this instance of SimilarityQuery
        """
        for simhash in as_collection(hashes) or []:
            if isinstance(simhash, Asset):
                self.hashes.append(simhash.get_attr(self.field))
            else:
                self.hashes.append(simhash)
        return self

    def add_asset(self, asset):
        """
        See add_hash which handles both hashes and Assets.
        """
        return self.add_hash(asset)

    def for_json(self):
        return {
            "script_score": {
                "query": {
                    "match_all": {}
                },
                "script": {
                    "source": "similarity",
                    "lang": "zorroa-similarity",
                    "params": {
                        "minScore": self.min_score,
                        "field": self.field,
                        "hashes":  self.hashes
                    }
                },
                "boost": self.boost,
                "min_score": self.min_score
            }
        }

    def __add__(self, simhash):
        self.add_hash(simhash)
        return self
