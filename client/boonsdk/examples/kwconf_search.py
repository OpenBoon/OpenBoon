#!/usr/bin/env python3

import boonsdk
from boonsdk import app_from_env

q = {
    "query": boonsdk.search.LabelConfidenceQuery("boonai-label-detection", "lakeside", 0.5)
}


app = app_from_env()
search = app.assets.search(q)
for a in search:
    print(a.document)
