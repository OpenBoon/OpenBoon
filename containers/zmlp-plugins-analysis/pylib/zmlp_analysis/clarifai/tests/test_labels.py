import os
from unittest.mock import patch

import pytest
from clarifai.rest import ClarifaiApp

from zmlp_analysis.clarifai.labels import ClarifaiLabelDetectionProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, \
    TestAsset, get_prediction_labels

clarifai_api_key = "<KEY>"


class MockClarifaiApp(object):
    """
    Class to handle clarifai responses.
    """

    def __init__(self):

        class GeneralModel(object):
            def predict_by_filename(self, filename):
                with open(os.path.dirname(__file__) + "/clarifai.rsp") as fp:
                    return eval(fp.read())

        class PublicModels(object):
            def __init__(self):
                self.general_model = GeneralModel()

        self.public_models = PublicModels()


class ClarifaiPublicModelsProcessorTests(PluginUnitTestCase):

    def setUp(self):
        self.image_path = zorroa_test_path('images/detect/dogbike.jpg')
        self.frame = Frame(TestAsset(self.image_path))

    @patch('zmlp_analysis.clarifai.labels.get_proxy_level_path')
    @patch('zmlp_analysis.clarifai.labels.get_clarifai_app')
    def test_process(self, get_app_patch, proxy_path_patch):
        get_app_patch.return_value = MockClarifaiApp()
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiLabelDetectionProcessor(),
                                        {'general-model': True})
        processor.process(self.frame)

        analysis = self.frame.asset.get_attr('analysis.clarifai-label-general-model')
        assert 'wheel' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 20 == analysis['count']


@pytest.mark.skip(reason='dont run automatically')
class ClarifaiLabelDetectionPublicModelsProcessorIntegrationTests(PluginUnitTestCase):

    def setUp(self):
        self.capp = ClarifaiApp(api_key=clarifai_api_key)

    @patch('zmlp_analysis.clarifai.labels.get_proxy_level_path')
    @patch('zmlp_analysis.clarifai.labels.get_clarifai_app')
    def run_process(self, get_app_patch, proxy_path_patch,
                    image_path, model_name, attr, assertions):
        frame = Frame(TestAsset(image_path))

        get_app_patch.return_value = self.capp
        proxy_path_patch.return_value = image_path

        processor = self.init_processor(ClarifaiLabelDetectionProcessor(),
                                        {model_name: True})
        processor.process(frame)

        analysis = frame.asset.get_attr(attr)
        for label in assertions['labels']:
            assert label in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert assertions['count'] == analysis['count']

    def test_travel_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set06/gif_tahoe.gif'),
            model_name='travel-model',
            attr='analysis.clarifai-label-travel-model',
            assertions={'labels': ['Winter'], 'count': 7}
        )

    def test_food_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set02/beer_kettle_01.jpg'),
            model_name='food-model',
            attr='analysis.clarifai-label-food-model',
            assertions={'labels': ['beer'], 'count': 19}
        )

    def test_apparel_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/face-recognition/face2.jpg'),
            model_name='apparel-model',
            attr='analysis.clarifai-label-apparel-model',
            assertions={'labels': ['Necklace'], 'count': 6}
        )

    def test_wedding_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set11/wedding1.jpg'),
            model_name='wedding-model',
            attr='analysis.clarifai-label-wedding-model',
            assertions={'labels': ['bride'], 'count': 20}
        )

    def test_nsfw_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set10/nsfw1.jpg'),
            model_name='nsfw-model',
            attr='analysis.clarifai-label-nsfw-model',
            assertions={'labels': ['nsfw', 'sfw'], 'count': 2}
        )

    def test_moderation_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set10/nsfw1.jpg'),
            model_name='moderation-model',
            attr='analysis.clarifai-label-moderation-model',
            assertions={'labels': ['suggestive'], 'count': 1}
        )

    def test_textures_and_patterns_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set09/letter.png'),
            model_name='textures-and-patterns-model',
            attr='analysis.clarifai-textures-and-patterns-model',
            assertions={'labels': ['handwriting'], 'count': 1}
        )
