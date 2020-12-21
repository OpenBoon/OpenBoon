from unittest.mock import patch

from zmlp_analysis.zvi.labels import ZviLabelDetectionProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, \
    TestAsset, get_prediction_labels


class ZviLabelDetectionProcessorTests(PluginUnitTestCase):

    @patch.object(ZviLabelDetectionProcessor, '_record_analysis_metric')
    @patch('zmlp_analysis.zvi.labels.get_proxy_level_path')
    def test_process(self, proxy_patch, _):
        toucan_path = zorroa_test_path('images/set01/toucan.jpg')
        proxy_patch.return_value = toucan_path
        frame = Frame(TestAsset(toucan_path))

        processor = self.init_processor(ZviLabelDetectionProcessor())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.zvi-label-detection')
        assert 'toucan' in get_prediction_labels(analysis)
        assert 5 == analysis['count']
        assert 'labels' == analysis['type']
