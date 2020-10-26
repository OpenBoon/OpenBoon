from unittest.mock import patch
from pytest import approx

from zmlp_analysis.aws.videos import RekognitionVideoUnsafeDetection
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels, \
    zorroa_test_path, get_mock_stored_file
from zmlpsdk import file_storage

patch_path = 'zmlp_analysis.aws.util.AwsEnv.rekognition'

VID_MP4 = "video/ted_talk.mp4"


class MockAWSClient:

    def detect_moderation_labels(self, Image=None, MinConfidence=60):
        return {
            'ModerationLabels': [
                {
                    'Name': 'Suggestive',
                    'Confidence': 65.14
                },
                {
                    'Name': 'Male Swimwear Or Underwear',
                    'Confidence': 65.14
                }
            ]
        }


class RekognitionVideoUnsafeDetectionProcessorTests(PluginUnitTestCase):

    @patch(patch_path, side_effect=MockAWSClient)
    @patch("zmlp_analysis.aws.videos.nsfw.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.nsfw.proxy.get_video_proxy')
    def test_unsafe_detection(self, get_vid_patch, store_patch, store_blob_patch, _, __):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.aws-unsafe-detection'

        get_vid_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(RekognitionVideoUnsafeDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        assert 'Suggestive' in get_prediction_labels(analysis)
        assert 'Male Swimwear Or Underwear' in get_prediction_labels(analysis)
        assert analysis['count'] == 2


expected_results = [
    (
        {"model_id": "model-id-12345"},
        [
            ('Suggestive', approx(0.6514, 0.0001)),
            ('Male Swimwear Or Underwear', approx(0.6514, 0.0001))
        ]
    )
]
