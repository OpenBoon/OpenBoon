import logging
import os
import csv
from unittest.mock import patch

import pytest

from zmlp import StoredFile
from zmlp_analysis.aws import AmazonTranscribeProcessor
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_mock_stored_file

logging.basicConfig()


@pytest.mark.skip(reason='dont run automatically')
class AmazonTranscribeProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        self.path = zorroa_test_path('fallback/ted_talk.mp4')
        self.asset = TestAsset(self.path)
        self.asset.set_attr('media.length', 15.0)

        with open('aws_credentials.csv', 'r') as f:
            next(f)
            reader = csv.reader(f)
            for line in reader:
                access_key_id = line[2]
                secret_access_key = line[3]
        os.environ["ZORROA_AWS_KEY"] = access_key_id
        os.environ["ZORROA_AWS_SECRET"] = secret_access_key

    def tearDown(self):
        del os.environ['ZORROA_AWS_KEY']
        del os.environ['ZORROA_AWS_SECRET']

    @patch("zmlp_analysis.aws.transcribe.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def run_process(self, native_url_patch, store_patch, store_blob_patch, _):
        native_url_patch.return_value = 'gs://zorroa-dev-data/video/audio8D0_VU.flac'
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        processor = self.init_processor(AmazonTranscribeProcessor(), {'language': 'en-US'})
        frame = Frame(self.asset)
        processor.process(frame)

        assert 'poop' in self.asset.get_attr('analysis.aws-transcribe.content')

        return store_patch

    def test_speech_detection(self):
        self.run_process()

    def test_speech_detection_existing_proxy(self):
        self.asset.add_file(StoredFile({
            'id': 'assets/12345/audio/audio_proxy.flac',
            'category': 'audio', 'name': 'audio_proxy.flac'
        }))
        store_patch = self.run_process()

        with open(store_patch.call_args_list[0][0][0]) as fp:
            vtt = fp.read()
        assert "toilet" in vtt
        assert "emerge" in vtt
