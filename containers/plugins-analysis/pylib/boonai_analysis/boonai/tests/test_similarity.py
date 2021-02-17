import os
from unittest.mock import patch

from boonflow import Frame
from boonflow.testing import PluginUnitTestCase, test_path, TestAsset
from boonai_analysis.zvi.similarity import ZviSimilarityProcessor


class MxUnitTests(PluginUnitTestCase):
    @classmethod
    def setUpClass(cls):
        super(MxUnitTests, cls).setUpClass()
        cls.toucan_path = test_path('images/set01/toucan.jpg')

    def setUp(self):
        self.frame = Frame(TestAsset(self.toucan_path))
        if not os.path.exists("/models"):
            ZviSimilarityProcessor.model_path = test_path("models/resnet-152")

    @patch('boonai_analysis.zvi.similarity.get_proxy_level_path')
    def test_ResNetSimilarity_defaults(self, proxy_patch):
        proxy_patch.return_value = self.toucan_path
        processor = ZviSimilarityProcessor()
        processor = self.init_processor(processor, {'debug': True})
        processor.process(self.frame)

        self.assertEquals(2048, len(self.frame.asset['analysis.boonai-image-similarity.simhash']))
