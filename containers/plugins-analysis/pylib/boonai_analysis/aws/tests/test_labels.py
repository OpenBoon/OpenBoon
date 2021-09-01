from unittest.mock import patch

from pytest import approx

from boonai_analysis.aws import RekognitionLabelDetection
from boonflow.base import Frame
from boonflow.testing import PluginUnitTestCase, TestAsset, \
    test_path, get_prediction_labels


class RekognitionLabelDetectionProcessorTests(PluginUnitTestCase):
    model_id = "model-id-12345"

    @patch("boonai_analysis.aws.labels.get_proxy_level_path")
    @patch('boonai_analysis.aws.util.AwsEnv.rekognition')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockAWSClient()

        flower_paths = test_path("training/test_dsy.jpg")
        proxy_patch.return_value = flower_paths
        frame = Frame(TestAsset(flower_paths))

        args = expected_results[0][0]

        processor = self.init_processor(RekognitionLabelDetection(), args)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-label-detection')
        assert 'Plant' in get_prediction_labels(analysis)
        assert 'Daisy' in get_prediction_labels(analysis)
        assert 'Cat' in get_prediction_labels(analysis)
        assert analysis['count'] == 3


expected_results = [
    (
        {"model_id": "model-id-12345"},
        [
            ('Plant', approx(0.9990, 0.0001)),
            ('Daisy', approx(0.9959, 0.0001))
        ]
    )
]


class MockAWSClient:

    def detect_labels(self, Image=None, MaxLabels=3):
        return {
            'Labels': [
                {
                    'Name': 'Plant',
                    'Confidence': 99.90
                },
                {
                    'Name': 'Daisy',
                    'Confidence': 99.59
                },
                {
                    'Name': 'Cat',
                    'Confidence': 101
                }
            ]
        }
