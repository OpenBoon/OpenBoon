#!/usr/bin/env python3

from boonsdk import app_from_env
from boonsdk.search import PredictionLabelsAggregation, PredictionMetricsAggregation


agg1 = PredictionLabelsAggregation("face_rec", "boonai-face-recognition", max_score=1)
agg2 = PredictionMetricsAggregation("face_metrics", "boonai-face-recognition")

aggs = {}
aggs.update(agg1)
aggs.update(agg2)

q = {
    "size": 0,
    "aggs": aggs
}

app = app_from_env()
search = app.assets.search(q)

for bucket in search.aggregation(agg1)['buckets']:
    print(bucket)
