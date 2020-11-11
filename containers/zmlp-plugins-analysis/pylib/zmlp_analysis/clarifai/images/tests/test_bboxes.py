import os
from unittest.mock import patch

from zmlp_analysis.clarifai.images import bboxes
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset, get_prediction_labels

client_patch = 'zmlp_analysis.clarifai.util.ClarifaiApp'


class MockClarifaiApp:
    """
    Class to handle clarifai responses.
    """

    def __init__(self, api_key=None):
        self.public_models = PublicModels()


class ClarifaiPublicModelsProcessorTests(PluginUnitTestCase):

    def setUp(self):
        self.image_path = zorroa_test_path('images/detect/dogbike.jpg')
        self.frame = Frame(TestAsset(self.image_path))

    @patch('zmlp_analysis.clarifai.images.bboxes.get_proxy_level_path')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_face_detection_process(self, _, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(bboxes.ClarifaiFaceDetectionProcessor())
        processor.process(self.frame)

        print(self.frame.asset.get_attr('analysis'))
        analysis = self.frame.asset.get_analysis('clarifai-face-detection')
        assert 'face' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 1 == analysis['count']

    @patch('zmlp_analysis.clarifai.images.bboxes.get_proxy_level_path')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_logo_process(self, _, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(bboxes.ClarifaiLogoDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-logo-detection')
        assert 'Shell' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 6 == analysis['count']


class PublicModels:
    def __init__(self):
        self.face_detection_model = FaceDetectionModel()
        self.logo_model = LogoModel()


class FaceDetectionModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(os.path.dirname(__file__), '', 'mock_data/clarifai_faces.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class LogoModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(os.path.dirname(__file__), '', 'mock_data/clarifai_logo.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())
