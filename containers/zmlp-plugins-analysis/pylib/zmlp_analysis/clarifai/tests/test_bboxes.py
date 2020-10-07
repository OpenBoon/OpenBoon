# from unittest.mock import patch
#
# import pytest
# from clarifai.rest import ClarifaiApp
#
# from zmlp_analysis.clarifai.bboxes import ClarifaiBoundingBoxDetectionProcessor
# from zmlpsdk import Frame
# from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, \
#     TestAsset, get_prediction_labels
#
# clarifai_api_key = "<KEY>"
#
#
# @pytest.mark.skip(reason='dont run automatically')
# class ClarifaiBoundingBoxDetectionPublicModelsProcessorIntegrationTests(PluginUnitTestCase):
#
#     def setUp(self):
#         self.capp = ClarifaiApp(api_key=clarifai_api_key)
#
#     @patch('zmlp_analysis.clarifai.bboxes.get_proxy_level_path')
#     @patch('zmlp_analysis.clarifai.bboxes.get_clarifai_app')
#     def run_process(self, get_app_patch, proxy_path_patch,
#                     image_path, model_name, attr, assertions):
#         frame = Frame(TestAsset(image_path))
#
#         get_app_patch.return_value = self.capp
#         proxy_path_patch.return_value = image_path
#
#         processor = self.init_processor(ClarifaiBoundingBoxDetectionProcessor(),
#                                         {model_name: True})
#         processor.process(frame)
#
#         analysis = frame.asset.get_attr(attr)
#         for label in assertions['labels']:
#             assert label in get_prediction_labels(analysis)
#         assert 'labels' in analysis['type']
#         assert assertions['count'] == analysis['count']
#
#     def test_face_detection_process(self):
#         self.run_process(
#             image_path=zorroa_test_path('images/set11/wedding1.jpg'),
#             model_name='face-detection-model',
#             attr='analysis.clarifai-bbox-face-detection-model',
#             assertions={'labels': ['face'], 'count': 3}
#         )
#
#     def test_logo_process(self):
#         self.run_process(
#             image_path=zorroa_test_path('images/set11/logos.jpg'),
#             model_name='logo-model',
#             attr='analysis.clarifai-bbox-logo-model',
#             assertions={'labels': ['Shell', 'Target', 'Nike', 'Starbucks'], 'count': 4}
#         )


# flake8: noqa
import os
from unittest.mock import patch

from zmlp_analysis.clarifai.bboxes import *
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
        self.image_path = zorroa_test_path('images/detect/dogbike.jpg')
        self.frame = Frame(TestAsset(self.image_path))

    @patch('zmlp_analysis.clarifai.bboxes.get_proxy_level_path')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_face_detection_process(self, _, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiFaceDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-face-detection-model')
        assert 'face' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 1 == analysis['count']

    @patch('zmlp_analysis.clarifai.bboxes.get_proxy_level_path')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_logo_process(self, _, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiLogoDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-logo-model')
        assert 'Shell' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 4 == analysis['count']


class PublicModels:
    def __init__(self):
        self.face_detection_model = FaceDetectionModel()
        self.logo_model = LogoModel()


class FaceDetectionModel:
    def predict_by_filename(self, filename):
        with open(os.path.dirname(__file__) + "/mock_data/clarifai_faces.rsp") as fp:
            return eval(fp.read())


class LogoModel:
    def predict_by_filename(self, filename):
        with open(os.path.dirname(__file__) + "/mock_data/clarifai_logo.rsp") as fp:
            return eval(fp.read())
