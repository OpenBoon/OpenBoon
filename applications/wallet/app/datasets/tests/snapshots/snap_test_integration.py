# -*- coding: utf-8 -*-
# snapshottest: v1 - https://goo.gl/zC4yUc
from __future__ import unicode_literals

from snapshottest import Snapshot


snapshots = Snapshot()

snapshots['TestLabelingEndpoints.test_add_labels_by_search 1'] = {
    'label': {
        'datasetId': '287baa12-8f80-1a31-9273-76fd36c58a09',
        'label': 'Cool Label',
        'scope': 'TRAIN'
    },
    'maxAssets': 5,
    'search': {
        'query': {
            'bool': {
                'filter': [
                    {
                        'range': {
                            'media.size': {
                                'gte': 1,
                                'lte': 20000
                            }
                        }
                    }
                ]
            }
        }
    }
}
