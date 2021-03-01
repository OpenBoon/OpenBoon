from unittest.mock import patch

from boonai_analysis.boonai.ocr import ZviOcrProcessor
from boonflow import Frame
from boonflow.testing import PluginUnitTestCase, TestAsset, test_data


class OcrProcessorTests(PluginUnitTestCase):

    @patch('boonai_analysis.boonai.ocr.get_proxy_level_path')
    def test_process(self, proxy_patch):
        image_path = test_data('images/set09/nvidia_manual_page.jpg', uri=False)
        proxy_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))
        processor = self.init_processor(ZviOcrProcessor(), {})
        processor.process(frame)
        assert 'NVIDIA' in frame.asset.get_attr('analysis.boonai-text-detection.content')

    @patch.object(TestAsset, 'get_files')
    @patch('boonai_analysis.boonai.ocr.file_storage.localize_file')
    def test_process_ocr_proxy(self, storage_patch, get_files_patch):
        image_path = test_data('images/set09/nvidia_manual_page.jpg', uri=False)
        storage_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))
        frame.asset.set_attr('files', [
            {
                'category': 'ocr-proxy',
                'name': 'ocr-proxy.jpg',
                'mimetype': 'image/jpeg'
            }
        ])
        processor = self.init_processor(ZviOcrProcessor(), {})
        processor.process(frame)
        assert 'NVIDIA' in frame.asset.get_attr('analysis.boonai-text-detection.content')

        get_files_patch.assert_called_with(category='ocr-proxy')
