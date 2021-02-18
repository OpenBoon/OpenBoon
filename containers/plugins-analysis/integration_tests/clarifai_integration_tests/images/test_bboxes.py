import os
from unittest.mock import patch
import pytest

from boonai_analysis.clarifai.images import bboxes
from boonflow import Frame
from boonflow.testing import PluginUnitTestCase, test_path, TestAsset, get_prediction_labels


@pytest.mark.skip(reason='dont run automatically')
class ClarifaiBboxDetectionPublicModelsProcessorIntegrationTests(PluginUnitTestCase):

    def setUp(self):
        cred_location = os.path.join(os.path.dirname(__file__), '..', 'clarifai-creds')
        with open(cred_location, 'rb') as f:
            key = f.read().decode()
        os.environ['CLARIFAI_KEY'] = key

    def tearDown(self):
        del os.environ['CLARIFAI_KEY']

    @patch('boonai_analysis.clarifai.images.bboxes.get_proxy_level_path')
    def run_process(self, proxy_path_patch, image_path, detector, attr, assertions):
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
            image_path=test_path('images/set01/faces.jpg'),
            detector=bboxes.ClarifaiFaceDetectionProcessor(),
            attr='clarifai-face-detection',
            assertions={'labels': ['face'], 'count': 2}
        )

    def test_logo_process(self):
        self.run_process(
            image_path=test_path('images/set11/logos.jpg'),
            detector=bboxes.ClarifaiLogoDetectionProcessor(),
            attr='clarifai-logo-detection',
            assertions={'labels': ['Shell', 'Target', 'Starbucks', 'Nike'], 'count': 6}
        )
