import os

from unittest.mock import patch
from zmlp_analysis.aws.tests.video.conftest import MockS3Client, MockRekClient

from zmlp_analysis.aws import videos
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, \
    get_mock_stored_file, get_prediction_labels

VID_MP4 = "video/sample_ipad.m4v"
MEDIA_LENGTH = 101.0

rek_patch_path = 'zmlp_analysis.aws.util.AwsEnv.rekognition'
s3_patch_path = 'zmlp_analysis.aws.util.AwsEnv.s3'


class CustomLabelDetectionProcessorTests(PluginUnitTestCase):

    @patch(s3_patch_path, side_effect=MockS3Client)
    def setUp(self, s3_patch):
        os.environ['ZMLP_PROJECT_ID'] = '00000000-0000-0000-0000-000000000001'
        os.environ['ZORROA_AWS_BUCKET'] = 'zorroa-unit-tests'

    @patch(s3_patch_path, side_effect=MockS3Client)
    @patch(rek_patch_path, side_effect=MockRekClient)
    @patch("zmlp_analysis.aws.videos.custom.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.custom.proxy.get_video_proxy')
    def test_process_custom_label_detection(self, get_prx_patch, store_patch, store_blob_patch,
                                            _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        get_prx_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(videos.CustomLabelVideoDetectProcessor())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(asset)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-video-detection')
        preds = get_prediction_labels(analysis)
        assert 'bird' in preds
        assert analysis['count'] == 1
        assert analysis['predictions'][0]['score'] == 32.324
