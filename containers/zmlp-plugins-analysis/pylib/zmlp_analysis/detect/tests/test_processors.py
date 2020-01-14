from unittest.mock import patch

from zmlp import ZmlpClient
from zmlpsdk import Frame
from zmlpsdk.proxy import store_asset_proxy
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_data, TestAsset

from zmlp_analysis.detect.processors import ZmlpObjectDetectionProcessor


class ZmlpObjectDetectionProcessorTests(PluginUnitTestCase):

    def setUp(self):
        self.image_path = zorroa_test_data('images/detect/dogbike.jpg')
        self.frame = Frame(TestAsset(self.image_path))

    @patch.object(ZmlpClient, 'upload_file')
    def test_process(self, upload_patch):
        upload_patch.return_value = {
            "name": "proxy_200x200.jpg",
            "category": "proxy",
            "mimetype": "image/jpeg",
            "attrs": {
                "width": 576,
                "height": 1024
            }
        }

        # We have to add a proxy to use ML, there is no source
        # fallback currently.
        store_asset_proxy(self.frame.asset, self.image_path, (576, 1024))
        processor = self.init_processor(ZmlpObjectDetectionProcessor(), {})
        processor.process(self.frame)

        elements = self.frame.asset.document["elements"]
        grouped = dict([(e["labels"][0], e) for e in elements])
        assert grouped["dog"]["regions"] == ["SW", "SE"]
        assert grouped["toilet"]["regions"] == ["SW"]
        assert grouped["bicycle"]["regions"] == ["NW", "NE", "SW", "SE", "CENTER"]
