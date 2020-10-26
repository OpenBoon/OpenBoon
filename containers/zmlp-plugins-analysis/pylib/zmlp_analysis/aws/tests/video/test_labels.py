from unittest.mock import patch
from pytest import approx

from zmlp_analysis.aws.videos import RekognitionVideoLabelDetection
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels, \
    zorroa_test_path, get_mock_stored_file
from zmlpsdk import file_storage

patch_path = 'zmlp_analysis.aws.util.AwsEnv.rekognition'

VID_MP4 = "video/ted_talk.mp4"


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
                }
            ]
        }


class RekognitionVideoLabelDetectionProcessorTests(PluginUnitTestCase):

    @patch(patch_path, side_effect=MockAWSClient)
    @patch("zmlp_analysis.aws.videos.labels.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.labels.proxy.get_video_proxy')
    def test_label_detection(self, get_vid_patch, store_patch, store_blob_patch, _, __):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.aws-label-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(RekognitionVideoLabelDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        predictions = get_prediction_labels(analysis)
        assert 'Plant' in predictions
        assert 'Daisy' in predictions
        assert analysis['count'] == 2


expected_results = [
    (
        {"model_id": "model-id-12345"},
        [
            ('Plant', approx(0.9990, 0.0001)),
            ('Daisy', approx(0.9959, 0.0001))
        ]
    )
]
