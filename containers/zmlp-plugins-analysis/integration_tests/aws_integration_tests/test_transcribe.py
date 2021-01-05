import logging
import os
import csv
from unittest.mock import patch

import pytest

from zmlp_analysis.aws import AmazonTranscribeProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path

logging.basicConfig()


@pytest.mark.skip(reason='dont run automatically')
class AmazonTranscribeProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        super(AmazonTranscribeProcessorTestCase, self).setUp()
        self.path = zorroa_test_path('fallback/ted_talk.mp4')
        self.asset = TestAsset(self.path)
        self.asset.set_attr('media.length', 15.0)

        with open('aws_credentials.csv', 'r') as f:
            next(f)
            reader = csv.reader(f)
            for line in reader:
                access_key_id = line[2]
                secret_access_key = line[3]

        os.environ['ZORROA_AWS_KEY'] = access_key_id
        os.environ['ZORROA_AWS_SECRET'] = secret_access_key
        os.environ['ZORROA_AWS_BUCKET'] = 'zorroa-integration-tests'
        os.environ['ZORROA_AWS_REGION'] = 'us-east-1'
        os.environ['ZMLP_PROJECT_ID'] = '00000000-0000-0000-0000-000000000001'

    def tearDown(self):
        super(AmazonTranscribeProcessorTestCase, self).tearDown()
        del os.environ['ZORROA_AWS_KEY']
        del os.environ['ZORROA_AWS_SECRET']
        del os.environ['ZORROA_AWS_BUCKET']
        del os.environ['ZORROA_AWS_REGION']
        del os.environ['ZMLP_PROJECT_ID']

    @patch("zmlp_analysis.aws.transcribe.save_transcribe_timeline", return_value={})
    @patch("zmlp_analysis.aws.transcribe.save_raw_transcribe_result", return_value={})
    @patch("zmlp_analysis.aws.transcribe.save_transcribe_webvtt", return_value={})
    @patch('zmlp_analysis.aws.transcribe.get_audio_proxy')
    def test_process_audio_proxy(self, get_prx_patch, _, __, ___):
        get_prx_patch.return_value = zorroa_test_path("audio/audio1.flac")
        processor = self.init_processor(AmazonTranscribeProcessor(), {'language': 'en-US'})
        frame = Frame(self.asset)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        assert 'poop' in self.asset.get_attr('analysis.aws-transcribe.content')

    @patch("zmlp_analysis.aws.transcribe.save_transcribe_timeline", return_value={})
    @patch("zmlp_analysis.aws.transcribe.save_raw_transcribe_result", return_value={})
    @patch("zmlp_analysis.aws.transcribe.save_transcribe_webvtt", return_value={})
    @patch('zmlp_analysis.aws.transcribe.get_video_proxy')
    @patch('zmlp_analysis.aws.transcribe.get_audio_proxy')
    def test_process_video_proxy(self, get_prx_patch, get_vid_patch, _, __, ___):
        get_prx_patch.return_value = 0
        get_vid_patch.return_value = zorroa_test_path("video/ted_talk.mp4")

        processor = self.init_processor(AmazonTranscribeProcessor(), {'language': 'en-US'})
        frame = Frame(self.asset)
        processor.process(frame)
        self.mock_record_analysis_metric.assert_called_once()

        assert 'poop' in self.asset.get_attr('analysis.aws-transcribe.content')
