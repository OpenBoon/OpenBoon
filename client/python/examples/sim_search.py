#!/usr/bin/env python3

from zmlp import app_from_env

app = app_from_env()

q = {
    "query": {
        "bool": {
            "filter": [
                app.assets.get_sim_query(
                    "../../../test-data/images/set01/toucan.jpg"
                )
            ]
        }
    }
}


search = app.assets.search(q)
for a in search:
    print(a.uri)
