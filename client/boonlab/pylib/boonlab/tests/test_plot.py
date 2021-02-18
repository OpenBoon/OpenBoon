import unittest
from unittest.mock import patch

from boonsdk import BoonClient, app_from_env
from zvi.plot import plot_tsne


class PlotTests(unittest.TestCase):

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
                        '_source': {

                            "analysis": {"zvi-image-similarity": {
                                "simhash": "EESEAAAAA"}
                            }
                        }
                    },
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'ad0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                        '_source': {

                            "analysis": {"zvi-image-similarity": {
                                "simhash": "FFFASAAAA"}
                            }
                        }
                    },
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'bd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                        '_source': {

                            "analysis": {"zvi-image-similarity": {
                                "simhash": "DDDSAAAAA"}
                            }
                        }
                    },
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'cd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                        '_source': {

                            "analysis": {"zvi-image-similarity": {
                                "simhash": "CCCSAAAAA"}
                            }
                        }
                    },
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'ed0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                        '_source': {

                            "analysis": {"zvi-image-similarity": {
                                "simhash": "BBBSAAAAA"}
                            }
                        }
                    },
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'fd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                        '_source': {

                            "analysis": {"zvi-image-similarity": {
                                "simhash": "ACCCSAAHA"}
                            }
                        }
                    },
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'gd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                        '_source': {

                            "analysis": {"zvi-image-similarity": {
                                "simhash": "AAACCSAAA"}
                            }
                        }
                    },
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'hd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                        '_source': {

                            "analysis": {"zvi-image-similarity": {
                                "simhash": "ACGAEASAA"}
                            }
                        }
                    },
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'id0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                        '_source': {

                            "analysis": {"zvi-image-similarity": {
                                "simhash": "AAAAFGAAA"}
                            }
                        }
                    },
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'jd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                        '_source': {

                            "analysis": {"zvi-image-similarity": {
                                "simhash": "ADFAAAAAA"}
                            }
                        }
                    },

                ]
            }
        }

    @patch.object(BoonClient, 'post')
    def test_plot_tsne(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        rsp = self.app.assets.search(search=search)
        assets, x, clusters = plot_tsne(search=rsp, verbose=False)

        assert x[0][0] == 69.0
        assert x[1][0] == 70.0
