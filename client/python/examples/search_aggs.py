#!/usr/bin/env python3

from boonsdk import app_from_env

q = {
    "size": 0,
    "aggs": {
        "file_types": {
            "terms": {"field": "source.extension"}
        }
    }
}

app = app_from_env()
search = app.assets.search(q)
print(search.aggregation("file_types"))
