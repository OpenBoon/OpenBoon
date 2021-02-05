import logging
import os
from unittest.mock import patch

from zmlp_analysis.aws import video
from zmlp_analysis.aws.tests.conftest import MockS3Client, \
    MockRekClient, mock_clients, MockAwsCloudResources
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, \
    get_mock_stored_file, get_prediction_labels

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

    @patch(general_patch_path, side_effect=mock_clients)
    @patch(s3_patch_path, side_effect=MockS3Client)
    @patch(rek_patch_path, side_effect=MockRekClient)
    @patch("zmlp_analysis.aws.video.save_timeline", return_value={})
    @patch('zmlp_analysis.aws.video.AwsCloudResources', autospec=True)
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.proxy.get_video_proxy')
    def test_process_label_detection(self, get_prx_patch, store_patch,
                                     store_blob_patch, aws_patch, tl_patch, _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        get_prx_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()
        aws_patch.return_value = MockAwsCloudResources()

        processor = self.init_processor(video.RekognitionLabelDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(asset)
        processor.jobs = {asset.id: "12456"}
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-label-detection')
        preds = get_prediction_labels(analysis)
        assert 'Word' in preds
        assert analysis['count'] == 1

        timeline = tl_patch.call_args_list[0][0][1]
        jtl = timeline.for_json()
        assert jtl['tracks'][0]['name'] == 'Word'
        assert int(jtl['tracks'][0]['clips'][0]['score']) == 60

    @patch(general_patch_path, side_effect=mock_clients)
    @patch(s3_patch_path, side_effect=MockS3Client)
    @patch(rek_patch_path, side_effect=MockRekClient)
    @patch("zmlp_analysis.aws.video.save_timeline", return_value={})
    @patch('zmlp_analysis.aws.video.AwsCloudResources', autospec=True)
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.proxy.get_video_proxy')
    def test_process_unsafe_detection(self, get_prx_patch, store_patch,
                                      store_blob_patch, aws_patch, tl_patch, _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        get_prx_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()
        aws_patch.return_value = MockAwsCloudResources()

        processor = self.init_processor(video.RekognitionUnsafeDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(asset)
        processor.jobs = {asset.id: "12456"}
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-unsafe-detection')
        preds = get_prediction_labels(analysis)
        assert 'Revealing Clothes' in preds
        assert analysis['count'] == 2

        timeline = tl_patch.call_args_list[0][0][1]
        jtl = timeline.for_json()
        assert jtl['tracks'][0]['name'] == 'Revealing Clothes'
        assert int(jtl['tracks'][0]['clips'][0]['score']) == 83

    @patch(general_patch_path, side_effect=mock_clients)
    @patch(s3_patch_path, side_effect=MockS3Client)
    @patch(rek_patch_path, side_effect=MockRekClient)
    @patch("zmlp_analysis.aws.video.save_timeline", return_value={})
    @patch('zmlp_analysis.aws.video.AwsCloudResources', autospec=True)
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.proxy.get_video_proxy')
    def test_process_celebrity_detection(self, get_prx_patch, store_patch,
                                         store_blob_patch, aws_patch, tl_patch, _, __, ___):
        video_path = zorroa_test_path(VID_MP4)
        get_prx_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()
        aws_patch.return_value = MockAwsCloudResources()

        processor = self.init_processor(video.RekognitionCelebrityDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(asset)
        processor.jobs = {asset.id: "12456"}
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-celebrity-detection')
        preds = get_prediction_labels(analysis)
        assert 'Boris Johnson' in preds
        assert analysis['count'] == 1

        timeline = tl_patch.call_args_list[0][0][1]
        jtl = timeline.for_json()
        assert jtl['tracks'][0]['name'] == 'Boris Johnson'
        assert int(jtl['tracks'][0]['clips'][0]['score']) == 100
