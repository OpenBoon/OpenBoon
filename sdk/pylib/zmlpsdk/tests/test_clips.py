import logging
from unittest import TestCase


from zmlpsdk.clips import ClipTracker
from zmlpsdk.testing import TestAsset

logging.basicConfig(level=logging.DEBUG)


class TestClipTracker(TestCase):

    def test_clip_tracker_append(self):
        c = ClipTracker(TestAsset(), "pets")
        c.append(1, ["dog", "cat", "cow"])
        c.append(3, ["dog", "cat", "cow"])
        c.append(4, ["dog", "cow"])

        tl = c.timeline
        assert 1 == len(tl.tracks['cat']['clips'])

    def test_build_timeline(self):
        c = ClipTracker(TestAsset(), "pets")
        c.append(1, ["dog", "cat", "cow"])
        c.append(3, ["dog", "cat", "cow"])
        c.append(4, ["dog", "cow"])

        tl = c.build_timeline(5)
        assert 1 == len(tl.tracks['dog']['clips'])
        assert 1 == len(tl.tracks['cow']['clips'])
        assert 1 == tl.tracks['cow']['clips'][0]['start']
        assert 5 == tl.tracks['cow']['clips'][0]['stop']
