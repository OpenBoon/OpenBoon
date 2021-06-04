from unittest.mock import patch

from boonai_analysis.clarifai.util import MockClarifaiPredictionResponse, RecursiveNamespace
from boonai_analysis.clarifai.video import bboxes
from boonflow import Frame
from boonflow.testing import PluginUnitTestCase, test_path, TestAsset, \
    get_prediction_labels


class ClarifaiPublicModelsProcessorTests(PluginUnitTestCase):

    def setUp(self):
        self.video_path = test_path('video/julia_roberts.mp4')
        asset = TestAsset(self.video_path)
        asset.set_attr('media.length', 15.0)
        self.frame = Frame(asset)

    @patch("boonai_analysis.clarifai.util.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.util.proxy.get_video_proxy')
    @patch.object(bboxes.ClarifaiVideoFaceDetectionProcessor, 'predict')
    def test_face_detection_process(self, predict_patch, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path
        mock_response = MockClarifaiPredictionResponse()
        mock_response.outputs[0].data.regions = [RecursiveNamespace(**{'id': '52ntkub3eip6', 'region_info': {'bounding_box': {'top_row': 0.37609282, 'left_col': 0.39236027, 'bottom_row': 0.68923295, 'right_col': 0.5498285}}, 'data': {'concepts': [{'id': 'ai_8jtPl3Xj', 'name': 'face', 'value': 0.9994281, 'app_id': 'main'}]}})]  # noqa
        predict_patch.return_value = mock_response

        processor = self.init_processor(bboxes.ClarifaiVideoFaceDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-face-detection')
        assert 'face' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 1 == analysis['count']

    @patch("boonai_analysis.clarifai.util.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.util.proxy.get_video_proxy')
    @patch.object(bboxes.ClarifaiVideoLogoDetectionProcessor, 'predict')
    def test_logo_process(self, predict_patch, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path
        mock_response = MockClarifaiPredictionResponse()
        mock_response.outputs[0].data.regions = [RecursiveNamespace(**{'id': 'f948wpk6xhiz', 'region_info': {'bounding_box': {'top_row': 0.5063246, 'left_col': 0.059234455, 'bottom_row': 0.8185034, 'right_col': 0.23586488}}, 'data': {'concepts': [{'id': 'ai_wt9Jd9gB', 'name': 'Shell', 'value': 0.9441121, 'app_id': 'main'}]}}), RecursiveNamespace(**{'id': 'c0mzyu36ygzq', 'region_info': {'bounding_box': {'top_row': 0.009347461, 'left_col': 0.36731383, 'bottom_row': 0.32438722, 'right_col': 0.54123837}}, 'data': {'concepts': [{'id': 'ai_R8tNfTM8', 'name': 'Target', 'value': 0.9171441, 'app_id': 'main'}]}})]  # noqa
        predict_patch.return_value = mock_response

        processor = self.init_processor(bboxes.ClarifaiVideoLogoDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-logo-detection')
        assert 'Shell' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 2 == analysis['count']
