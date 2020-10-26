import os
from unittest.mock import patch

from zmlp_analysis.clarifai.video import regions
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
        self.video_path = zorroa_test_path('video/ted_talk.mp4')
        asset = TestAsset(self.video_path)
        asset.set_attr('media.length', 15.0)
        self.frame = Frame(asset)

    @patch("zmlp_analysis.clarifai.video.regions.video.save_timeline", return_value={})
    @patch('zmlp_analysis.clarifai.video.regions.proxy.get_video_proxy')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_celebrity_process(self, _, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path

        processor = self.init_processor(regions.ClarifaiVideoCelebrityDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-celebrity-model')
        assert 'ryan gosling' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 1 == analysis['count']

    @patch("zmlp_analysis.clarifai.video.regions.video.save_timeline", return_value={})
    @patch('zmlp_analysis.clarifai.video.regions.proxy.get_video_proxy')
    @patch(client_patch, side_effect=MockClarifaiApp)
    def test_demographics_process(self, _, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path

        processor = self.init_processor(regions.ClarifaiVideoDemographicsDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-demographics-model')
        assert 'feminine' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 23 == analysis['count']


class PublicModels:
    def __init__(self):
        self.celebrity_model = CelebrityModel()
        self.demographics_model = DemographicsModel()


class CelebrityModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(
            os.path.dirname(__file__),
            '..',
            'mock_data/clarifai_celebrity.rsp'
        )
        with open(mock_data) as fp:
            return eval(fp.read())


class DemographicsModel:
    def predict_by_filename(self, filename):
        mock_data = os.path.join(
            os.path.dirname(__file__),
            '..',
            'mock_data/clarifai_demographics.rsp'
        )
        with open(mock_data) as fp:
            return eval(fp.read())
