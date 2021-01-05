import os
from unittest.mock import patch

from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset
from zmlp_analysis.zvi.similarity import ZviSimilarityProcessor


class MxUnitTests(PluginUnitTestCase):
    @classmethod
    def setUpClass(cls):
        super(MxUnitTests, cls).setUpClass()
        cls.toucan_path = zorroa_test_path('images/set01/toucan.jpg')

    def setUp(self):
        super(MxUnitTests, self).setUp()
        self.frame = Frame(TestAsset(self.toucan_path))
        if not os.path.exists("/models"):
            ZviSimilarityProcessor.model_path = zorroa_test_path("models/resnet-152")

    @patch('zmlp_analysis.zvi.similarity.get_proxy_level_path')
    def test_ResNetSimilarity_defaults(self, proxy_patch):
        proxy_patch.return_value = self.toucan_path
        processor = ZviSimilarityProcessor()
        processor = self.init_processor(processor, {'debug': True})
        processor.process(self.frame)
        self.mock_record_analysis_metric.assert_called_once()

        self.assertEquals(2048, len(self.frame.asset['analysis.zvi-image-similarity.simhash']))
