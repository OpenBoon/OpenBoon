# flake8: noqa
import os
from unittest.mock import patch
import pytest
from clarifai.rest import ClarifaiApp

from zmlp_analysis.clarifai.bboxes import *
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset, get_prediction_labels


@pytest.mark.skip(reason='dont run automatically')
class ClarifaiBboxDetectionPublicModelsProcessorIntegrationTests(PluginUnitTestCase):

    def setUp(self):
        cred_location = os.path.dirname(__file__) + '/clarifai-creds'
        with open(cred_location, 'rb') as f:
            key = f.read().decode()
        os.environ['CLARIFAI_KEY'] = key

    def tearDown(self):
        del os.environ['CLARIFAI_KEY']

    @patch('zmlp_analysis.clarifai.bboxes.get_proxy_level_path')
    def run_process(self, proxy_path_patch,
                    image_path, detector, attr, assertions):
        frame = Frame(TestAsset(image_path))
        proxy_path_patch.return_value = image_path

        processor = self.init_processor(detector)
        processor.process(frame)

        analysis = frame.asset.get_analysis(attr)
        for label in assertions['labels']:
            assert label in get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert assertions['count'] == analysis['count']

    def test_face_detection_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set01/faces.jpg'),
            detector=ClarifaiFaceDetectionProcessor(),
            attr='clarifai-face-detection-model',
            assertions={'labels': ['face'], 'count': 2}
        )

    def test_logo_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set11/logos.jpg'),
            detector=ClarifaiLogoDetectionProcessor(),
            attr='clarifai-logo-model',
            assertions={'labels': ['Shell', 'Target', 'Starbucks', 'Nike'], 'count': 4}
        )