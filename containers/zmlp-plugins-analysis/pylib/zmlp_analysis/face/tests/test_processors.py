#!/usr/bin/env python
from unittest.mock import patch

from zmlp import ZmlpClient
from zmlp.analysis.testing import TestAsset, PluginUnitTestCase, zorroa_test_data
from zmlp.analysis import Frame
from zmlp.analysis.proxy import store_asset_proxy
from zmlp_analysis.face.processors import FaceRecognitionProcessor


class FaceUnitTestCase(PluginUnitTestCase):

    @patch.object(ZmlpClient, 'upload_file')
    def test_FaceRecognition(self, upload_patch):
        upload_patch.return_value = {
            "name": "proxy_200x200.jpg",
            "category": "proxy",
            "mimetype": "image/jpeg",
            "attrs": {
                "width": 1023,
                "height": 1024
            }
        }

        test_faces_path = zorroa_test_data("images/set01/faces.jpg")
        frame = Frame(TestAsset(test_faces_path))

        store_asset_proxy(frame.asset, test_faces_path, (1024, 1024))

        processor = self.init_processor(FaceRecognitionProcessor(), {})
        processor.process(frame)

        self.assertEquals(128, len(frame.asset.get_attr("analysis.faceRecognition.shash")))
        self.assertEquals(2, frame.asset.get_attr("analysis.faceRecognition.number"))
