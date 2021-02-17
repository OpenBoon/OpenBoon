import os
from unittest.mock import patch

from boonai_analysis.clarifai.video import labels
from boonflow import Frame
from boonflow.testing import PluginUnitTestCase, test_path, TestAsset, get_prediction_labels
from .util import mock_data_dir

client_patch = 'boonai_analysis.clarifai.util.ClarifaiApp'


class MockClarifaiApp:
    """
    Class to handle clarifai responses.
    """

    def __init__(self, api_key=None):
        self.public_models = PublicModels()


class ClarifaiPublicModelsProcessorTests(PluginUnitTestCase):

    def setUp(self):
        self.video_path = test_path('video/ted_talk.mp4')
        asset = TestAsset(self.video_path)
        asset.set_attr('media.length', 15.0)
        self.frame = Frame(asset)

    @patch("boonai_analysis.clarifai.video.labels.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.video.labels.proxy.get_video_proxy')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_general_process(self, _, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path

        processor = self.init_processor(labels.ClarifaiVideoLabelDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-label-detection')
        assert 'wheel' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 20 == analysis['count']
        assert 0.986 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.video.labels.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.video.labels.proxy.get_video_proxy')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_travel_process(self, _, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path

        processor = self.init_processor(labels.ClarifaiVideoTravelDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-travel-detection')
        assert 'labels' in analysis['type']
        assert 20 == analysis['count']
        assert 0.831 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.video.labels.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.video.labels.proxy.get_video_proxy')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_apparel_process(self, _, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path

        processor = self.init_processor(labels.ClarifaiVideoApparelDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-apparel-detection')
        assert 'Earring' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 20 == analysis['count']
        assert 0.813 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.video.labels.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.video.labels.proxy.get_video_proxy')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_wedding_process(self, _, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path

        processor = self.init_processor(labels.ClarifaiVideoWeddingDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-wedding-detection')
        assert 'bride' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 20 == analysis['count']
        assert 0.948 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.video.labels.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.video.labels.proxy.get_video_proxy')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_nsfw_process(self, _, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path

        processor = self.init_processor(labels.ClarifaiVideoExplicitDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-nsfw-detection')
        assert 'nsfw' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 2 == analysis['count']
        assert 0.724 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.video.labels.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.video.labels.proxy.get_video_proxy')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_moderation_process(self, _, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path

        processor = self.init_processor(labels.ClarifaiVideoModerationDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-unsafe-detection')
        assert 'suggestive' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 5 == analysis['count']
        assert 0.925 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.video.labels.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.video.labels.proxy.get_video_proxy')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_textures_and_patterns_process(self, _, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path

        processor = self.init_processor(labels.ClarifaiVideoTexturesDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-texture-detection')
        assert 'labels' in analysis['type']
        assert 20 == analysis['count']
        assert 0.637 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.video.labels.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.video.labels.proxy.get_video_proxy')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_food_process(self, _, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path

        processor = self.init_processor(labels.ClarifaiVideoFoodDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-food-detection')
        assert 'coffee' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 20 == analysis['count']
        assert 0.997 == analysis['predictions'][0]['score']


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
        mock_data = os.path.join(mock_data_dir, 'clarifai.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class FoodModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(mock_data_dir, 'clarifai_food.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class TravelModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(mock_data_dir, 'clarifai_travel.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class ApparelModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(mock_data_dir, 'clarifai_apparel.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class WeddingModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(mock_data_dir, 'clarifai_wedding.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class ExplicitModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(mock_data_dir, 'clarifai_nsfw.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class ModerationModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(mock_data_dir, 'clarifai_moderation.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())


class TexturesModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(mock_data_dir, 'clarifai_textures.rsp')
        with open(mock_data) as fp:
            return eval(fp.read())
