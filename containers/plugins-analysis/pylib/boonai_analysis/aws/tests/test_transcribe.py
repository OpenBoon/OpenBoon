import logging
import os
import pickle
from unittest.mock import patch

from boonai_analysis.aws.transcribe import AmazonTranscribeProcessor
from boonflow import Frame, file_storage
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path

logging.basicConfig()


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

    def upload_file(self, *args, **kwargs):
        return self

    def delete_object(self, **kwargs):
        return self


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


class AmazonTranscribeProcessorTestCase(PluginUnitTestCase):

    @patch('boonai_analysis.aws.util.AwsEnv.transcribe',
           side_effect=MockTranscribeClient)
    @patch('boonai_analysis.aws.util.AwsEnv.s3',
           side_effect=MockS3Client)
    def setUp(self, client_patch, s3_patch):
        os.environ['BOONAI_PROJECT_ID'] = '00000000-0000-0000-0000-000000000001'
        os.environ['BOONAI_AWS_BUCKET'] = 'boonai-unit-tests'

        self.processor = self.init_processor(AmazonTranscribeProcessor(), {'language': 'en-US'})
        self.test_video = 'gs://boonai-dev-data/video/ted_talk.mov'
        self.asset = TestAsset(self.test_video)
        self.asset.set_attr('media.length', 15.0)

    @patch("boonai_analysis.aws.transcribe.save_transcribe_timeline", return_value={})
    @patch("boonai_analysis.aws.transcribe.save_raw_transcribe_result", return_value={})
    @patch("boonai_analysis.aws.transcribe.save_transcribe_webvtt", return_value={})
    @patch('boonai_analysis.aws.transcribe.get_audio_proxy')
    @patch.object(file_storage, 'localize_file')
    @patch('boonai_analysis.aws.transcribe.AmazonTranscribeProcessor.recognize_speech')
    def test_run_process(self, speech_patch, localize_patch, audio_prx_patch, _, __, ___):
        speech_patch.return_value = "foo", load_results(name="transcribe.pk")
        audio_prx_patch.return_value = 1
        localize_patch.return_value = test_path("video/ted_talk.mp4")

        frame = Frame(self.asset)
        self.processor.process(frame)
        assert 'poop' in self.asset.get_attr('analysis.aws-transcribe.content')

    @patch('boonai_analysis.aws.transcribe.get_audio_proxy')
    def test_speech_detection_no_prx(self, audio_prx_patch):
        audio_prx_patch.return_value = 0

        asset = TestAsset('gs://boonai-dev-data/video/no_audio.mp4')
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        self.processor.process(frame)

        assert not self.asset.get_attr('analysis.aws-transcribe.content')

    @patch.object(file_storage, 'localize_file')
    @patch('boonai_analysis.aws.transcribe.get_video_proxy')
    @patch('boonai_analysis.aws.transcribe.get_audio_proxy')
    def test_speech_detection_video_has_no_auido(self, audio_prx_patch,
                                                 video_prx_patch, localize_patch):
        audio_prx_patch.return_value = 0
        video_prx_patch.return_value = 1
        localize_patch.return_value = test_path("video/no_audio.mp4")

        asset = TestAsset('gs://boonai-dev-data/video/no_audio.mp4')
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)
        self.processor.process(frame)

        assert not self.asset.get_attr('analysis.aws-transcribe.content')
