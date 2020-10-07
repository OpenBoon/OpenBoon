# flake8: noqa
import os
from unittest.mock import patch

from zmlp_analysis.clarifai.colors import *
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

    @patch('zmlp_analysis.clarifai.colors.get_proxy_level_path')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_color_process(self, _, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiColorDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-color-model')
        assert 'Yellow' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 4 == analysis['count']


class PublicModels:
    def __init__(self):
        self.color_model = ColorModel()


class ColorModel:
    def predict_by_filename(self, filename):
        with open(os.path.dirname(__file__) + "/mock_data/clarifai_colors.rsp") as fp:
            return eval(fp.read())
