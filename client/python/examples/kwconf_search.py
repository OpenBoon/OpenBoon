#!/usr/bin/env python3

import zmlp
from zmlp import app_from_env

q = {
    "query": zmlp.search.LabelConfidenceQuery("analysis.zvi.label-detection", "toucan", 0.5)
}


app = app_from_env()
search = app.assets.search(q)
for a in search:
    print(a.document)
