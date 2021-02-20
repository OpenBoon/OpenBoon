import os
from unittest.mock import patch
import pytest

from boonai_analysis.clarifai.video import labels
from boonflow import Frame
from boonflow.testing import PluginUnitTestCase, test_path, TestAsset, get_prediction_labels


@pytest.mark.skip(reason='dont run automatically')
class ClarifaiLabelDetectionPublicModelsProcessorIntegrationTests(PluginUnitTestCase):

    def setUp(self):
        cred_location = os.path.join(os.path.dirname(__file__), '..', 'clarifai-creds')
        with open(cred_location, 'rb') as f:
            key = f.read().decode()
        os.environ['CLARIFAI_KEY'] = key

    def tearDown(self):
        del os.environ['CLARIFAI_KEY']

    @patch("boonai_analysis.clarifai.video.labels.video.save_timeline", return_value={})
    @patch('boonai_analysis.clarifai.video.labels.proxy.get_video_proxy')
    def run_process(self, proxy_path_patch, _, video_path, detector, attr, assertions):
        proxy_path_patch.return_value = video_path

        processor = self.init_processor(detector)
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)

        processor.process(frame)

        analysis = frame.asset.get_analysis(attr)
        predictions = get_prediction_labels(analysis)
        for label in assertions['labels']:
            assert label in predictions
        assert 'labels' in analysis['type']
        assert assertions['count'] == analysis['count']

    def test_general_process(self):
        self.run_process(
            video_path=test_path('video/ted_talk.mp4'),
            detector=labels.ClarifaiVideoLabelDetectionProcessor(),
            attr='clarifai-label-detection',
            assertions={'labels': ['performance', 'music', 'stage'], 'count': 31}
        )

    def test_travel_process(self):
        self.run_process(
            video_path=test_path('video/sample_ipad.m4v'),
            detector=labels.ClarifaiVideoTravelDetectionProcessor(),
            attr='clarifai-travel-detection',
            assertions={'labels': ['Snow & Ski Sports', 'Kids Area', 'Winter'], 'count': 32}
        )

    def test_food_process(self):
        self.run_process(
            video_path=test_path('video/beer.mp4'),
            detector=labels.ClarifaiVideoFoodDetectionProcessor(),
            attr='clarifai-food-detection',
            assertions={'labels': ['beer', 'alcohol'], 'count': 20}
        )

    def test_apparel_process(self):
        self.run_process(
            video_path=test_path('video/wedding.mp4'),
            detector=labels.ClarifaiVideoApparelDetectionProcessor(),
            attr='clarifai-apparel-detection',
            assertions={'labels': ['Wedding Dress', 'Necklace'], 'count': 20}
        )

    def test_wedding_process(self):
        self.run_process(
            video_path=test_path('video/out.mp4'),
            detector=labels.ClarifaiVideoWeddingDetectionProcessor(),
            attr='clarifai-wedding-detection',
            assertions={'labels': ['love', 'vows'], 'count': 27}
        )

    def test_nsfw_process(self):
        self.run_process(
            video_path=test_path('video/model.mp4'),
            detector=labels.ClarifaiVideoExplicitDetectionProcessor(),
            attr='clarifai-nsfw-detection',
            assertions={'labels': ['sfw'], 'count': 2}
        )

    def test_moderation_process(self):
        self.run_process(
            video_path=test_path('video/model.mp4'),
            detector=labels.ClarifaiVideoModerationDetectionProcessor(),
            attr='clarifai-unsafe-detection',
            assertions={'labels': ['safe', 'suggestive'], 'count': 5}
        )

    def test_textures_and_patterns_process(self):
        self.run_process(
            video_path=test_path('video/beach.mp4'),
            detector=labels.ClarifaiVideoTexturesDetectionProcessor(),
            attr='clarifai-texture-detection',
            assertions={'labels': ['sand'], 'count': 20}
        )
