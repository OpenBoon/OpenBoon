import logging
import os
import csv
from unittest.mock import patch

import pytest

from zmlp_analysis.aws.videos.video import VideoDetect
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_prediction_labels, \
    zorroa_test_path, get_mock_stored_file

VID_MP4 = "video/ted_talk.mp4"

logging.basicConfig()


@pytest.mark.skip(reason='dont run automatically')
class AmazonTranscribeProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        self.path = zorroa_test_path('fallback/ted_talk.mp4')
        self.asset = TestAsset(self.path)
        self.asset.set_attr('media.length', 15.0)

        with open("aws_credentials.csv", 'r') as f:
            next(f)
            reader = csv.reader(f)
            for line in reader:
                access_key_id = line[2]
                secret_access_key = line[3]

        os.environ['ZORROA_AWS_KEY'] = access_key_id
        os.environ['ZORROA_AWS_SECRET'] = secret_access_key
        os.environ['ZORROA_AWS_BUCKET'] = 'rgz-test'  # 'zorroa-integration-tests'
        os.environ['ZORROA_AWS_REGION'] = 'us-east-2'
        os.environ['ZMLP_PROJECT_ID'] = '00000000-0000-0000-0000-000000000001'

    def tearDown(self):
        del os.environ['ZORROA_AWS_KEY']
        del os.environ['ZORROA_AWS_SECRET']
        del os.environ['ZORROA_AWS_BUCKET']
        del os.environ['ZORROA_AWS_REGION']
        del os.environ['ZMLP_PROJECT_ID']

    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.aws.videos.video.get_audio_proxy')
    def test_process_audio_proxy(self, get_prx_patch, store_patch, store_blob_patch):
        video_path = zorroa_test_path(VID_MP4)
        namespace = 'analysis.aws-video-label-detection'

        get_prx_patch.return_value = zorroa_test_path(VID_MP4)
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(VideoDetect())
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(self.asset)
        processor.process(frame)

        analysis = asset.get_attr(namespace)
        assert 'poop' in self.asset.get_attr('analysis.aws-transcribe.content')

    # @patch("zmlp_analysis.aws.transcribe.save_transcribe_timeline", return_value={})
    # @patch("zmlp_analysis.aws.transcribe.save_raw_transcribe_result", return_value={})
    # @patch("zmlp_analysis.aws.transcribe.save_transcribe_webvtt", return_value={})
    # @patch('zmlp_analysis.aws.transcribe.get_video_proxy')
    # @patch('zmlp_analysis.aws.transcribe.get_audio_proxy')
    # def test_process_video_proxy(self, get_prx_patch, get_vid_patch, _, __, ___):
    #     get_prx_patch.return_value = 0
    #     get_vid_patch.return_value = zorroa_test_path("video/ted_talk.mp4")
    #
    #     processor = self.init_processor(AmazonTranscribeProcessor(), {'language': 'en-US'})
    #     frame = Frame(self.asset)
    #     processor.process(frame)
    #
    #     assert 'poop' in self.asset.get_attr('analysis.aws-transcribe.content')
