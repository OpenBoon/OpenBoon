from unittest.mock import patch

from boonai_analysis.aws import RekognitionFaceDetection
from boonflow.base import Frame
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path, get_prediction_labels


class RekognitionFaceDetectionProcessorTests(PluginUnitTestCase):
    model_id = "model-id-12345"

    @patch("boonai_analysis.aws.faces.get_proxy_level_path")
    @patch('boonai_analysis.aws.util.AwsEnv.rekognition')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockAWSClient()

        image_path = test_path('images/face-recognition/face1.jpg')
        proxy_patch.return_value = image_path
        frame = Frame(TestAsset(image_path))

        args = {"model_id": "model-id-12345"}

        processor = self.init_processor(RekognitionFaceDetection(), args)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-face-detection')
        assert 'face0' in get_prediction_labels(analysis)
        assert analysis['predictions'][0]['bbox'] == [
            0.2811230421066284,
            0.4751185476779938,
            0.2983958963304758,
            0.5119047239422798
        ]


class MockAWSClient:

    def detect_faces(self, Image=None):
        return {
            'FaceDetails': [
                {
                    'BoundingBox': {
                        "Width": 0.01727285422384739,
                        "Height": 0.03678617626428604,
                        "Left": 0.2811230421066284,
                        "Top": 0.4751185476779938,
                    },
                    'Confidence': 99.90
                }
            ]
        }

    def compare_faces(self, SimilarityThreshold=None, SourceImage=None, TargetImage=None):
        return {
            'FaceMatches': [
                {
                    'Face': {
                        'BoundingBox': {
                            "Width": 0.01727285422384739,
                            "Height": 0.03678617626428604,
                            "Left": 0.2811230421066284,
                            "Top": 0.4751185476779938,
                        },
                        'Confidence': 99.90
                    }
                }
            ]
        }


class MockLabelDetectionAnalysis:

    def __init__(self):
        self.min_score = 0.50
