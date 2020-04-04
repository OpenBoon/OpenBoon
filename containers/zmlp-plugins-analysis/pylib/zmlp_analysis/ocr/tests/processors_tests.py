from unittest.mock import patch

from zmlp import ZmlpClient
from zmlp_analysis.ocr.processors import OcrProcessor
from zmlpsdk import Frame
from zmlpsdk.proxy import store_asset_proxy
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_data, get_mock_stored_file


class OcrProcessorTests(PluginUnitTestCase):

    @patch.object(ZmlpClient, 'upload_file')
    def test_process(self, upload_patch):
        upload_patch.return_value = get_mock_stored_file()._data

        image_path = zorroa_test_data('images/set09/nvidia_manual_page.jpg', uri=False)
        frame = Frame(TestAsset(image_path))
        store_asset_proxy(frame.asset, image_path, (512, 512))
        processor = self.init_processor(OcrProcessor(), {})
        processor.process(frame)

        assert 'NVIDIA' in frame.asset.get_attr('analysis.zvi-text-detection.content')
