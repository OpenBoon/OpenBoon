import os

from unittest.mock import patch

from .conftest import MockS3Client
from zmlp_analysis.aws.videos import RekognitionVideoLabelDetection
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels, \
    zorroa_test_path, get_mock_stored_file
from zmlpsdk import file_storage

rek_patch_path = 'zmlp_analysis.aws.util.AwsEnv.rekognition'
s3_patch_path = 'zmlp_analysis.aws.util.AwsEnv.s3'

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

    @patch(s3_patch_path, side_effect=MockS3Client)
    def setUp(self, s3_patch):
        os.environ['ZMLP_PROJECT_ID'] = '00000000-0000-0000-0000-000000000001'
        os.environ['ZORROA_AWS_BUCKET'] = 'zorroa-unit-tests'

    @patch(s3_patch_path, side_effect=MockS3Client)
    @patch(rek_patch_path, side_effect=MockAWSClient)
    @patch("zmlp_analysis.aws.videos.labels.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.labels.proxy.get_video_proxy')
    def test_label_detection(self, get_vid_patch, store_patch, store_blob_patch, _, __, ___):
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
