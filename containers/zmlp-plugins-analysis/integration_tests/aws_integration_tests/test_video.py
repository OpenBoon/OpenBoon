import logging
import os
import csv
from unittest.mock import patch

import pytest

from zmlp_analysis.aws import video
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, \
    get_mock_stored_file, get_prediction_labels

VID_MP4 = "video/credits.mov"
MUSTANG = "video/mustang.mp4"
TED_TALK = "video/ted_talk.mp4"
MODEL = "video/model.mp4"
BORIS_JOHNSON = "video/boris-johnson.mp4"
MEDIA_LENGTH = 101.0

logging.basicConfig()


@pytest.mark.skip(reason='dont run automatically')
class AmazonVideoProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        self.path = zorroa_test_path('fallback/ted_talk.mp4')
        self.asset = TestAsset(self.path)
        self.asset.set_attr('media.length', MEDIA_LENGTH)

        with open("aws_credentials.csv", 'r') as f:
            next(f)
            reader = csv.reader(f)
            for line in reader:
                access_key_id = line[2]
                secret_access_key = line[3]

        with open('rekog_role_arn', 'r') as f:
            os.environ['ZORROA_AWS_ML_USER_ROLE_ARN'] = f.read()

        with open('rekog_topic_sub_queue', 'r') as f:
            topic_arn, sqs_arn, sqs_url = f.readlines()

        os.environ['ZORROA_AWS_KEY'] = access_key_id
        os.environ['ZORROA_AWS_SECRET'] = secret_access_key
        os.environ['ZORROA_AWS_BUCKET'] = 'rgz-test'  # 'zorroa-integration-tests'
        os.environ['ZORROA_AWS_REGION'] = 'us-east-2'
        os.environ['ZMLP_PROJECT_ID'] = '00000000-0000-0000-0000-000000000001'
        os.environ['ZORROA_AWS_ML_USER_SQS_ARN'] = sqs_arn.strip()
        os.environ['ZORROA_AWS_ML_USER_SQS_URL'] = sqs_url.strip()
        os.environ['ZORROA_AWS_ML_USER_SNS_TOPIC_ARN'] = topic_arn.strip()

    def tearDown(self):
        del os.environ['ZORROA_AWS_KEY']
        del os.environ['ZORROA_AWS_SECRET']
        del os.environ['ZORROA_AWS_BUCKET']
        del os.environ['ZORROA_AWS_REGION']
        del os.environ['ZMLP_PROJECT_ID']

    @patch("zmlp_analysis.aws.video.all.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.all.proxy.get_video_proxy')
    def test_process_label_detection(self, get_prx_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(VID_MP4)
        get_prx_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.RekognitionLabelDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(self.asset)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-label-detection')
        preds = get_prediction_labels(analysis)
        assert 'Word' in preds
        assert analysis['count'] > 10

    @patch("zmlp_analysis.aws.video.all.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.all.proxy.get_video_proxy')
    def test_process_text_detection(self, get_prx_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(TED_TALK)
        get_prx_patch.return_value = zorroa_test_path(TED_TALK)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.RekognitionTextDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(self.asset)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-text-detection')
        preds = get_prediction_labels(analysis)
        assert 'have' in preds
        assert analysis['count'] >= 20

    @patch("zmlp_analysis.aws.video.all.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.all.proxy.get_video_proxy')
    def test_process_face_detection(self, get_prx_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(TED_TALK)
        get_prx_patch.return_value = zorroa_test_path(TED_TALK)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.RekognitionFaceDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(self.asset)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-face-detection')
        preds = get_prediction_labels(analysis)
        assert 'face70' in preds
        assert analysis['count'] >= 30

    @patch("zmlp_analysis.aws.video.all.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.all.proxy.get_video_proxy')
    def test_process_unsafe_detection(self, get_prx_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(MODEL)
        get_prx_patch.return_value = zorroa_test_path(MODEL)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.RekognitionUnsafeDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(self.asset)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-unsafe-detection')
        preds = get_prediction_labels(analysis)
        assert 'Suggestive' in preds
        assert analysis['count'] == 3

    @patch("zmlp_analysis.aws.video.all.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.all.proxy.get_video_proxy')
    def test_process_celebrity_detection(self, get_prx_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(BORIS_JOHNSON)
        get_prx_patch.return_value = zorroa_test_path(BORIS_JOHNSON)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.RekognitionCelebrityDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(self.asset)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-celebrity-detection')
        preds = get_prediction_labels(analysis)
        assert 'Boris Johnson' in preds
        assert analysis['count'] == 2

    @patch("zmlp_analysis.aws.video.all.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.all.proxy.get_video_proxy')
    def test_process_person_tracking(self, get_prx_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(BORIS_JOHNSON)
        get_prx_patch.return_value = zorroa_test_path(BORIS_JOHNSON)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.RekognitionPeoplePathingDetection())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(self.asset)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-person-tracking-detection')
        preds = get_prediction_labels(analysis)
        assert 'person0' in preds
        assert analysis['count'] >= 30

    @patch("zmlp_analysis.aws.video.all.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.all.proxy.get_video_proxy')
    def test_process_black_frame_detection(self, get_prx_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(MUSTANG)
        get_prx_patch.return_value = zorroa_test_path(MUSTANG)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.BlackFramesVideoDetectProcessor())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(self.asset)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-black-frames-detection')
        preds = get_prediction_labels(analysis)
        assert 'BlackFrames' in preds
        assert analysis['count'] == 1

    @patch("zmlp_analysis.aws.video.all.video.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.video.all.proxy.get_video_proxy')
    def test_process_end_credits_detection(self, get_prx_patch, store_patch, store_blob_patch, _):
        video_path = zorroa_test_path(VID_MP4)
        get_prx_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(video.EndCreditsVideoDetectProcessor())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', MEDIA_LENGTH)
        frame = Frame(self.asset)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-credits-detection')
        preds = get_prediction_labels(analysis)
        assert 'EndCredits' in preds
        assert analysis['count'] == 1
