import os
import pytest

from unittest.mock import patch

from zmlp_analysis.google import CloudSpeechToTextProcessor
from zmlpsdk.testing import TestAsset, PluginUnitTestCase, zorroa_test_data
from zmlpsdk import Frame


@pytest.mark.skip(reason='dont run automaticallly')
class CloudSpeechToTextProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = os.path.dirname(__file__) + '/gcp-creds.json'
        self.processor = self.init_processor(
            CloudSpeechToTextProcessor(), {
             'alternate_languages': ['en-US'],
             'primary_language': 'es-US'})

    def tearDown(self):
        del os.environ["GOOGLE_APPLICATION_CREDENTIALS"]

    @patch('zmlp_analysis.google.cloud_speech.get_proxy_level_path')
    def test_speech_detection(self, proxy_patch):
        path = zorroa_test_data("video/ted_talk.mov")
        proxy_patch.return_value = path
        clip = TestAsset(path)

        clip.set_attr("clip.type", "scene")
        clip.set_attr("clip.start", 0.0)
        clip.set_attr("clip.stop", 9.0)
        clip.set_attr("clip.length", 9.0)
        clip.set_attr("clip.timeline", "shot")
        clip.set_attr('media.duration', 9.0)
        frame = Frame(clip)
        self.processor.process(frame)
        assert 'poop' in clip.get_attr('analysis.google.speechRecognition.content')
        assert clip.get_attr('analysis.google.speechRecognition.language') == 'en-us'
        assert clip.get_attr('analysis.google.speechRecognition.confidence') > .9

    @patch('zmlp_analysis.google.cloud_speech.get_proxy_level_path')
    def test_no_sound_video(self, proxy_patch):
        path = zorroa_test_data('video/FatManOnABike1914.mp4', False)
        proxy_patch.return_value = path
        video = TestAsset(path, id="foo")
        video.set_attr("clip.type", "scene")
        video.set_attr("clip.start", 0.0)
        video.set_attr("clip.stop", 30.0)
        video.set_attr("clip.length", 30.0)
        video.set_attr("clip.timeline", "shot")

        frame = Frame(video)
        frame.asset.set_attr('media.duration', 144.0)
        self.processor.process(frame)
        assert frame.asset.get_attr('google.speechRecognition') is None
