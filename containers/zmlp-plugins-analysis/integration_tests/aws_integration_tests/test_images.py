import logging
import os
from unittest.mock import patch

import pytest

from zmlp_analysis.aws import RekognitionCelebrityDetection
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels

CREDS = os.path.join(os.path.dirname(__file__)) + '/gcp-creds.json'

logging.basicConfig()


@pytest.mark.skip(reason='dont run automatically')
class AsyncSpeechToTextProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        os.environ["ZORROA_AWS_KEY"] = "AKIAXAKJCGYIACN6NXPF"
        os.environ["ZORROA_AWS_SECRET"] = "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"

    def tearDown(self):
        del os.environ['ZORROA_AWS_KEY']
        del os.environ['ZORROA_AWS_SECRET']

    @patch('zmlp_analysis.aws.celebs.get_proxy_level_path')
    def test_celebrity_recognition(self, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        proxy_patch.return_value = path

        processor = self.init_processor(RekognitionCelebrityDetection(), {})
        frame = Frame(TestAsset(path))
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-celebrity-detection')
        assert 'Ryan Gosling' in get_prediction_labels(analysis)
