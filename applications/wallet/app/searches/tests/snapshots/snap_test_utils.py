# -*- coding: utf-8 -*-
# snapshottest: v1 - https://goo.gl/zC4yUc
from __future__ import unicode_literals

from snapshottest import Snapshot


snapshots = Snapshot()

snapshots['TestFilterBoy.test_finalize_query_normal_filters 1'] = {
    'query': {
        'bool': {
            'filter': [
                {
                    'range': {
                        'source.filesize': {
                            'gte': 1,
                            'lte': 100
                        }
                    }
                },
                {
                    'terms': {
                        'source.extension': [
                            'jpeg',
                            'tiff'
                        ]
                    }
                }
            ]
        }
    }
}

snapshots['TestFilterBoy.test_finalize_query_only_limit_filter 1'] = {
    'sort': {
        'system.timeCreated': {
            'order': 'desc'
        }
    }
}
