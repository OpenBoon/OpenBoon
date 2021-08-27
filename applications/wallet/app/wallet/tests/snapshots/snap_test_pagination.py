# -*- coding: utf-8 -*-
# snapshottest: v1 - https://goo.gl/zC4yUc
from __future__ import unicode_literals

from snapshottest import Snapshot


snapshots = Snapshot()

snapshots['TestPaginationLimiting.test_deep_empty_page_correctly_sets_previous_link 1'] = {
    'count': 6,
    'next': None,
    'previous': 'http://testserver/apiv1/projects/123/jobs/?from=5&size=5',
    'results': [
    ]
}

snapshots['TestPaginationLimiting.test_limit_under_max_assets 1'] = {
    'count': 6,
    'next': 'http://testserver/apiv1/projects/123/jobs/?from=5&size=5',
    'previous': None,
    'results': [
        {
            'id': '1'
        },
        {
            'id': '2'
        },
        {
            'id': '3'
        },
        {
            'id': '4'
        },
        {
            'id': '5'
        }
    ]
}

snapshots['TestPaginationLimiting.test_max_assets_equals_limit 1'] = {
    'count': 5,
    'next': None,
    'previous': None,
    'results': [
        {
            'id': '1'
        },
        {
            'id': '2'
        },
        {
            'id': '3'
        },
        {
            'id': '4'
        },
        {
            'id': '5'
        }
    ]
}

snapshots['TestPaginationLimiting.test_second_page_max_asset_limited 1'] = {
    'count': 6,
    'next': None,
    'previous': 'http://testserver/apiv1/projects/123/jobs/?size=5',
    'results': [
        {
            'id': '1'
        }
    ]
}
