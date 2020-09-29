from unittest.mock import patch

import pytest
from clarifai.rest import ClarifaiApp

from zmlp_analysis.clarifai.bboxes import ClarifaiBoundingBoxDetectionProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, \
    TestAsset, get_prediction_labels

clarifai_api_key = "<KEY>"


@pytest.mark.skip(reason='dont run automatically')
class ClarifaiBoundingBoxDetectionPublicModelsProcessorIntegrationTests(PluginUnitTestCase):

    def setUp(self):
        self.capp = ClarifaiApp(api_key=clarifai_api_key)

    @patch('zmlp_analysis.clarifai.bboxes.get_proxy_level_path')
    @patch('zmlp_analysis.clarifai.bboxes.get_clarifai_app')
    def run_process(self, get_app_patch, proxy_path_patch,
                    image_path, model_name, attr, assertions):
        frame = Frame(TestAsset(image_path))

        get_app_patch.return_value = self.capp
        proxy_path_patch.return_value = image_path

        processor = self.init_processor(ClarifaiBoundingBoxDetectionProcessor(),
                                        {model_name: True})
        processor.process(frame)

        analysis = frame.asset.get_attr(attr)
        for label in assertions['labels']:
            a = get_prediction_labels(analysis)
            assert label in a
        assert 'labels' in analysis['type']
        assert assertions['count'] == analysis['count']

    def test_face_detection_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set11/wedding1.jpg'),
            model_name='face-detection-model',
            attr='analysis.clarifai-bbox-face-detection-model',
            assertions={'labels': ['face'], 'count': 3}
        )

    def test_logo_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set11/logos.jpg'),
            model_name='logo-model',
            attr='analysis.clarifai-bbox-logo-model',
            assertions={'labels': ['Shell', 'Target', 'Nike', 'Starbucks'], 'count': 4}
        )
