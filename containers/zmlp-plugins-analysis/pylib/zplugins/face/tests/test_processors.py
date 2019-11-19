#!/usr/bin/env python

import zorroa.zsdk
from zplugins.face.processors import FaceRecognitionProcessor
from zorroa.zsdk import Asset
from zorroa.zsdk.testing import PluginUnitTestCase, zorroa_test_data


class FaceUnitTestCase(PluginUnitTestCase):
    def test_FaceRecognition(self):
        test_faces_path = zorroa_test_data("images/set01/faces.jpg")
        frame = zorroa.zsdk.Frame(Asset(test_faces_path))
        ofile = self.ofs.prepare("asset", frame.asset, "proxy.jpg")
        ofile.store(open(test_faces_path))

        frame.asset.set_attr("proxies.proxies", [{"id": ofile.id}, {"id": ofile.id},
                                                 {"id": ofile.id}])

        processor = self.init_processor(FaceRecognitionProcessor(), {})
        processor.process(frame)

        self.assertEquals(128, len(frame.asset.get_attr("analysis.faceRecognition.shash")))
        self.assertEquals(2, frame.asset.get_attr("analysis.faceRecognition.number"))
