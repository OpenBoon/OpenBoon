from unittest.mock import patch

from zmlp_analysis.aws import RekognitionFaceDetection
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels


class RekognitionFaceDetectionProcessorTests(PluginUnitTestCase):
    model_id = "model-id-12345"

    @patch("zmlp_analysis.aws.faces.get_proxy_level_path")
    @patch('zmlp_analysis.aws.faces.get_zvi_rekognition_client')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockAWSClient()

        image_path = zorroa_test_path('images/face-recognition/face1.jpg')
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

    @patch("zmlp_analysis.aws.faces.get_proxy_level_path")
    @patch('zmlp_analysis.aws.faces.get_zvi_rekognition_client')
    @patch("zmlp_analysis.aws.faces.LabelDetectionAnalysis")
    def test_compare(self, analysis_patch, client_patch, proxy_patch):
        client_patch.return_value = MockAWSClient()
        analysis_patch.return_value = MockLabelDetectionAnalysis()

        source_path = zorroa_test_path('images/face-recognition/face1.jpg')
        target_path = zorroa_test_path('images/face-recognition/face2.jpg')
        proxy_patch.return_value = target_path
        frame = Frame(TestAsset(target_path))

        args = {"model_id": "model-id-12345"}

        processor = self.init_processor(RekognitionFaceDetection(), args)
        results = processor.compare(source_path, target_path)

        r = results[0]
        assert r[0] == 'face0'  # label name
        assert r[1] == 99.90  # confidence score
        assert r[2] == [  # bounding box
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
