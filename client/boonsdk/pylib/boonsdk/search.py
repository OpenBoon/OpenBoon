import copy

from .entity import VideoClip, Asset, BoonSdkException
from .util import as_collection

__all__ = [
    'AssetSearchScroller',
    'VideoClipSearchScroller',
    'AssetSearchResult',
    'VideoClipSearchResult',
    'AssetSearchCsvExporter',
    'LabelConfidenceQuery',
    'SingleLabelConfidenceQuery',
    'SimilarityQuery',
    'FaceSimilarityQuery',
    'LabelConfidenceTermsAggregation',
    'LabelConfidenceMetricsAggregation',
    'SearchScroller',
    'VideoClipContentMatchQuery'
]


class SearchScroller:
    """
    The SearchScroller can iterate over large amounts of data without incurring paging
    overhead by utilizing a server side cursor.  The cursor is held open for the specified
    timeout time unless it is refreshed before the timeout occurs.  In this sense, it's important
    to complete whatever operation you're taking on each asset within the timeout time.  For example
    if your page size is 32 and your timeout is 1m, you have 1 minute to handles 32 assets.  If that
    is not enough time, consider increasing the timeout or lowering your page size.
    """
    def __init__(self, klass, endpoint, app, search, timeout="1m", raw_response=False):
        """
        Create a new AbstractSearchScroller instance.

        Args:
            app (BoonApp): A BoonApp instance.
            search: (dict): The ES search
            timeout (str): The maximum amount of time the ES scroll will be active unless it's
                refreshed.
            raw_response (bool): Yield the raw ES response rather than assets. The raw
                response will contain the entire page, not individual assets.
        """
        self.klass = klass
        self.endpoint = endpoint
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
            "{}?scroll={}".format(self.endpoint, self.timeout), self.search)
        scroll_id = result.get("_scroll_id")
        if not scroll_id:
            raise BoonSdkException("No scroll ID returned with scroll search, has it timed out?")
        try:
            while True:
                hits = result.get("hits")
                if not hits:
                    return
                if self.raw_response:
                    yield result
                else:
                    for hit in hits['hits']:
                        yield self.klass.from_hit(hit)

                scroll_id = result.get("_scroll_id")
                if not scroll_id:
                    raise BoonSdkException(
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


class AssetSearchScroller(SearchScroller):
    """
    AssetSearchScroller handles scrolling through Assets.
    """
    def __init__(self, app, search, timeout="1m", raw_response=False):
        super(AssetSearchScroller, self).__init__(
            Asset, 'api/v3/assets/_search', app, search, timeout, raw_response
        )


class VideoClipSearchScroller(SearchScroller):
    """
    VideoClipSearchScroller handles scrolling through video clips.
    """

    def __init__(self, app, search, timeout="1m", raw_response=False):
        super(VideoClipSearchScroller, self).__init__(
            VideoClip, 'api/v1/clips/_search', app, search, timeout, raw_response
        )


class AssetSearchCsvExporter:
    """
    Export a search to a CVS file.
    """
    def __init__(self, app, search):
        self.app = app
        self.search = search

    def export(self, fields, path):
        """
        Export the given fields to a csv file output path.

        Args:
            fields (list): An array of field names.
            path (str): a file path.

        Returns:
            int: The number of assets exported.

        """
        count = 0
        scroller = AssetSearchScroller(self.app, self.search)
        fields = as_collection(fields)
        with open(str(path), "w") as fp:
            for asset in scroller:
                count += 1
                line = ",".join(["'{}'".format(asset.get_attr(field)) for field in fields])
                fp.write(f'{line}\n')
        return count


class SearchResult:
    """
    Stores a search result from ElasticSearch and provides some convenience methods
    for accessing the data.

    """
    def __init__(self, klass, endpoint, app, search):
        """
        Create a new SearchResult.

        Args:
            klass (Class): The Class to wrap the search result.
            endpoint (str): The endpoint to use for search.
            app (BoonApp): A BoonApp instance.
            search (dict): An ElasticSearch query.
        """
        self.klass = klass
        self.endpoint = endpoint
        self.app = app
        if search and getattr(search, "to_dict", None):
            search = search.to_dict()
        self.search = search
        self.result = None

        self._execute_search()

    @property
    def items(self):
        """
        A list of assets returned by the query. This is not all of the matches,
        just a single page of results.

        Returns:
            list: The list of assets for this page.

        """
        hits = self.result.get("hits")
        if not hits:
            return []
        return [self.klass.from_hit(hit) for hit in hits['hits']]

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
        return SearchResult(self.klass, self.endpoint, self.app, search)

    def _execute_search(self):
        self.result = self.app.client.post(self.endpoint, self.search)

    def __iter__(self):
        return iter(self.items)

    def __getitem__(self, item):
        return self.items[item]


class AssetSearchResult(SearchResult):
    """
    The AssetSearchResult subclass handles paging throug an Asset search result.
    """
    def __init__(self, app, search):
        super(AssetSearchResult, self).__init__(
            Asset, 'api/v3/assets/_search', app, search
        )

    @property
    def assets(self):
        return self.items


class VideoClipSearchResult(SearchResult):
    """
    The VideoClipSearchResult subclass handles paging through an VideoClip search result.
    """
    def __init__(self, app, search):
        super(VideoClipSearchResult, self).__init__(
            VideoClip, 'api/v1/clips/_search', app, search
        )

    @property
    def clips(self):
        return self.items


class LabelConfidenceTermsAggregation:
    """
    Convenience class for making a simple terms aggregation on an array of predictions
    """
    def __init__(self, namespace):
        self.field = "analysis.{}.predictions".format(namespace)

    def for_json(self):
        return {
            "nested": {
                "path": self.field
            },
            "aggs": {
                "names": {
                    "terms": {
                        "field": self.field + ".label",
                        "size": 1000,
                        "order": {"_count": "desc"}
                    }
                }
            }
        }


class LabelConfidenceMetricsAggregation(object):

    def __init__(self, namespace, agg_type="stats"):
        """
        Create a new LabelConfidenceMetricsAggregation

        Args:
            namespace (str): The analysis namespace. (ex: boonai-label-detection)
            agg_type (str): A type of metrics agg to perform.
                stats, extended_stats,
        """
        self.field = "analysis.{}.predictions".format(namespace)
        self.agg_type = agg_type

    def for_json(self):
        return {
            "nested": {
                "path": self.field
            },
            "aggs": {
                "labels": {
                    "terms": {
                        "field": self.field + ".label",
                        "size": 1000,
                        "order": {"_count": "desc"}
                    },
                    "aggs": {
                        "stats": {
                            self.agg_type: {
                                "field": self.field + ".score"
                            }
                        }
                    }
                }
            }
        }


class LabelConfidenceQuery(object):
    """
    A helper class for building a label confidence score query.  This query must point
    at label confidence structure:  For example: analysis.boonai.label-detection.

    References:
        "labels": [
                {"label": "dog", "score": 0.97 },
                {"label": "fox", "score": 0.63 }
        ]
    """

    def __init__(self, namespace, labels, min_score=0.1, max_score=1.0):
        """
        Create a new LabelConfidenceScoreQuery.

        Args:
            namespace (str): The analysis namespace with predictions. (ex: boonai-label-detection)
            labels (list): A list of labels to filter.
            min_score (float): The minimum label score, default to 0.1.
            Note that 0.0 allows everything.
            max_score (float): The maximum score, defaults to 1.0 which is highest
        """
        self.namespace = namespace
        self.field = "analysis.{}.predictions".format(namespace)
        self.labels = as_collection(labels)
        self.score = [min_score, max_score]

    def for_json(self):
        return {
            "bool": {
                "filter": [
                    {
                        "terms": {
                            self.field + ".label": self.labels
                        }
                    }
                ],
                "must": [
                    {
                        "nested": {
                            "path": self.field,
                            "query": {
                                "function_score": {
                                    "boost_mode": "sum",
                                    "field_value_factor": {
                                        "field": self.field + ".score",
                                        "missing": 0
                                    },
                                    "query": {
                                        "bool": {
                                            "filter": [
                                                {
                                                    "terms": {
                                                        self.field + ".label": self.labels
                                                    }
                                                },
                                                {
                                                    "range": {
                                                        self.field + ".score": {
                                                            "gte": self.score[0],
                                                            "lte": self.score[1]
                                                        }
                                                    }
                                                }
                                            ]
                                        }
                                    }
                                }
                            }
                        }
                    }
                ]
            }
        }


class SingleLabelConfidenceQuery(object):
    """
    A helper class for building a label confidence score query.  This query must point
    at label confidence structure:  For example: analysis.boonai.label-detection.

    References:
        "labels": [
                {"label": "dog", "score": 0.97 },
                {"label": "fox", "score": 0.63 }
        ]
    """

    def __init__(self, namespace, labels, min_score=0.1, max_score=1.0):
        """
        Create a new SingleLabelConfidenceScoreQuery.

        Args:
            namespace (str): The analysis namespace with predictions. (ex: boonai-label-detection)
            labels (list): A list of labels to filter.
            min_score (float): The minimum label score, default to 0.1.
            Note that 0.0 allows everything.
            max_score (float): The maximum score, defaults to 1.0 which is highest
        """
        self.namespace = namespace

        self.field = "analysis.{}".format(namespace)
        self.labels = as_collection(labels)
        self.score = [min_score, max_score]

    def for_json(self):
        return {
            "bool": {
                "filter": [
                    {
                        "terms": {
                            self.field + ".label": self.labels
                        }
                    }
                ],
                "must": [
                    {
                        "function_score": {
                            "query": {
                                "bool": {
                                    "must": [
                                        {
                                            "terms": {
                                                self.field + ".label": self.labels
                                            }
                                        },
                                        {
                                            "range": {
                                                self.field + ".score": {
                                                    "gte": self.score[0],
                                                    "lte": self.score[1]
                                                }
                                            }
                                        }
                                    ]
                                }
                            },
                            "boost": "5",
                            "boost_mode": "sum",
                            "field_value_factor": {
                                "field": self.field + ".score",
                                "missing": 0
                            }
                        }
                    }
                ]
            }
        }


class VideoClipContentMatchQuery:
    """
    Provides a Match query against clip.content and sets clip.score as the query score.
    """
    def __init__(self, query, min_score=0.1, max_score=1.0):
        """
        Create a new VideoClipContentMatchQuery.

        Args
            query (str): The text of the query.
            min_score (float): The minimum label score, default to 0.1.
            max_score (float): The maximum score, defaults to 1.0 which is highest
        """
        self.query = query
        self.score = [min_score, max_score]

    def for_json(self):
        return {
            "bool": {
                "must": [
                    {
                        "function_score": {
                            "boost_mode": "sum",
                            "field_value_factor": {
                                "field": "clip.score",
                                "missing": 0
                            },
                            "query": {
                                "bool": {
                                    "filter": [
                                        {
                                            "match": {
                                                "clip.content": self.query
                                            }
                                        },
                                        {
                                            "range": {
                                                "clip.score": {
                                                    "gte": self.score[0],
                                                    "lte": self.score[1]
                                                }
                                            }
                                        }
                                    ]
                                }
                            }
                        }
                    }
                ]
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
                 field="analysis.boonai-image-similarity.simhash"):
        self.field = field
        self.hashes = []
        self.min_score = min_score
        self.boost = boost

        self.add_hash(hashes)

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
            elif getattr(simhash, 'simhash', None):
                if simhash.simhash:
                    self.hashes.append(simhash.simhash)
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
                    "lang": "boonai-similarity",
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


class VideoClipSimilarityQuery(SimilarityQuery):
    def __init__(self, hashes, min_score=0.75, boost=1.0):
        super(VideoClipSimilarityQuery, self).__init__(
            hashes, min_score, boost, 'clip.simhash')


class FaceSimilarityQuery:
    """
    Performs a face similarity search.
    """
    def __init__(self, faces, min_score=0.90, boost=1.0,
                 field="analysis.boonai-face-detection.predictions.simhash"):
        """
        Create a new FaceSimilarityQuery.

        Args:
            faces (list): A prediction with a 'simhash' property or a simhash itself.
            min_score (float): The minimum score.
            boost (float): A boost value which weights this query higer than others.
            field (str): An optional field to compare make the comparison with. Defaults to ZVI.
        """
        hashes = []
        for face in as_collection(faces):
            if isinstance(face, str):
                hashes.append(face)
            else:
                hashes.append(face['simhash'])

        self.simquery = SimilarityQuery(
            hashes,
            min_score,
            boost,
            field)

    def for_json(self):
        return self.simquery.for_json()
