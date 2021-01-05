import os
from unittest.mock import patch
import pytest

from zmlp_analysis.clarifai.images import labels
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset, get_prediction_labels


@pytest.mark.skip(reason='dont run automatically')
class ClarifaiLabelDetectionPublicModelsProcessorIntegrationTests(PluginUnitTestCase):

    def setUp(self):
        super(ClarifaiLabelDetectionPublicModelsProcessorIntegrationTests, self).setUp()
        cred_location = os.path.join(os.path.dirname(__file__), '..', 'clarifai-creds')
        with open(cred_location, 'rb') as f:
            key = f.read().decode()
        os.environ['CLARIFAI_KEY'] = key

    def tearDown(self):
        super(ClarifaiLabelDetectionPublicModelsProcessorIntegrationTests, self).tearDown()
        del os.environ['CLARIFAI_KEY']

    @patch('zmlp_analysis.clarifai.images.labels.get_proxy_level_path')
    def run_process(self, proxy_path_patch,
                    image_path, detector, attr, assertions):
        frame = Frame(TestAsset(image_path))
        proxy_path_patch.return_value = image_path

        processor = self.init_processor(detector)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = frame.asset.get_analysis(attr)
        for label in assertions['labels']:
            assert label in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert assertions['count'] == analysis['count']

    def test_travel_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set06/gif_tahoe.gif'),
            detector=labels.ClarifaiTravelDetectionProcessor(),
            attr='clarifai-travel-detection',
            assertions={'labels': ['Winter'], 'count': 20}
        )

    def test_food_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set02/beer_kettle_01.jpg'),
            detector=labels.ClarifaiFoodDetectionProcessor(),
            attr='clarifai-food-detection',
            assertions={'labels': ['beer'], 'count': 20}
        )

    def test_apparel_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/face-recognition/face2.jpg'),
            detector=labels.ClarifaiApparelDetectionProcessor(),
            attr='clarifai-apparel-detection',
            assertions={'labels': ['Necklace'], 'count': 20}
        )

    def test_wedding_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set11/wedding1.jpg'),
            detector=labels.ClarifaiWeddingDetectionProcessor(),
            attr='clarifai-wedding-detection',
            assertions={'labels': ['bride'], 'count': 20}
        )

    def test_nsfw_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set10/nsfw1.jpg'),
            detector=labels.ClarifaiExplicitDetectionProcessor(),
            attr='clarifai-nsfw-detection',
            assertions={'labels': ['nsfw', 'sfw'], 'count': 2}
        )

    def test_moderation_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set10/nsfw1.jpg'),
            detector=labels.ClarifaiModerationDetectionProcessor(),
            attr='clarifai-unsafe-detection',
            assertions={'labels': ['suggestive'], 'count': 5}
        )

    def test_textures_and_patterns_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set09/letter.png'),
            detector=labels.ClarifaiTexturesDetectionProcessor(),
            attr='clarifai-texture-detection',
            assertions={'labels': ['handwriting'], 'count': 20}
        )
