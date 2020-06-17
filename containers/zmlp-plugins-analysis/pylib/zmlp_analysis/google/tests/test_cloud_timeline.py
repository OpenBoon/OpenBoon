import os
import json

from google.cloud.videointelligence_v1.proto import video_intelligence_pb2

from zmlp_analysis.google import cloud_timeline
from zmlpsdk.testing import PluginUnitTestCase
from zmlp import to_json


class TestCloudTimelineBuilder(PluginUnitTestCase):

    def test_build_text_timeline(self):
        annots = self.load_results('detect-text.dat')
        timeline = cloud_timeline.build_text_detection_timeline(annots)
        assert 'gcp-video-text-detection' == timeline.name
        assert 1 == len(timeline.tracks)
        track = timeline.get_track('Detected Text')
        assert len(track.clips) > 0

        clips = track.clips
        assert 'into the world of sanitation' == clips[0].metadata['content']
        assert 'there\'s more coming -- (Laughter)-' == clips[1].metadata['content']
        assert 'sanitation, toilets and poop,' == clips[2].metadata['content']
        assert 'and I have yet to emerge.' == clips[3].metadata['content']

    def test_build_label_timeline(self):
        annots = self.load_results('detect-labels.dat')
        timeline = cloud_timeline.build_label_detection_timeline(annots)
        assert 'gcp-video-label-detection' == timeline.name
        assert 21 == len(timeline.tracks)
        for track in timeline.tracks:
            assert len(track.clips) > 0

    def test_build_explicit_timeline(self):
        annots = self.load_results('detect-explicit.dat')
        timeline = cloud_timeline.build_content_moderation_timeline(annots)

        # Validate sorting
        jtl = json.loads(to_json(timeline))
        assert jtl['tracks'][0]['name'] == 'Unlikely'
        assert jtl['tracks'][1]['name'] == 'Very Unlikely'

    def load_results(self, name):
        rsp = video_intelligence_pb2.AnnotateVideoResponse()
        with open(os.path.dirname(__file__) + '/mock-data/{}'.format(name), 'rb') as fp:
            rsp.ParseFromString(fp.read())
        return rsp.annotation_results[0]
