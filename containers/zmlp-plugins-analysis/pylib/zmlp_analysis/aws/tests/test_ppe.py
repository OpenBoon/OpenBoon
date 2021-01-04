import os
import json
from unittest.mock import patch

from zmlp_analysis.aws import RekognitionPPEDetection
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels


class RekognitionFaceDetectionProcessorTests(PluginUnitTestCase):
    model_id = "model-id-12345"

    @patch("zmlp_analysis.aws.ppe.get_proxy_level_path")
    @patch('zmlp_analysis.aws.util.AwsEnv.rekognition')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockAWSClient()

        image_path = zorroa_test_path('images/face-recognition/face1.jpg')
        proxy_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))

        args = {"model_id": self.model_id}

        processor = self.init_processor(RekognitionPPEDetection(), args)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        analysis = frame.asset.get_analysis('aws-ppe-detection')
        assert 'FACE_COVER' in get_prediction_labels(analysis)
        assert analysis['predictions'][0]['bbox'] == [
            0.5334620475769043,
            0.18087884783744812,
            0.5812848322093487,
            0.24308180063962936
        ]


class MockAWSClient:

    def detect_protective_equipment(self, Image=None):
        ppe_json = os.path.join(os.path.dirname(__file__), "mock-data/ppe_data.json")
        with open(ppe_json, 'r') as fp:
            return json.load(fp)


class MockLabelDetectionAnalysis:

    def __init__(self):
        self.min_score = 0.50
