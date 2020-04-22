import os
from unittest.mock import patch

import pytest
from clarifai.rest import ClarifaiApp

from zmlp_analysis.clarifai.labels import ClarifaiLabelDetectionProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, \
    TestAsset, get_prediction_labels


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

        analysis = self.frame.asset.get_attr('analysis.clarifai-general-model')
        assert 'wheel' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 20 == analysis['count']


@pytest.mark.skip(reason='dont run automatically')
class ClarifaiPublicModelsProcessorIntegrationTests(PluginUnitTestCase):

    def setUp(self):
        self.image_path = zorroa_test_path('images/set06/gif_tahoe.gif')
        self.image_path = zorroa_test_path('images/face-recognition/face2.jpg')
        self.frame = Frame(TestAsset(self.image_path))
        self.capp = ClarifaiApp(api_key="<KEY>")

    @patch('zmlp_analysis.clarifai.labels.get_proxy_level_path')
    @patch('zmlp_analysis.clarifai.labels.get_clarifai_app')
    def test_process(self, get_app_patch, proxy_path_patch):
        get_app_patch.return_value = self.capp
        proxy_path_patch.return_value = self.image_path

        processor = self.init_processor(ClarifaiLabelDetectionProcessor(),
                                        {'travel-model': True})
        processor.process(self.frame)

        analysis = self.frame.asset.get_attr('analysis.clarifai-travel-model')
        assert 'Winter' in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 20 == analysis['count']
