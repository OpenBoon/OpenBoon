from unittest.mock import patch

from zmlp_analysis.zvi.objects import ZviObjectDetectionProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset, \
    get_prediction_labels


class ZmlpObjectDetectionProcessorTests(PluginUnitTestCase):

    @patch('zmlp_analysis.zvi.objects.get_proxy_level_path')
    def test_process_single_detections(self, proxy_patch):
        image_path = zorroa_test_path('images/detect/dogbike.jpg')
        proxy_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))

        processor = self.init_processor(ZviObjectDetectionProcessor(), {})
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = frame.asset.get_attr('analysis.zvi-object-detection')
        grouped = get_prediction_labels(analysis)
        assert 'dog' in grouped
        assert 'toilet' in grouped
        assert 'bicycle' in grouped
        assert 'labels' == analysis['type']

    @patch('zmlp_analysis.zvi.objects.get_proxy_level_path')
    def test_process_multi_detections(self, proxy_patch):
        image_path = zorroa_test_path('images/detect/cats.jpg')
        proxy_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))

        processor = self.init_processor(ZviObjectDetectionProcessor(), {})
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = frame.asset.get_attr('analysis.zvi-object-detection')
        grouped = get_prediction_labels(analysis)
        assert ["cat", "cat", "cat", "cat"] == grouped
