import os
import tempfile
import unittest
from unittest.mock import patch

from zmlp import ZmlpClient, app_from_env
from zvi.convert_search import ConvertSearchResults


class PandasTests(unittest.TestCase):

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
                            "analysis": {"zvi-image-similarity": {
                                "simhash": "AAAAAAAA"}
                            },
                            "labels": [
                                {
                                    "modelId": "ds-id-12345",
                                    "label": "Glion",
                                }
                            ],
                            "media": {
                                "height": 636,
                                "width": 960
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
                            'analysis': {"zvi-image-similarity": {
                                "simhash": "BBBBBBBB"}
                            },
                            "labels": [
                                {
                                    "modelId": "ds-id-12345",
                                    "label": "Gandalf",
                                }
                            ],
                            "media": {
                                "height": 640,
                                "width": 960
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
                            'analysis': {"zvi-image-similarity": {
                                "simhash": "CCCCCCCC"}
                            },
                            "media": {
                                "height": 640,
                                "width": 960
                            }
                        }
                    }
                ]
            }
        }

    @patch.object(ZmlpClient, 'post')
    def test_search_to_dict(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        attrs = [
            'source.path',
            'analysis.zvi-image-similarity.simhash',
            'labels'
        ]
        rsp = self.app.assets.search(search=search)
        csr = ConvertSearchResults(
            search=rsp,
            attrs=attrs[1:],  # all but source.path
            descriptor='source.path'
        )
        asset_dict = csr.to_dict()

        assert list(asset_dict.keys()) == attrs
        assert asset_dict['source.path'] == [
            'https://i.imgur.com/SSN26nN.jpg',
            'https://i.imgur.com/foo.jpg',
            'https://i.imgur.com/bar.jpg'
        ]
        assert asset_dict['analysis.zvi-image-similarity.simhash'] == [
            'AAAAAAAA',
            'BBBBBBBB',
            'CCCCCCCC'
        ]

    @patch.object(ZmlpClient, 'post')
    def test_search_to_dict_no_search(self, post_patch):
        post_patch.return_value = self.mock_search_result

        csr = ConvertSearchResults(search=None, attrs=None, descriptor='source.path')
        asset_dict = csr.to_dict()

        assert list(asset_dict.keys()) == ['source.path', 'media.height', 'media.width']

    @patch.object(ZmlpClient, 'post')
    def test_search_to_df(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        attrs = [
            'source.path',
            'analysis.zvi-image-similarity.simhash',
            'labels'
        ]
        rsp = self.app.assets.search(search=search)
        csr = ConvertSearchResults(
            search=rsp,
            attrs=attrs[1:],  # all but source.path
            descriptor='source.path'
        )
        df = csr.to_df()

        assert df.shape == (3, 3)
        assert list(df.columns) == attrs
        assert df.iloc[1][attrs[1]] == 'BBBBBBBB'  # simhash
        assert not df.iloc[2][attrs[2]]  # no `labels` for last asset

    @patch.object(ZmlpClient, 'post')
    def test_search_to_df_no_search(self, post_patch):
        post_patch.return_value = self.mock_search_result

        csr = ConvertSearchResults(search=None, attrs=None, descriptor='source.path')
        df = csr.to_df()

        assert df.shape == (3, 3)
        assert list(df.columns) == ['source.path', 'media.height', 'media.width']

    @patch.object(ZmlpClient, 'post')
    def test_search_to_csv(self, post_patch):
        _, output_file = tempfile.mkstemp(".csv")
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        attrs = [
            'source.path',
            'analysis.zvi-image-similarity.simhash',
            'labels'
        ]
        rsp = self.app.assets.search(search=search)
        csr = ConvertSearchResults(
            search=rsp,
            attrs=attrs[1:],  # all but source.path
            descriptor='source.path'
        )
        out = csr.to_csv(output_file=output_file)

        assert os.path.exists(output_file)
        assert out == output_file

    @patch.object(ZmlpClient, 'post')
    def test_search_to_csv_no_search(self, post_patch):
        _, output_file = tempfile.mkstemp(".csv")
        post_patch.return_value = self.mock_search_result

        csr = ConvertSearchResults(search=None, attrs=None, descriptor='source.path')
        out = csr.to_csv(output_file=output_file)

        assert os.path.exists(output_file)
        assert out == output_file
