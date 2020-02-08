import os
from unittest.mock import patch

from zmlp_analysis.google import CloudVideoIntelligenceProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset


# NOTE: These test require you have a service account key located at
# ~/zorroa/keys/gcloud-integration-test.json.
class CloudVideoIntelligenceProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = os.path.dirname(__file__) + '/gcp-creds.json'

    def tearDown(self):
        del os.environ["GOOGLE_APPLICATION_CREDENTIALS"]

    @patch('zmlp_analysis.google.cloud_video.get_proxy_level')
    def test_video_labels(self, proxy_patch):
        processor = self.init_processor(
            CloudVideoIntelligenceProcessor())
        path = zorroa_test_path('video/sample_ipad.m4v')
        proxy_patch.return_value = path
        clip = TestAsset(path)
        clip.set_attr("clip", {
            "type": "scene",
            "start": 0,
            "stop": 5,
            "length": 5
        })
        clip.set_attr('media.duration', 15.0)
        frame = Frame(clip)
        processor.process(frame)
        assert 'winter' in clip.get_attr('analysis.google.videoLabel.shot.keywords')
        assert 'winter' in clip.get_attr('analysis.google.videoLabel.segment.keywords')

    @patch('zmlp_analysis.google.cloud_video.get_proxy_level')
    def test_video_text(self, proxy_patch):
        processor = self.init_processor(
            CloudVideoIntelligenceProcessor())
        path = zorroa_test_path('video/credits.mov')
        proxy_patch.return_value = path
        asset = TestAsset(path)
        asset.set_attr("clip", {
            "type": "scene",
            "start": 0,
            "stop": 3,
            "length": 3,
        })
        asset.set_attr('media.duration', 4.0)
        frame = Frame(asset)
        processor.process(frame)
        assert 'francois duhamel' in asset.get_attr('analysis.google.videoText.content').lower()
