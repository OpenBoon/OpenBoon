import logging
from unittest import TestCase

from boonai_analysis.utils.prechecks import Prechecks
from boonflow.testing import TestAsset

logging.basicConfig(level=logging.DEBUG)


class TestPreChecks(TestCase):

    def test_is_valid_video_length(self):
        asset = TestAsset('gs://boonai-dev-data/video/no_audio.mp4')
        asset.set_attr('media.length', 15.0)
        assert Prechecks.is_valid_video_length(asset)

    def test_is_valid_video_length_fail(self):
        asset = TestAsset('gs://boonai-dev-data/video/no_audio.mp4')
        asset.set_attr('media.length', Prechecks.max_video_length + 1)
        assert not Prechecks.is_valid_video_length(asset)

    def test_is_valid_video_length_missing_attr(self):
        asset = TestAsset('gs://boonai-dev-data/video/no_audio.mp4')
        assert not Prechecks.is_valid_video_length(asset)
