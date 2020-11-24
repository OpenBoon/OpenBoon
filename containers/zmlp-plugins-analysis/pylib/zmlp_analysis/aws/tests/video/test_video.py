import os
import logging

from unittest.mock import patch
from .conftest import MockS3Client, MockRekClient, mock_clients

from zmlp_analysis.aws.videos.video import BlackFramesVideoDetectProcessor,  \
    EndCreditsVideoDetectProcessor
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_mock_stored_file

logging.basicConfig()

VID_MP4 = "video/credits.mov"
MUSTANG = "video/mustang.mp4"
MEDIA_LENGTH = 101.0

general_patch_path = 'zmlp_analysis.aws.util.AwsEnv.general_aws_client'
rek_patch_path = 'zmlp_analysis.aws.util.AwsEnv.rekognition'
s3_patch_path = 'zmlp_analysis.aws.util.AwsEnv.s3'


class RekognitionVideoDetectionProcessorTests(PluginUnitTestCase):

    @patch(s3_patch_path, side_effect=MockS3Client)
    def setUp(self, s3_patch):
        os.environ['ZMLP_PROJECT_ID'] = '00000000-0000-0000-0000-000000000001'
        os.environ['ZORROA_AWS_BUCKET'] = 'zorroa-unit-tests'
        os.environ['PATH'] += ':/usr/local/bin'

    @patch(general_patch_path, side_effect=mock_clients)
    @patch(s3_patch_path, side_effect=MockS3Client)
    @patch(rek_patch_path, side_effect=MockRekClient)
    @patch("zmlp_analysis.aws.videos.video.video.save_timeline", return_value={})
    @patch('zmlp_analysis.aws.videos.video.util.get_sqs_message_success', return_value=True)
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.video.proxy.get_video_proxy')
    def test_process_black_frame_detection(self, get_prx_patch, store_patch, store_blob_patch,
                                           _, __, ___, ____, _____):
        video_path = zorroa_test_path(VID_MP4)
        get_prx_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(BlackFramesVideoDetectProcessor())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(asset)
        processor.process(frame)

    @patch(general_patch_path, side_effect=mock_clients)
    @patch(s3_patch_path, side_effect=MockS3Client)
    @patch(rek_patch_path, side_effect=MockRekClient)
    @patch("zmlp_analysis.aws.videos.video.video.save_timeline", return_value={})
    @patch('zmlp_analysis.aws.videos.video.util.get_sqs_message_success', return_value=True)
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.video.proxy.get_video_proxy')
    def test_process_end_credits_detection(self, get_prx_patch, store_patch, store_blob_patch,
                                           _, __, ___, ____, _____):
        video_path = zorroa_test_path(VID_MP4)
        get_prx_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(EndCreditsVideoDetectProcessor())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(asset)
        processor.process(frame)
