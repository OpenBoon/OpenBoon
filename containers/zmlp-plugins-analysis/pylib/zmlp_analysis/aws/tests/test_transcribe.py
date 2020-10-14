import logging
import os
from unittest.mock import patch
import pickle

from zmlp import StoredFile
from zmlp_analysis.aws.transcribe import AmazonTranscribeProcessor
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_mock_stored_file, zorroa_test_path

logging.basicConfig(level=logging.INFO)


def load_results():
    name = "transcribe.pk"
    with open(os.path.dirname(__file__) + "/mock-data/{}".format(name), 'rb') as fp:
        rsp = pickle.load(fp)
    return rsp


class MockTranscribeClient:

    def __init__(self, *args, **kwargs):
        self.meta = MockMeta()

    def start_transcription_job(self, **kwargs):
        name = "job_response.pk"
        with open(os.path.dirname(__file__) + "/mock-data/{}".format(name), 'rb') as fp:
            rsp = pickle.load(fp)
        return rsp

    def get_transcription_job(self, **kwargs):
        name = "transcribe_job.pk"
        with open(os.path.dirname(__file__) + "/mock-data/{}".format(name), 'rb') as fp:
            rsp = pickle.load(fp)
        return rsp

    def delete_transcription_job(self, **kwargs):
        return self


class MockS3Client:

    def __init__(self, *args, **kwargs):
        self.objects = MockS3Object()

    def create_bucket(self, **kwargs):
        return self

    def delete(self, **kwargs):
        return self


@pytest.mark.skip(reason='dont run automatically')
class AmazonTranscribeProcessorTestCase(PluginUnitTestCase):

    @patch('zmlp_analysis.aws.transcribe.boto3.client',
           side_effect=MockTranscribeClient)
    def setUp(self, client_patch):
        self.processor = self.init_processor(AmazonTranscribeProcessor(), {'language': 'en-US'})

    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    @patch.object(file_storage, 'localize_file')
    @patch('zmlp_analysis.aws.transcribe.boto3.client')
    @patch('zmlp_analysis.aws.transcribe.AmazonTranscribeProcessor.recognize_speech')
    def test_speech_detection(self, speech_patch, client_patch, localize_patch,
                              native_url_patch, store_patch, store_blob_patch):
        speech_patch.return_value = load_results()
        client_patch.return_value = MockTranscribeClient()
        localize_patch.return_value = zorroa_test_path("video/ted_talk.mov")
        native_url_patch.return_value = 'gs://zorroa-dev-data/video/audio8D0_VU.flac'
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        asset = TestAsset('gs://zorroa-dev-data/video/ted_talk.mov')
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        self.processor.process(frame)
        assert 'poop' in asset.get_attr('analysis.aws-transcribe.content')

    @patch.object(file_storage.cache, 'localize_uri')
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    @patch('zmlp_analysis.aws.transcribe.boto3.client')
    @patch('zmlp_analysis.aws.transcribe.AmazonTranscribeProcessor.recognize_speech')
    def test_speech_detection_existing_proxy(self,
                                             speech_patch,
                                             client_patch,
                                             native_url_patch,
                                             store_patch,
                                             store_blob_patch,
                                             local_patch):
        speech_patch.return_value = load_results()
        local_patch.return_value = zorroa_test_path('audio/audio1.flac')
        client_patch.return_value = MockTranscribeClient()
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
        assert 'poop' in asset.get_attr('analysis.aws-transcribe.content')

    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    @patch.object(file_storage, 'localize_file')
    @patch('zmlp_analysis.aws.transcribe.boto3.client')
    @patch('zmlp_analysis.aws.transcribe.TranscribeCompleteWaiter')
    def test_speech_detection_no_audio(self, waiter_patch, speech_patch, localize_patch,
                                       native_url_patch, store_patch, store_blob_patch):
        waiter_patch.return_value = MockTranscribeCompleteWaiter()
        speech_patch.return_value = MockTranscribeClient()
        localize_patch.return_value = zorroa_test_path("video/no_audio.mp4")
        native_url_patch.return_value = 'gs://zorroa-dev-data/video/audio8D0_VU.flac'
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        asset = TestAsset('gs://zorroa-dev-data/video/no_audio.mp4')
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        self.processor.process(frame)


class MockMeta:

    def __init__(self, *args, **kwargs):
        self.region_name = 'us-east-2'


class MockS3Object:

    def __init__(self, *args, **kwargs):
        pass

    def delete(self, **kwargs):
        return self


class MockTranscribeCompleteWaiter:

    def __init__(self, *args, **kwargs):
        pass

    def wait(self, job_name=None, **kwargs):
        return self
