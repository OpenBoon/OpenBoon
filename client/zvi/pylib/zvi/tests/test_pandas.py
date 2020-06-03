import unittest
from unittest.mock import patch

from zmlp import ZmlpClient, app_from_env
from zvi.pandas import search_to_df

class PandasTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()

        self.mock_import_result = {
            'bulkResponse': {
                'took': 15,
                'errors': False,
                'items': [{
                    'create': {
                        '_index': 'yvqg1901zmu5bw9q',
                        '_type': '_doc',
                        '_id': 'dd0KZtqyec48n1q1fniqVMV5yllhRRGx',
                        '_version': 1,
                        'result': 'created',
                        'forced_refresh': True,
                        '_shards': {
                            'total': 1,
                            'successful': 1,
                            'failed': 0
                        },
                        '_seq_no': 0,
                        '_primary_term': 1,
                        'status': 201
                    }
                }]
            },
            'failed': [],
            'created': ['dd0KZtqyec48n1q1fniqVMV5yllhRRGx'],
            'jobId': 'ba310246-1f87-1ece-b67c-be3f79a80d11'
        }

        # A mock search result used for asset search tests
        self.mock_search_result = {
            'took': 4,
            'timed_out': False,
            'hits': {
                'total': {'value': 2},
                'max_score': 0.2876821,
                'hits': [
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                        '_score': 0.2876821,
                        '_source': {
                            'source': {
                                'path': 'https://i.imgur.com/SSN26nN.jpg'
                            }
                        }
                    },
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'aabbccddec48n1q1fginVMV5yllhRRGx2WKyKLjDphg',
                        '_score': 0.2876821,
                        '_source': {
                            'source': {
                                'path': 'https://i.imgur.com/foo.jpg'
                            },
                            'analysis': {
                                'simhash': 'ABCDEFG'
                            }
                        }
                    }
                ]
            }
        }

    @patch.object(ZmlpClient, 'post')
    def test_search_to_df(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        rsp = self.app.assets.search(search=search)
        df = search_to_df(
            search=rsp,
            attrs=['analysis.simhash'],
            descriptor='source.path'
        )

        assert df.shape == (2, 2)
        assert list(df.columns) == ['source.path', 'analysis.simhash']
        assert df.iloc[1]['analysis.simhash'] == 'ABCDEFG'
