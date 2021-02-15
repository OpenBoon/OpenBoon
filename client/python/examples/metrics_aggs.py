#!/usr/bin/env python3
from boonsdk import app_from_env

query = {
    "size": 0,
    "query": {
        "match_all": {},
    },
    "aggs": {
        "metrics": {
            "nested": {
                "path": "metrics.pipeline"
            },
            "aggs": {
                "names": {
                    "terms": {
                        "field": "metrics.pipeline.processor"
                    },
                    "aggs": {
                        "stats": {
                            "extended_stats": {
                                "field": "metrics.pipeline.executionTime"
                            }
                        }
                    }
                }
            }
        }
    }
}

app = app_from_env()
search = app.assets.search(query)

for bucket in search.aggregation("metrics")["sterms#names"]["buckets"]:
    print("processor: {} {}".format(bucket["key"], bucket["extended_stats#stats"]["avg"]))
    print("---")
