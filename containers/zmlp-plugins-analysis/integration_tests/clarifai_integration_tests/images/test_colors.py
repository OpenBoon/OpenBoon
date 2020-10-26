import os
from unittest.mock import patch
import pytest

from zmlp_analysis.clarifai.images.colors import ClarifaiColorDetectionProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset, get_prediction_labels


@pytest.mark.skip(reason='dont run automatically')
class ClarifaiColorDetectionPublicModelsProcessorIntegrationTests(PluginUnitTestCase):

    def setUp(self):
        cred_location = os.path.join(os.path.dirname(__file__), '..', 'clarifai-creds')
        with open(cred_location, 'rb') as f:
            key = f.read().decode()
        os.environ['CLARIFAI_KEY'] = key

    def tearDown(self):
        del os.environ['CLARIFAI_KEY']

    @patch('zmlp_analysis.clarifai.images.colors.get_proxy_level_path')
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

    def test_color_process(self):
        self.run_process(
            image_path=zorroa_test_path('images/set05/color_test.png'),
            detector=ClarifaiColorDetectionProcessor(),
            attr='clarifai-color-model',
            assertions={'labels': ['Yellow', 'OrangeRed'], 'count': 4}
        )
