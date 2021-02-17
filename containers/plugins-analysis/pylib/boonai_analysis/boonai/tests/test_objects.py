from unittest.mock import patch

from boonai_analysis.zvi.objects import ZviObjectDetectionProcessor
from boonflow import Frame
from boonflow.testing import PluginUnitTestCase, test_path, TestAsset, \
    get_prediction_labels


class BoonSdkObjectDetectionProcessorTests(PluginUnitTestCase):

    @patch('boonai_analysis.zvi.objects.get_proxy_level_path')
    def test_process_single_detections(self, proxy_patch):
        image_path = test_path('images/detect/dogbike.jpg')
        proxy_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))

        processor = self.init_processor(ZviObjectDetectionProcessor(), {})
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.boonai-object-detection')
        grouped = get_prediction_labels(analysis)
        assert 'dog' in grouped
        assert 'toilet' in grouped
        assert 'bicycle' in grouped
        assert 'labels' == analysis['type']

    @patch('boonai_analysis.zvi.objects.get_proxy_level_path')
    def test_process_multi_detections(self, proxy_patch):
        image_path = test_path('images/detect/cats.jpg')
        proxy_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))

        processor = self.init_processor(ZviObjectDetectionProcessor(), {})
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.boonai-object-detection')
        grouped = get_prediction_labels(analysis)
        assert ["cat", "cat", "cat", "cat"] == grouped
