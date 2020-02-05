import os

from unittest.mock import patch

from zmlp_analysis.google.cloud_vision import CloudVisionProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset

class CloudVisionProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = 'gcp-creds.json'

    def tearDown(self):
        del os.environ['GOOGLE_APPLICATION_CREDENTIALS']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_image_text_processor(self, proxy_patch):
        path = zorroa_test_path('images/set01/visa.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionProcessor(), {'detect_image_text': True})
        processor.process(frame)
        assert ('Giants Franchise History' in
                frame.asset.get_attr('analysis.google.imageTextDetection.content'))

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_document_text_processor(self, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionProcessor(), {'detect_document_text': True})
        processor.process(frame)
        print(frame.asset.get_attr('analysis.google'))
        assert 'HEY GIRL' in frame.asset.get_attr('analysis.google.documentTextDetection.content')

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_landmark_detection(self, proxy_patch):
        path = zorroa_test_path('images/set08/eiffel_tower.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionProcessor(), {'detect_landmarks': True})
        processor.process(frame)
        assert 'Eiffel Tower' in frame.asset.get_attr('analysis.google.landmarkDetection.keywords')

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_explicit_detection(self, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionProcessor(), {'detect_explicit': True})
        processor.process(frame)
        assert frame.asset.get_attr('analysis.google.explicit.spoof') == 1.0

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_face_detection(self, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionProcessor(), {'detect_faces': True})
        processor.process(frame)
        assert 'joy' in frame.asset.get_attr('analysis.google.faceDetection.keywords')

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_label_detection(self, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionProcessor(), {'detect_labels': True})
        processor.process(frame)
        assert 'Hair' in frame.asset.get_attr('analysis.google.labelDetection.keywords')

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_object_detection(self, proxy_patch):
        path = zorroa_test_path('images/detect/dogbike.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionProcessor(), {'detect_objects': True})
        processor.process(frame)
        assert 'Dog' in frame.asset.get_attr('analysis.google.labelDetection.keywords')

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    def test_object_detection(self, proxy_patch):
        path = zorroa_test_path('images/detect/dogbike.jpg')
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionProcessor(), {'detect_objects': True})
        processor.process(frame)
        assert 'Dog' in frame.asset.get_attr('analysis.google.objectDetection.keywords')
