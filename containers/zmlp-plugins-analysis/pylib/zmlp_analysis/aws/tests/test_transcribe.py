import logging
import os
import pytest
from unittest.mock import patch
import pickle

from zmlp import StoredFile
from zmlp_analysis.aws.transcribe import AmazonTranscribeProcessor
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, get_mock_stored_file, zorroa_test_path

logging.basicConfig(level=logging.INFO)


def load_results(name):
    with open(os.path.dirname(__file__) + "/mock-data/{}".format(name), 'rb') as fp:
        rsp = pickle.load(fp)
    return rsp


def mocked_requests_get(*args, **kwargs):
    """ This method will be used by the mock to replace requests.get """

    class MockResponse:
        def __init__(self, json_data, status_code):
            self.json_data = json_data
            self.status_code = status_code

        def json(self):
            return load_results(name="mocked_requests_get.pk")

    if args[0] == 'http://someurl.com/test.json':
        return MockResponse({"key1": "value1"}, 200)
    elif args[0] == 'http://someotherurl.com/anothertest.json':
        return MockResponse({"key2": "value2"}, 200)

    return MockResponse(None, 404)


class MockTranscribeClient:
    def __init__(self, *args, **kwargs):
        self.meta = MockMeta()

    def start_transcription_job(self, **kwargs):
        return load_results(name="job_response.pk")

    def get_transcription_job(self, **kwargs):
        return load_results(name="transcribe_job.pk")

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
        self.test_video = 'gs://zorroa-dev-data/video/ted_talk.mov'
        self.asset = TestAsset(self.test_video)
        self.asset.set_attr('media.length', 15.0)

    @patch("zmlp_analysis.aws.transcribe.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    @patch.object(file_storage, 'localize_file')
    @patch('zmlp_analysis.aws.transcribe.boto3.client')
    @patch('zmlp_analysis.aws.transcribe.AmazonTranscribeProcessor.recognize_speech')
    def run_process(self, speech_patch, client_patch, localize_patch, native_url_patch,
                    store_patch, store_blob_patch, _):
        speech_patch.return_value = load_results(name="transcribe.pk")
        client_patch.return_value = MockTranscribeClient()
        localize_patch.return_value = zorroa_test_path("video/ted_talk.mov")
        native_url_patch.return_value = 'gs://zorroa-dev-data/video/audio8D0_VU.flac'
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        frame = Frame(self.asset)
        self.processor.process(frame)
        assert 'poop' in self.asset.get_attr('analysis.aws-transcribe.content')

    def test_speech_detection(self):
        self.run_process()

    def test_speech_detection_existing_proxy(self):
        self.asset.add_file(StoredFile({
            'id': 'assets/12345/audio/audio_proxy.flac',
            'category': 'audio',
            'name': 'audio_proxy.flac'
        }))
        self.run_process()

    @patch("zmlp_analysis.aws.transcribe.save_timeline", return_value={})
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    @patch.object(file_storage, 'localize_file')
    @patch('zmlp_analysis.aws.transcribe.boto3.client')
    @patch('zmlp_analysis.aws.transcribe.TranscribeCompleteWaiter')
    @patch('zmlp_analysis.aws.transcribe.requests.get', side_effect=mocked_requests_get)
    def test_speech_detection_no_audio(self, _, waiter_patch, speech_patch, localize_patch,
                                       native_url_patch, store_patch, store_blob_patch, st_patch):
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
