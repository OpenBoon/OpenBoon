# flake8: noqa
import os
from unittest.mock import patch

from zmlp_analysis.clarifai.video.bboxes import *
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, \
    TestAsset, get_prediction_labels

client_patch = 'zmlp_analysis.clarifai.util.ClarifaiApp'


class MockClarifaiApp:
    """
    Class to handle clarifai responses.
    """

    def __init__(self, api_key=None):
        self.public_models = PublicModels()


class ClarifaiPublicModelsProcessorTests(PluginUnitTestCase):

    def setUp(self):
        self.video_path = zorroa_test_path('video/julia_roberts.mp4')
        asset = TestAsset(self.video_path)
        asset.set_attr('media.length', 15.0)
        self.frame = Frame(asset)

    @patch("zmlp_analysis.clarifai.video.bboxes.video.save_timeline", return_value={})
    @patch('zmlp_analysis.clarifai.video.bboxes.proxy.get_video_proxy')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_face_detection_process(self, _, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path

        processor = self.init_processor(ClarifaiVideoFaceDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-video-face-detection-model')
        assert 'face' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 1 == analysis['count']

    @patch("zmlp_analysis.clarifai.video.bboxes.video.save_timeline", return_value={})
    @patch('zmlp_analysis.clarifai.video.bboxes.proxy.get_video_proxy')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_logo_process(self, _, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path

        processor = self.init_processor(ClarifaiVideoLogoDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-video-logo-model')
        assert 'Shell' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 4 == analysis['count']


class PublicModels:
    def __init__(self):
        self.face_detection_model = FaceDetectionModel()
        self.logo_model = LogoModel()


class FaceDetectionModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(os.path.dirname(__file__), '..', 'mock_data/clarifai_faces.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class LogoModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(os.path.dirname(__file__), '..', 'mock_data/clarifai_logo.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())
