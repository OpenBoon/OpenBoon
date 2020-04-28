from unittest.mock import patch

from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset, \
    get_prediction_labels
from zmlp_analysis.zvi.faces import ZviFaceDetectionProcessor


class ZviFaceDetectionProcessorTests(PluginUnitTestCase):

    @patch('zmlp_analysis.zvi.faces.get_proxy_level_path')
    def test_process_detection(self, proxy_patch):
        image_path = zorroa_test_path('images/face-recognition/face1.jpg')
        proxy_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))

        processor = self.init_processor(ZviFaceDetectionProcessor(), {})
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.zvi-face-detection')
        grouped = get_prediction_labels(analysis)
        assert 'face0' in grouped
        assert 'labels' == analysis['type']