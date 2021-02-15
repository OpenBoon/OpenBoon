#!/usr/bin/env python3
import sys

from boonsdk import app_from_env

app = app_from_env()

q = {
    "query": app.assets.get_sim_query(sys.argv[1], min_score=0.75)
}

search = app.assets.search(q)
for a in search:
    print("{} {}".format(a.uri, a.score))
