from unittest.mock import patch

from zmlp_analysis.aws.videos import RekognitionVideoFaceDetection
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels, \
    zorroa_test_path, get_mock_stored_file
from zmlpsdk import file_storage

patch_path = 'zmlp_analysis.aws.util.AwsEnv.rekognition'

VID_MP4 = "video/ted_talk.mp4"


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


class RekognitionVideoLabelDetectionProcessorTests(PluginUnitTestCase):

    @patch(patch_path, side_effect=MockAWSClient)
    @patch("zmlp_analysis.aws.videos.faces.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.faces.proxy.get_video_proxy')
    def test_label_detection(self, get_vid_patch, store_patch, store_blob_patch, _, __):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.aws-face-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(RekognitionVideoFaceDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'face0' in predictions
