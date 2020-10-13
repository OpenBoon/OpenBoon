import logging
import os
from unittest.mock import patch

from zmlp import StoredFile
from zmlp_analysis.google.cloud_speech import AsyncSpeechToTextProcessor
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_mock_stored_file, zorroa_test_path

from google.cloud import speech_v1p1beta1 as speech

logging.basicConfig(level=logging.INFO)


def load_results():
    name = "speech-to-text.dat"
    rsp = speech.types.LongRunningRecognizeResponse()
    with open(os.path.dirname(__file__) + "/mock-data/{}".format(name), 'rb') as fp:
        rsp.ParseFromString(fp.read())
    return rsp


class MockSpeechToTextClient(object):

    def __init__(self, *args, **kwargs):
        pass

    def long_running_recognize(self, **kwargs):
        return self


class AsyncSpeechToTextProcessorTestCase(PluginUnitTestCase):

    @patch('zmlp_analysis.google.cloud_speech.initialize_gcp_client',
           side_effect=MockSpeechToTextClient)
    def setUp(self, client_patch):
        self.processor = self.init_processor(
            AsyncSpeechToTextProcessor(), {'language': 'en-US'})

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    @patch.object(file_storage, 'localize_file')
    @patch('zmlp_analysis.google.cloud_speech.initialize_gcp_client')
    @patch('zmlp_analysis.google.cloud_speech.AsyncSpeechToTextProcessor.recognize_speech')
    def test_speech_detection(self, speech_patch, client_patch, localize_patch,
                              native_url_patch, store_patch, store_blob_patch, _):
        speech_patch.return_value = load_results()
        client_patch.return_value = MockSpeechToTextClient()
        localize_patch.return_value = zorroa_test_path("video/ted_talk.mov")
        native_url_patch.return_value = 'gs://zorroa-dev-data/video/audio8D0_VU.flac'
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        asset = TestAsset('gs://zorroa-dev-data/video/ted_talk.mov')
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        self.processor.process(frame)
        assert 'poop' in asset.get_attr('analysis.gcp-speech-to-text.content')

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    @patch.object(file_storage.cache, 'localize_uri')
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    @patch('zmlp_analysis.google.cloud_speech.initialize_gcp_client')
    @patch('zmlp_analysis.google.cloud_speech.AsyncSpeechToTextProcessor.recognize_speech')
    def test_speech_detection_existing_proxy(self,
                                             speech_patch,
                                             client_patch,
                                             native_url_patch,
                                             store_patch,
                                             store_blob_patch,
                                             local_patch, _):
        speech_patch.return_value = load_results()
        local_patch.return_value = zorroa_test_path('audio/audio1.flac')
        client_patch.return_value = MockSpeechToTextClient()
        native_url_patch.return_value = 'gs://zorroa-dev-data/video/audio8D0_VU.flac'
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        asset = TestAsset('gs://zorroa-dev-data/video/ted_talk.mov')
        asset.set_attr('media.length', 15.0)
        asset.add_file(StoredFile({
            'id': 'assets/12345/audio/audio_proxy.flac',
            'category': 'audio', 'name': 'audio_proxy.flac'
        }))

        frame = Frame(asset)
        self.processor.process(frame)
        assert 'poop' in asset.get_attr('analysis.gcp-speech-to-text.content')

    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    @patch.object(file_storage, 'localize_file')
    @patch('zmlp_analysis.google.cloud_speech.initialize_gcp_client')
    def test_speech_detection_no_audio(self, speech_patch, localize_patch,
                                       native_url_patch, store_patch, store_blob_patch):
        speech_patch.return_value = MockSpeechToTextClient()
        localize_patch.return_value = zorroa_test_path("video/no_audio.mp4")
        native_url_patch.return_value = 'gs://zorroa-dev-data/video/audio8D0_VU.flac'
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        asset = TestAsset('gs://zorroa-dev-data/video/no_audio.mp4')
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        self.processor.process(frame)
