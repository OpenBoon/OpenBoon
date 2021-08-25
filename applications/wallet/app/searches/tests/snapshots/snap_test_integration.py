# -*- coding: utf-8 -*-
# snapshottest: v1 - https://goo.gl/zC4yUc
from __future__ import unicode_literals

from snapshottest import Snapshot


snapshots = Snapshot()

snapshots['TestQuery.test_get_with_max_assets_limited 1'] = {
    'count': 2,
    'next': None,
    'previous': None,
    'results': [
        {
            'assetStyle': 'image',
            'id': '_V_suiBEd3QEPBWxMq6yW6SII8cCuP1U',
            'metadata': {
                'source': {
                    'checksum': 754419346,
                    'extension': 'tiff',
                    'filename': 'singlepage.tiff',
                    'filesize': 11082,
                    'mimetype': 'image/tiff',
                    'path': 'gs://zorroa-dev-data/image/singlepage.tiff'
                }
            },
            'thumbnailUrl': 'http://testserver/api/v1/projects/6abc33f0-4acf-4196-95ff-4cbb7f640a06/assets/_V_suiBEd3QEPBWxMq6yW6SII8cCuP1U/files/category/web-proxy/name/web-proxy.jpg/',
            'videoLength': None,
            'videoProxyUrl': None
        }
    ]
}
