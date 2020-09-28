import os
from unittest.mock import patch

import pytest
from clarifai.rest import ClarifaiApp

from zmlp_analysis.clarifai.colors import ClarifaiColorDetectionProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, \
    TestAsset, get_prediction_labels

clarifai_api_key = "0cf7ca777e6a4ebeb44fffc6c241abd3"


@pytest.mark.skip(reason='dont run automatically')
class ClarifaiColorDetectionPublicModelsProcessorIntegrationTests(PluginUnitTestCase):

    def setUp(self):
        self.capp = ClarifaiApp(api_key=clarifai_api_key)

    @patch('zmlp_analysis.clarifai.colors.get_proxy_level_path')
    @patch('zmlp_analysis.clarifai.colors.get_clarifai_app')
    def run_process(self, get_app_patch, proxy_path_patch,
                    image_path, model_name, attr, assertions):
        frame = Frame(TestAsset(image_path))

        get_app_patch.return_value = self.capp
        proxy_path_patch.return_value = image_path

        processor = self.init_processor(ClarifaiColorDetectionProcessor(),
                                        {model_name: True})
        processor.process(frame)

        analysis = frame.asset.get_attr(attr)
        for label in assertions['labels']:
            assert label in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert assertions['count'] == analysis['count']

    def test_color_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set05/color_test.png'),
            model_name='color-model',
            attr='analysis.clarifai-color-color-model',
            assertions={'labels': ['Yellow', 'OrangeRed'], 'count': 4}
        )
