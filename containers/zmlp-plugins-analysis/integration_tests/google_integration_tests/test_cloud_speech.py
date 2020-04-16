import os
import logging
import pytest
from unittest.mock import patch

from zmlp import StoredFile
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_mock_stored_file

from zmlp_analysis.google.cloud_speech import AsyncSpeechToTextProcessor

CREDS = os.path.join(os.path.dirname(__file__)) + '/gcp-creds.json'

logging.basicConfig()


@pytest.mark.skip(reason='dont run automatically')
class AsyncSpeechToTextProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = os.path.dirname(__file__) + '/gcp-creds.json'
        self.processor = self.init_processor(
            AsyncSpeechToTextProcessor(), {'language': 'en-US'})

    def tearDown(self):
        del os.environ['GOOGLE_APPLICATION_CREDENTIALS']

    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_speech_detection(self, native_url_patch, store_patch, store_blob_patch):
        native_url_patch.return_value = 'gs://zorroa-dev-data/video/audio8D0_VU.flac'
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        asset = TestAsset('gs://zorroa-dev-data/video/ted_talk.mov')
        asset.set_attr('media.length', 15.0)
        asset.set_attr('clip.timeline', 'full')
        frame = Frame(asset)
        self.processor.process(frame)
        assert 'poop' in asset.get_attr('analysis.gcp.speech-to-text.content')

    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_speech_detection_existing_proxy(
            self, native_url_patch, store_patch, store_blob_patch):
        native_url_patch.return_value = 'gs://zorroa-dev-data/video/audio8D0_VU.flac'
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        asset = TestAsset('gs://zorroa-dev-data/video/ted_talk.mov')
        asset.set_attr('media.length', 15.0)
        asset.set_attr('clip.timeline', 'full')
        asset.add_file(StoredFile({
            'id': 'assets/12345/audio/audio_proxy.flac',
            'category': 'audio', 'name': 'audio_proxy.flac'
        }))

        frame = Frame(asset)
        self.processor.process(frame)
        assert 'poop' in asset.get_attr('analysis.gcp.speech-to-text.content')
