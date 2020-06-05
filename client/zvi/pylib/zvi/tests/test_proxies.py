import unittest
from unittest.mock import patch
import numpy as np

from zmlp import Asset
from zmlp.app import AssetApp
from zvi.proxies import download_proxy
from zmlpsdk.testing import zorroa_test_path


class ProxiesTests(unittest.TestCase):

    def setUp(self):
        self.test_files = [{
            "id": "assets/123/proxy/proxy_200x200.jpg",
            "category": "proxy",
            "name": "proxy_200x200.jpg",
            "mimetype": "image/jpeg",
            "attrs": {
                "width": 200,
                "height": 200
            }
        },
            {
                "id": "assets/123/proxy/proxy_400x400.jpg",
                "category": "proxy",
                "name": "proxy_400x400.jpg",
                "mimetype": "image/jpeg",
                "attrs": {
                    "width": 400,
                    "height": 400
                }
            }]

    @patch.object(AssetApp, 'download_file')
    def test_download_proxy(self, dl_patch):
        dl_patch.return_value = zorroa_test_path("training/test_dsy.jpg")

        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        for level in [0, 1]:  # using 200x200 and 400x400
            img = download_proxy(asset=asset, level=level)
            assert type(img) == np.ndarray
