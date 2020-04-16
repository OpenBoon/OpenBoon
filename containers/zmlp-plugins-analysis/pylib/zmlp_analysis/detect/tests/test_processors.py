from unittest.mock import patch

from zmlp import ZmlpClient
from zmlp_analysis.detect.processors import ZmlpObjectDetectionProcessor
from zmlpsdk import Frame
from zmlpsdk.proxy import store_asset_proxy
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_data, TestAsset, \
    get_prediction_labels


class ZmlpObjectDetectionProcessorTests(PluginUnitTestCase):

    @patch.object(ZmlpClient, 'upload_file')
    def test_process_single_detections(self, upload_patch):
        image_path = zorroa_test_data('images/detect/dogbike.jpg')
        frame = Frame(TestAsset(image_path))

        upload_patch.return_value = {
            'id': 'assets/id/proxy/proxy_200x200.jpg',
            'name': 'proxy_200x200.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 576,
                'height': 1024
            }
        }

        # We have to add a proxy to use ML, there is no source
        # fallback currently.
        store_asset_proxy(frame.asset, image_path, (576, 1024))
        processor = self.init_processor(ZmlpObjectDetectionProcessor(), {})
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.zvi-object-detection')
        grouped = get_prediction_labels(analysis)
        assert 'dog' in grouped
        assert 'toilet' in grouped
        assert 'bicycle' in grouped
        assert 'labels' == analysis['type']

    @patch.object(ZmlpClient, 'upload_file')
    def test_process_multi_detections(self, upload_patch):
        image_path = zorroa_test_data('images/detect/cats.jpg')
        frame = Frame(TestAsset(image_path))

        upload_patch.return_value = {
            'id': 'assets/id/proxy/proxy_200x200.jpg',
            'name': 'proxy_200x200.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 576,
                'height': 1024
            }
        }

        # We have to add a proxy to use ML, there is no source
        # fallback currently.
        store_asset_proxy(frame.asset, image_path, (576, 1024))
        processor = self.init_processor(ZmlpObjectDetectionProcessor(), {})
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.zvi-object-detection')
        grouped = get_prediction_labels(analysis)
        assert ["cat", "cat", "cat", "cat"] == grouped
