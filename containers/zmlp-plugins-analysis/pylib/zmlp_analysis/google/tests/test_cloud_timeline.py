import os
from unittest.mock import patch

import google.cloud.videointelligence as videointelligence

from zmlp_analysis.google import cloud_timeline
from zmlpsdk.testing import PluginUnitTestCase, TestAsset
from zmlpsdk import file_storage

from .test_cloud_speech import load_results as speech_load_results


class TestCloudTimelineBuilder(PluginUnitTestCase):

    @patch.object(file_storage.assets, 'store_file')
    def test_save_video_speech_transcription_webvtt(self, patch):
        patch.return_value = None

        annots = self.load_results('detect-speech.dat')
        path, sf = cloud_timeline.save_video_speech_transcription_webvtt(
            TestAsset(id="123"), annots)

        with open(path, "r") as fp:
            data = fp.read()

        assert 'We have main engine start or three two one.' in data
        assert '00:00:04.700 --> 00:00:09.800' in data

    @patch.object(file_storage.assets, 'store_file')
    def test_save_save_speech_to_text_webvtt(self, patch):
        patch.return_value = None

        annots = speech_load_results()
        path, sf = cloud_timeline.save_speech_to_text_webvtt(
            TestAsset(id="123"), annots)

        with open(path, "r") as fp:
            data = fp.read()

        assert "toilets and poop" in data
        assert "00:00:04.400 --> 00:00:06.200" in data

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    def test_save_speech_speech_to_text_timeline(self, _):
        annots = speech_load_results()
        timeline = cloud_timeline.save_speech_to_text_timeline(TestAsset(id="123"), annots)

        assert 'gcp-speech-to-text' == timeline.name
        assert 1 == len(timeline.tracks)

        clips = timeline.tracks['Language en-us']['clips']
        clips = sorted(clips, key=lambda i: i['start'])

        assert ['sanitation does mold killing Sanitation'] == clips[0]["content"]
        assert ['toilets and poop'] == clips[1]["content"]
        assert ['and I have yet to emerge'] == clips[2]["content"]

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    def test_save_speech_transcription_timeline(self, _):
        annots = self.load_results('detect-speech.dat')
        timeline = cloud_timeline.save_speech_transcription_timeline(TestAsset(id="123"), annots)
        assert 'gcp-video-speech-transcription' == timeline.name
        assert 1 == len(timeline.tracks)

        clips = timeline.tracks['Speech Transcription']['clips']
        clips = sorted(clips, key=lambda i: i['start'])

        assert ['We have main engine start or three two one.'] == clips[0]["content"]
        assert ['robots memory synced and locked'] == clips[3]["content"]
        assert ['this is pretty freaky.'] == clips[4]["content"]

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    def test_save_text_timeline(self, _):
        annots = self.load_results('detect-text.dat')
        timeline = cloud_timeline.save_text_detection_timeline(TestAsset(id="123"), annots)
        assert 'gcp-video-text-detection' == timeline.name
        assert 1 == len(timeline.tracks)

        clips = timeline.tracks['Detected Text']['clips']
        clips = sorted(clips, key=lambda i: i['start'])

        assert ['into the world of sanitation'] == clips[0]["content"]
        assert ['there\'s more coming -- (Laughter)-'] == clips[1]["content"]
        assert ['sanitation, toilets and poop,'] == clips[2]["content"]
        assert ['and I have yet to emerge.'] == clips[3]["content"]

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    def test_save_label_timeline(self, _):
        annots = self.load_results('detect-labels.dat')
        timeline = cloud_timeline.save_label_detection_timeline(TestAsset(id="123"), annots)
        assert 'gcp-video-label-detection' == timeline.name
        assert 4.416666 == timeline.tracks['people']['clips'][0]['stop']
        assert 0 == timeline.tracks['people']['clips'][0]['start']
        assert 0.266 == round(timeline.tracks['people']['clips'][0]['score'], 3)

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    def test_save_logo_detection_timeline(self, _):
        annots = self.load_results('detect-logos.dat')
        timeline = cloud_timeline.save_logo_detection_timeline(TestAsset(id="123"), annots)
        assert 'gcp-video-logo-detection' == timeline.name
        assert 12.387375 == timeline.tracks['Mustang']['clips'][0]['stop']

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    def test_save_object_detection_timeline(self, _):
        patch.retval = {}
        annots = self.load_results('detect-objects.dat')
        timeline = cloud_timeline.save_object_detection_timeline(TestAsset(id="123"), annots)
        assert 'gcp-video-object-detection' == timeline.name
        assert 1.92 == timeline.tracks['footwear']['clips'][0]['stop']

    @patch("zmlp_analysis.google.cloud_timeline.save_timeline", return_value={})
    def test_save_explicit_timeline(self, _):

        annots = self.load_results('detect-explicit.dat')
        timeline = cloud_timeline.save_content_moderation_timeline(TestAsset(id="123"), annots)

        expect = [
            ['Very Unlikely'],
            ['Very Unlikely'],
            ['Very Unlikely'],
            ['Unlikely'],
            ['Unlikely']
        ]
        idx = 0
        for track in timeline.tracks.values():
            for clip in track['clips']:
                assert expect[idx] == clip['content']
                idx += 1

    def load_results(self, name):
        rsp = videointelligence.AnnotateVideoResponse()
        with open(os.path.dirname(__file__) + '/mock-data/{}'.format(name), 'rb') as fp:
            rsp._pb.ParseFromString(fp.read())
        return rsp.annotation_results[0]
