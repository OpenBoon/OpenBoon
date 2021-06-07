from unittest.mock import patch

from boonai_analysis.clarifai.util import MockClarifaiPredictionResponse, \
    RecursiveNamespace
from boonai_analysis.clarifai.video import labels
from boonflow import Frame
from boonflow.testing import PluginUnitTestCase, test_path, TestAsset, \
    get_prediction_labels


class ClarifaiPublicModelsProcessorTests(PluginUnitTestCase):

    def setUp(self):
        self.video_path = test_path('video/ted_talk.mp4')
        asset = TestAsset(self.video_path)
        asset.set_attr('media.length', 15.0)
        self.frame = Frame(asset)

    @patch("boonai_analysis.clarifai.util.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.util.proxy.get_video_proxy')
    @patch.object(labels.ClarifaiVideoLabelDetectionProcessor, 'predict')
    def test_general_process(self, predict_patch, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path
        mock_response = MockClarifaiPredictionResponse()
        mock_response.outputs[0].data.concepts = [RecursiveNamespace(**{'app_id': 'main', 'id': 'ai_l8TKp2h5', 'name': 'people', 'value': 0.9857286214828491}), RecursiveNamespace(**{'app_id': 'main', 'id': 'ai_gRZHdRD7', 'name': 'wheel', 'value': 0.9602692127227783})]  # noqa
        predict_patch.return_value = mock_response

        processor = self.init_processor(labels.ClarifaiVideoLabelDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-label-detection')
        assert 'wheel' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 2 == analysis['count']
        assert 0.986 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.util.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.util.proxy.get_video_proxy')
    @patch.object(labels.ClarifaiVideoTravelDetectionProcessor, 'predict')
    def test_travel_process(self, predict_patch, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path
        mock_response = MockClarifaiPredictionResponse()
        mock_response.outputs[0].data.concepts = [RecursiveNamespace(**{'id': 'ai_hjvL4F9K', 'name': 'Winter', 'value': 0.83090353, 'app_id': 'main'}), RecursiveNamespace(**{'id': 'ai_4NvhWszJ', 'name': 'Snow & Ski Sports', 'value': 0.79168034, 'app_id': 'main'})]  # noqa
        predict_patch.return_value = mock_response

        processor = self.init_processor(labels.ClarifaiVideoTravelDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-travel-detection')
        assert 'labels' in analysis['type']
        assert 2 == analysis['count']
        assert 0.831 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.util.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.util.proxy.get_video_proxy')
    @patch.object(labels.ClarifaiVideoApparelDetectionProcessor, 'predict')
    def test_apparel_process(self, predict_patch, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path
        mock_response = MockClarifaiPredictionResponse()
        mock_response.outputs[0].data.concepts = [RecursiveNamespace(**{'id': 'ai_2KGqNjLM', 'name': 'Earring', 'value': 0.81278193, 'app_id': 'main'}), RecursiveNamespace(**{'id': 'ai_cjhVr9Tf', 'name': 'Necklace', 'value': 0.44886035, 'app_id': 'main'})]  # noqa
        predict_patch.return_value = mock_response

        processor = self.init_processor(labels.ClarifaiVideoApparelDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-apparel-detection')
        assert 'Earring' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 2 == analysis['count']
        assert 0.813 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.util.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.util.proxy.get_video_proxy')
    @patch.object(labels.ClarifaiVideoWeddingDetectionProcessor, 'predict')
    def test_wedding_process(self, predict_patch, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path
        mock_response = MockClarifaiPredictionResponse()
        mock_response.outputs[0].data.concepts = [RecursiveNamespace(**{'id': 'ai_pQlX4jGW', 'name': 'bride', 'value': 0.9476397, 'app_id': 'main'}), RecursiveNamespace(**{'id': 'ai_5GsSN3JG', 'name': 'bouquet', 'value': 0.9432099, 'app_id': 'main'})]  # noqa
        predict_patch.return_value = mock_response

        processor = self.init_processor(labels.ClarifaiVideoWeddingDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-wedding-detection')
        assert 'bride' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 2 == analysis['count']
        assert 0.948 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.util.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.util.proxy.get_video_proxy')
    @patch.object(labels.ClarifaiVideoExplicitDetectionProcessor, 'predict')
    def test_nsfw_process(self, predict_patch, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path
        mock_response = MockClarifaiPredictionResponse()
        mock_response.outputs[0].data.concepts = [RecursiveNamespace(**{'id': 'ai_KxzHKtPl', 'name': 'nsfw', 'value': 0.724364, 'app_id': 'main'}), RecursiveNamespace(**{'id': 'ai_RT20lm2Q', 'name': 'sfw', 'value': 0.275636, 'app_id': 'main'})]  # noqa
        predict_patch.return_value = mock_response

        processor = self.init_processor(labels.ClarifaiVideoExplicitDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-nsfw-detection')
        assert 'nsfw' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 2 == analysis['count']
        assert 0.724 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.util.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.util.proxy.get_video_proxy')
    @patch.object(labels.ClarifaiVideoModerationDetectionProcessor, 'predict')
    def test_moderation_process(self, predict_patch, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path
        mock_response = MockClarifaiPredictionResponse()
        mock_response.outputs[0].data.concepts = [RecursiveNamespace(**{'id': 'ai_RtXh5qkR', 'name': 'suggestive', 'value': 0.9253318, 'app_id': 'main'}), RecursiveNamespace(**{'id': 'ai_V76bvrtj', 'name': 'explicit', 'value': 0.07466002, 'app_id': 'main'}), RecursiveNamespace(**{'id': 'ai_QD1zClSd', 'name': 'safe', 'value': 8.12024e-06, 'app_id': 'main'}), RecursiveNamespace(**{'id': 'ai_kBBGf7r8', 'name': 'gore', 'value': 5.6501545e-08, 'app_id': 'main'}), RecursiveNamespace(**{'id': 'ai_8QQwMjQR', 'name': 'drug', 'value': 3.6426066e-08, 'app_id': 'main'})]  # noqa
        predict_patch.return_value = mock_response

        processor = self.init_processor(labels.ClarifaiVideoModerationDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-unsafe-detection')
        assert 'suggestive' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 5 == analysis['count']
        assert 0.925 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.util.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.util.proxy.get_video_proxy')
    @patch.object(labels.ClarifaiVideoTexturesDetectionProcessor, 'predict')
    def test_textures_and_patterns_process(self, predict_patch, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path
        mock_response = MockClarifaiPredictionResponse()
        mock_response.outputs[0].data.concepts = [RecursiveNamespace(**{'id': 'ai_a93e5160e97c4d899e9d1d13f09a7438', 'name': 'handwriting', 'value': 0.6368297, 'app_id': 'main'}), RecursiveNamespace(**{'id': 'ai_ad896dc084b14f04882d849e0fa138d7', 'name': 'shibori', 'value': 0.017188853, 'app_id': 'main'})]  # noqa
        predict_patch.return_value = mock_response

        processor = self.init_processor(labels.ClarifaiVideoTexturesDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-texture-detection')
        assert 'labels' in analysis['type']
        assert 2 == analysis['count']
        assert 0.637 == analysis['predictions'][0]['score']

    @patch("boonai_analysis.clarifai.util.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.util.proxy.get_video_proxy')
    @patch.object(labels.ClarifaiVideoFoodDetectionProcessor, 'predict')
    def test_food_process(self, predict_patch, proxy_path_patch, __):
        proxy_path_patch.return_value = self.video_path
        mock_response = MockClarifaiPredictionResponse()
        mock_response.outputs[0].data.concepts = [RecursiveNamespace(**{'id': 'ai_f1zKlGnc', 'name': 'coffee', 'value': 0.9970342, 'app_id': 'main'}), RecursiveNamespace(**{'id': 'ai_dptdbnBR', 'name': 'espresso', 'value': 0.9809456, 'app_id': 'main'})]  # noqa
        predict_patch.return_value = mock_response

        processor = self.init_processor(labels.ClarifaiVideoFoodDetectionProcessor())
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis('clarifai-food-detection')
        assert 'coffee' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 2 == analysis['count']
        assert 0.997 == analysis['predictions'][0]['score']
