import unittest
from unittest.mock import patch

from boonsdk import BoonClient, app_from_env
from boonlab.hashes import read_hash_as_vectors


class HashesTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()

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
                            },
                            "analysis": {"boonai-image-similarity": {
                                "simhash": "AAAAAAAA"}
                            },
                            "labels": [
                                {
                                    "modelId": "ds-id-12345",
                                    "label": "Glion",
                                }
                            ],
                            'system': {
                                'state': 'Analyzed'
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
                            'analysis': {"boonai-image-similarity": {
                                "simhash": "BBBBBBBB"}
                            },
                            "labels": [
                                {
                                    "modelId": "ds-id-12345",
                                    "label": "Gandalf",
                                }
                            ],
                            'system': {
                                'state': 'Analyzed'
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
                                'path': 'https://i.imgur.com/bar.jpg'
                            },
                            'analysis': {"boonai-image-similarity2": {
                                "simhash": "CCCCCCCC"}
                            },
                            "labels": [
                                {
                                    "modelId": "ds-id-12345",
                                    "label": "Gandalf",
                                }
                            ],
                            'system': {
                                'state': 'Analyzed'
                            }
                        }
                    }
                ]
            }
        }

    @patch.object(BoonClient, 'post')
    def test_read_hash_as_vectors(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        rsp = self.app.assets.search(search=search)
        x, assets = read_hash_as_vectors(search=rsp)

        assert x[0][0] == 65
        assert x[1][0] == 66
