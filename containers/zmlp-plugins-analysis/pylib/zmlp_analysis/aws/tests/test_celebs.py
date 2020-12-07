import json
import os

from unittest.mock import patch

from zmlp_analysis.aws import RekognitionCelebrityDetection
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels


class RekognitionCelebritiesDetectionProcessorTests(PluginUnitTestCase):
    model_id = "model-id-12345"

    @patch("zmlp_analysis.aws.celebs.get_proxy_level_path")
    @patch('zmlp_analysis.aws.util.AwsEnv.rekognition')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockAWSClient()

        image_path = zorroa_test_path('images/set08/meme.jpg')
        proxy_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))

        args = {"model_id": "model-id-12345"}

        processor = self.init_processor(RekognitionCelebrityDetection(), args)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-celebrity-detection')
        assert 'Ryan Gosling' in get_prediction_labels(analysis)


class MockAWSClient:

    def recognize_celebrities(self, Image=None):
        with open(os.path.dirname(__file__) + "/celebs.json", "r") as fp:
            return json.load(fp)
