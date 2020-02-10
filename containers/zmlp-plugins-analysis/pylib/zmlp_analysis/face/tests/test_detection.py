#!/usr/bin/env python
import logging
from unittest.mock import patch

from zmlp import ZmlpClient
from zmlpsdk import Frame
from zmlpsdk.proxy import store_asset_proxy
from zmlpsdk.testing import TestAsset, PluginUnitTestCase, zorroa_test_data
from zmlp_analysis.face.detection import ZmlpFaceDetectionProcessor

logging.basicConfig(level=logging.DEBUG)


class ZmlpFaceDetectionProcessorTests(PluginUnitTestCase):

    @patch.object(ZmlpClient, 'upload_file')
    def test_process(self, upload_patch):
        upload_patch.return_value = {
            "name": "zmlpFaceDetection_200x200.jpg",
            "category": "proxy",
            "mimetype": "image/jpeg",
            "attrs": {
                "width": 512,
                "height": 339
            }
        }

        test_faces_path = zorroa_test_data("images/face-recognition/face2.jpg")
        frame = Frame(TestAsset(test_faces_path))
        store_asset_proxy(frame.asset, test_faces_path, (512, 339))
        processor = self.init_processor(ZmlpFaceDetectionProcessor(), {})
        processor.process(frame)

        asset = frame.asset
        element = asset.get_attr('elements')[0]
        assert 'face' == element['type']
        assert None is element.get('labels')
        assert [559, 464, 401, 259] == element['rect']
        assert 'proxy/zmlpFaceDetection_200x200.jpg' == element['proxy']
        assert 'zmlp.faceDetection' == element['analysis']
        assert None is element.get('vector')
