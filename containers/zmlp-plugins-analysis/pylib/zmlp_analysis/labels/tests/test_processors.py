from unittest.mock import patch

from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_data, \
    TestAsset, get_mock_stored_file, get_prediction_labels
from zmlpsdk import Frame
from zmlpsdk.proxy import store_asset_proxy
from zmlp import ZmlpClient
from zmlp_analysis.labels import ZviLabelDetectionProcessor


class ZviLabelDetectionProcessorTests(PluginUnitTestCase):

    @patch.object(ZmlpClient, 'upload_file')
    def test_process(self, upload_patch):
        toucan_path = zorroa_test_data('images/set01/toucan.jpg')
        frame = Frame(TestAsset(toucan_path))
        upload_patch.return_value = get_mock_stored_file()._data
        store_asset_proxy(frame.asset, toucan_path, (512, 512))

        processor = self.init_processor(ZviLabelDetectionProcessor())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.zvi-label-detection')
        assert 'toucan' in get_prediction_labels(analysis)
        assert 1 == analysis['count']
        assert 'labels' == analysis['type']
