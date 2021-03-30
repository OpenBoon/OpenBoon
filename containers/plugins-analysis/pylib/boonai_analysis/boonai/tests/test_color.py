from unittest.mock import patch

from boonai_analysis.boonai.color import RegionalColorSimilarity
from boonflow import Frame
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path


class ColorProcessorTests(PluginUnitTestCase):

    @patch('boonai_analysis.boonai.color.get_proxy_level_path')
    def test_process(self, proxy_patch):
        image_path = test_path('images/set01/toucan.jpg')
        proxy_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))
        processor = self.init_processor(RegionalColorSimilarity(), {})
        processor.process(frame)
