from unittest.mock import patch

from boonai_analysis.clarifai.images import bboxes
from boonai_analysis.clarifai.util import MockClarifaiPredictionResponse, \
    RecursiveNamespace
from boonflow import Frame
from boonflow.testing import PluginUnitTestCase, test_path, TestAsset, \
    get_prediction_labels


class ClarifaiPublicModelsProcessorTests(PluginUnitTestCase):

    def setUp(self):
        self.image_path = test_path('images/detect/dogbike.jpg')
        self.frame = Frame(TestAsset(self.image_path))

    @patch('boonai_analysis.clarifai.images.bboxes.get_proxy_level_path')
    @patch.object(bboxes.ClarifaiFaceDetectionProcessor, 'predict')
    def test_face_detection_process(self, predict_patch, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path
        mock_response = MockClarifaiPredictionResponse()
        mock_response.outputs[0].data.regions = [RecursiveNamespace(**{'id': '52ntkub3eip6', 'region_info': { 'bounding_box': { 'top_row': 0.37609282, 'left_col': 0.39236027, 'bottom_row': 0.68923295, 'right_col': 0.5498285}}, 'data': {'concepts': [{ 'id': 'ai_8jtPl3Xj', 'name': 'face', 'value': 0.9994281, 'app_id': 'main'}]}})]  # noqa
        predict_patch.return_value = mock_response

        processor = self.init_processor(bboxes.ClarifaiFaceDetectionProcessor())
        processor.process(self.frame)

        print(self.frame.asset.get_attr('analysis'))
        analysis = self.frame.asset.get_analysis('clarifai-face-detection')
        assert 'face' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 1 == analysis['count']

    @patch('boonai_analysis.clarifai.images.bboxes.get_proxy_level_path')
    @patch.object(bboxes.ClarifaiLogoDetectionProcessor, 'predict')
    def test_logo_process(self, predict_patch, proxy_path_patch):
        proxy_path_patch.return_value = self.image_path
        mock_response = MockClarifaiPredictionResponse()
        mock_response.outputs[0].data.regions = [RecursiveNamespace(**{'id': 'f948wpk6xhiz', 'region_info': {'bounding_box': {'top_row': 0.5063246, 'left_col': 0.059234455, 'bottom_row': 0.8185034, 'right_col': 0.23586488}}, 'data': {'concepts': [{'id': 'ai_wt9Jd9gB', 'name': 'Shell', 'value': 0.9441121, 'app_id': 'main'}]}}), RecursiveNamespace(**{'id': 'c0mzyu36ygzq', 'region_info': {'bounding_box': {'top_row': 0.009347461, 'left_col': 0.36731383, 'bottom_row': 0.32438722, 'right_col': 0.54123837}}, 'data': {'concepts': [{'id': 'ai_R8tNfTM8', 'name': 'Target', 'value': 0.9171441, 'app_id': 'main'}]}}), RecursiveNamespace(**{'id': 'jps2spneg3pl', 'region_info': {'bounding_box': {'top_row': 0.50345194, 'left_col': 0.3661345, 'bottom_row': 0.82065433, 'right_col': 0.5419154}}, 'data': {'concepts': [{'id': 'ai_j9DxLkP8', 'name': 'Starbucks', 'value': 0.53994125, 'app_id': 'main'}]}}), RecursiveNamespace(**{'id': '9scp2d8pjujj', 'region_info': {'bounding_box': {'top_row': 0.13935669, 'left_col': 0.08883309, 'bottom_row': 0.29017067, 'right_col': 0.17740436}}, 'data': {'concepts': [{'id': 'ai_3533DtSc', 'name': 'Nike', 'value': 0.2928831, 'app_id': 'main'}]}}), RecursiveNamespace(**{'id': 'lnqh3y34b2g6', 'region_info': {'bounding_box': {'top_row': 0.5017445, 'left_col': 0.6717983, 'bottom_row': 0.82335234, 'right_col': 0.85118574}}, 'data': {'concepts': [{'id': 'ai_TmWdpWdB', 'name': 'Apple Inc', 'value': 0.12959078, 'app_id': 'main'}]}}), RecursiveNamespace(**{'id': 'b74z5kmpxv8c', 'region_info': {'bounding_box': {'top_row': 0.019707976, 'left_col': 0.7070061, 'bottom_row': 0.31236735, 'right_col': 0.87143886}}, 'data': {'concepts': [{'id': 'ai_ghfXsrPJ', 'name': 'McDonalds', 'value': 0.070925675, 'app_id': 'main'}]}})]  # noqa
        predict_patch.return_value = mock_response

        processor = self.init_processor(bboxes.ClarifaiLogoDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-logo-detection')
        assert 'Shell' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 6 == analysis['count']
