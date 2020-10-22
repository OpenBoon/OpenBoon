# flake8: noqa
import os
from unittest.mock import patch
import pytest

from zmlp_analysis.clarifai.video.labels import *
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset, get_prediction_labels


@pytest.mark.skip(reason='dont run automatically')
class ClarifaiLabelDetectionPublicModelsProcessorIntegrationTests(PluginUnitTestCase):

    def setUp(self):
        cred_location = os.path.join(os.path.dirname(__file__), '..', 'clarifai-creds')
        with open(cred_location, 'rb') as f:
            key = f.read().decode()
        os.environ['CLARIFAI_KEY'] = key

    def tearDown(self):
        del os.environ['CLARIFAI_KEY']

    @patch("zmlp_analysis.clarifai.video.labels.video.save_timeline", return_value={})
    @patch('zmlp_analysis.clarifai.video.labels.proxy.get_video_proxy')
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
            video_path=zorroa_test_path('video/ted_talk.mp4'),
            detector=ClarifaiVideoLabelDetectionProcessor(),
            attr='clarifai-video-general-model',
            assertions={'labels': ['performance', 'music', 'stage'], 'count': 31}
        )

    def test_travel_process(self):
        self.run_process(
            video_path=zorroa_test_path('video/sample_ipad.m4v'),
            detector=ClarifaiVideoTravelDetectionProcessor(),
            attr='clarifai-video-travel-model',
            assertions={'labels': ['Snow & Ski Sports', 'Kids Area', 'Winter'], 'count': 10}
        )

    def test_food_process(self):
        self.run_process(
            video_path=zorroa_test_path('video/beer.mp4'),
            detector=ClarifaiVideoFoodDetectionProcessor(),
            attr='clarifai-video-food-model',
            assertions={'labels': ['beer', 'alcohol'], 'count': 20}
        )

    def test_apparel_process(self):
        self.run_process(
            video_path=zorroa_test_path('video/wedding.mp4'),
            detector=ClarifaiVideoApparelDetectionProcessor(),
            attr='clarifai-video-apparel-model',
            assertions={'labels': ['Wedding Dress', 'Necklace'], 'count': 9}
        )

    def test_wedding_process(self):
        self.run_process(
            video_path=zorroa_test_path('video/out.mp4'),
            detector=ClarifaiVideoWeddingDetectionProcessor(),
            attr='clarifai-video-wedding-model',
            assertions={'labels': ['love', 'vows'], 'count': 20}
        )

    def test_nsfw_process(self):
        self.run_process(
            video_path=zorroa_test_path('video/model.mp4'),
            detector=ClarifaiVideoExplicitDetectionProcessor(),
            attr='clarifai-video-nsfw-model',
            assertions={'labels': ['sfw'], 'count': 1}
        )

    def test_moderation_process(self):
        self.run_process(
            video_path=zorroa_test_path('video/model.mp4'),
            detector=ClarifaiVideoModerationDetectionProcessor(),
            attr='clarifai-video-moderation-model',
            assertions={'labels': ['safe', 'suggestive'], 'count': 2}
        )

    def test_textures_and_patterns_process(self):
        self.run_process(
            video_path=zorroa_test_path('video/beach.mp4'),
            detector=ClarifaiVideoTexturesDetectionProcessor(),
            attr='clarifai-video-textures-and-patterns-model',
            assertions={'labels': ['sand'], 'count': 1}
        )
