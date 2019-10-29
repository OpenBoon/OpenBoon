import unittest

from zplugins.video.clipifiers import FFProbeKeyframeClipifier
from zplugins.video.importers import VideoImporter
from zsdk import Asset
from zsdk.testing import zorroa_test_data


class KeyframeClipifierUnitTestCase(unittest.TestCase):
    def setUp(self):
        super(KeyframeClipifierUnitTestCase, self).setUp()
        self.movie_path = zorroa_test_data('video/sample_ipad.m4v')
        processor = VideoImporter()
        self.clipifier = FFProbeKeyframeClipifier(processor, minimum_clip_length=1.0)

    def test_clipify(self):
        asset = Asset(self.movie_path)
        clips = self.clipifier.get_clips(asset)
        expected_clips = [(0.0, 1.001), (1.001, 2.336), (2.336, 10.11), (10.11, 11.378),
                          (11.378, 13.013), (13.013, 15.048)]
        assert clips == expected_clips
