import os
from unittest.mock import patch

from google.cloud.videointelligence_v1.proto import video_intelligence_pb2

from zmlp_analysis.google import cloud_timeline
from zmlpsdk.testing import PluginUnitTestCase, TestAsset


class TestCloudTimelineBuilder(PluginUnitTestCase):

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
        assert 0.266 == timeline.tracks['people']['clips'][0]['score']

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
        rsp = video_intelligence_pb2.AnnotateVideoResponse()
        with open(os.path.dirname(__file__) + '/mock-data/{}'.format(name), 'rb') as fp:
            rsp.ParseFromString(fp.read())
        return rsp.annotation_results[0]
