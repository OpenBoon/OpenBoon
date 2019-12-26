from unittest.mock import patch

from zmlp import ZmlpClient
from zmlp.analysis import Frame
from zmlp.analysis.proxy import store_asset_proxy
from zmlp.analysis.testing import PluginUnitTestCase, zorroa_test_data, TestAsset

from zmlp_analysis.detect.processors import PixelMLObjectDetectionProcessor


class PixelMLObjectDetectionProcessorTests(PluginUnitTestCase):

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
                "width": 2322,
                "height": 4128
            }
        }

        # We have to add a proxy to use ML, there is no source
        # fallback currently.
        store_asset_proxy(self.frame.asset, self.image_path, (2322, 4128))
        processor = self.init_processor(PixelMLObjectDetectionProcessor(), {})
        processor.process(self.frame)

        elements = self.frame.asset.document["elements"]
        grouped = dict([(e["labels"][0], e) for e in elements])
        print(elements)
        assert grouped["dog"]["regions"] == ["SW", "SE"]
        assert grouped["toilet"]["regions"] == ["SW"]
        assert grouped["bicycle"]["regions"] == ["NW", "NE", "SW", "SE", "CENTER"]
