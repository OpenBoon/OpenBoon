import os

from unittest.mock import patch

from zmlp_analysis.google.cloud_vision import *
from zmlpsdk import Frame
from zmlpsdk.proxy import store_asset_proxy
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset

from zmlp import ZmlpClient

class CloudVisionProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = os.path.dirname(__file__) + '/gcp-creds.json'

    def tearDown(self):
        del os.environ['GOOGLE_APPLICATION_CREDENTIALS']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_image_text_processor(self, proxy_patch):
        path = zorroa_test_path('images/set01/visa.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionDetectImageText())
        processor.process(frame)
        assert ('Giants Franchise History' in
                frame.asset.get_attr('analysis.google.imageTextDetection.content'))

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_document_text_processor(self, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionDetectDocumentText())
        processor.process(frame)
        print(frame.asset.get_attr('analysis.google'))
        assert 'HEY GIRL' in frame.asset.get_attr('analysis.google.documentTextDetection.content')

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_landmark_detection(self, proxy_patch):
        path = zorroa_test_path('images/set08/eiffel_tower.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionDetectLandmarks())
        processor.process(frame)
        assert 'Eiffel Tower' in frame.asset.get_attr('analysis.google.landmarkDetection.keywords')

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_explicit_detection(self, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionDetectExplicit())
        processor.process(frame)
        assert frame.asset.get_attr('analysis.google.explicit.spoof') == 1.0

    @patch.object(ZmlpClient, 'upload_file')
    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_face_detection(self, proxy_patch, upload_patch):
        upload_patch.return_value = {
            "name": "googleFaceDetection_200x200.jpg",
            "category": "proxy",
            "mimetype": "image/jpeg",
            "attrs": {
                "width": 512,
                "height": 339
            }
        }
        path = zorroa_test_path('images/set08/meme.jpg')
        proxy_patch.return_value = path
        asset = TestAsset(path)
        frame = Frame(asset)
        store_asset_proxy(frame.asset, path, (512, 339))
        processor = self.init_processor(CloudVisionDetectFaces())
        processor.process(frame)
        assert 1 == asset.get_attr("analysis.google.faceDetection.faceCount")
        assert 1 == len(asset.get_attr("elements"))

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_label_detection(self, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionDetectLabels())
        processor.process(frame)
        assert 'Hair' in frame.asset.get_attr('analysis.google.labelDetection.keywords')

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_object_detection(self, proxy_patch):
        path = zorroa_test_path('images/detect/dogbike.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionDetectObjects())
        processor.process(frame)
        assert 'Dog' in frame.asset.get_attr('analysis.google.objectDetection.keywords')
