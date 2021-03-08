#!/usr/bin/env python3

from boonsdk import app_from_env
from boonsdk.search import LabelConfidenceTermsAggregation

agg = LabelConfidenceTermsAggregation("face_rec", "boonai-face-recognition", max_score=1)

aggs = {}
aggs.update(agg)

q = {
    "size": 0,
    "aggs": aggs
}

app = app_from_env()
search = app.assets.search(q)
for bucket in search.aggregation(agg)['buckets']:
    print(bucket)

