# flake8: noqa
import os
from unittest.mock import patch

from zmlp_analysis.clarifai.images.labels import *
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

    @patch('zmlp_analysis.clarifai.images.labels.get_proxy_level_path')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_general_process(self, _, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiLabelDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-general-model')
        assert 'wheel' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 20 == analysis['count']

    @patch('zmlp_analysis.clarifai.images.labels.get_proxy_level_path')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_food_process(self, _, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiFoodDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-food-model')
        assert 'coffee' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 19 == analysis['count']

    @patch('zmlp_analysis.clarifai.images.labels.get_proxy_level_path')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_travel_process(self, _, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiTravelDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-travel-model')
        assert 'Winter' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 7 == analysis['count']

    @patch('zmlp_analysis.clarifai.images.labels.get_proxy_level_path')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_apparel_process(self, _, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiApparelDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-apparel-model')
        assert 'Earring' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 6 == analysis['count']

    @patch('zmlp_analysis.clarifai.images.labels.get_proxy_level_path')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_wedding_process(self, _, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiWeddingDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-wedding-model')
        assert 'bride' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 20 == analysis['count']

    @patch('zmlp_analysis.clarifai.images.labels.get_proxy_level_path')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_nsfw_process(self, _, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiExplicitDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-nsfw-model')
        assert 'nsfw' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 2 == analysis['count']

    @patch('zmlp_analysis.clarifai.images.labels.get_proxy_level_path')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_moderation_process(self, _, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiModerationDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-moderation-model')
        assert 'suggestive' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 1 == analysis['count']

    @patch('zmlp_analysis.clarifai.images.labels.get_proxy_level_path')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_textures_and_patterns_process(self, _, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiTexturesDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-textures-and-patterns-model')
        assert 'handwriting' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 1 == analysis['count']


class PublicModels:
    def __init__(self):
        self.general_model = GeneralModel()
        self.food_model = FoodModel()
        self.travel_model = TravelModel()
        self.apparel_model = ApparelModel()
        self.wedding_model = WeddingModel()
        self.nsfw_model = ExplicitModel()
        self.moderation_model = ModerationModel()
        self.textures_and_patterns_model = TexturesModel()


class GeneralModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(os.path.dirname(__file__), '..', 'mock_data/clarifai.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class FoodModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(os.path.dirname(__file__), '..', 'mock_data/clarifai_food.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class TravelModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(os.path.dirname(__file__), '..', 'mock_data/clarifai_travel.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class ApparelModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(os.path.dirname(__file__), '..', 'mock_data/clarifai_apparel.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class WeddingModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(os.path.dirname(__file__), '..', 'mock_data/clarifai_wedding.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class ExplicitModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(os.path.dirname(__file__), '..', 'mock_data/clarifai_nsfw.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class ModerationModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(os.path.dirname(__file__), '..', 'mock_data/clarifai_moderation.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class TexturesModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(os.path.dirname(__file__), '..', 'mock_data/clarifai_textures.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())
