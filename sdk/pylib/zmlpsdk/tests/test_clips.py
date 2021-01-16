import logging
from unittest import TestCase


from zmlpsdk.clips import ClipTracker
from zmlpsdk.testing import TestAsset

logging.basicConfig(level=logging.DEBUG)


class TestClipTracker(TestCase):

    def test_clip_tracker_append(self):
        c = ClipTracker(TestAsset(), "pets")
        c.append(1, ["dog", "cat", "cow"], [1.0, 0.9, 0.8])
        c.append(3, ["dog", "cat", "cow"], [1.0, 0.9, 0.8])
        c.append(4, ["dog", "cow"], [1.0, 0.9])

        tl = c.timeline
        assert 1 == len(tl.tracks['cat']['clips'])

    def test_build_timeline(self):
        c = ClipTracker(TestAsset(), "pets")
        c.append(1, ["dog", "cat", "cow"], [1.0, 0.86, 0.76])
        c.append(3, ["dog", "cat", "cow"], [1.0, 1.0, 1.0])
        c.append(4, ["dog", "cow"], [1.0, 1.0])

        tl = c.build_timeline(5)
        assert 1 == len(tl.tracks['dog']['clips'])
        assert 1 == len(tl.tracks['cow']['clips'])
        assert 1 == tl.tracks['cow']['clips'][0]['start']
        assert 5 == tl.tracks['cow']['clips'][0]['stop']
        assert 1.0 == tl.tracks['dog']['clips'][0]['score']
        assert 0.86 == tl.tracks['cat']['clips'][0]['score']
        assert 0.76 == tl.tracks['cow']['clips'][0]['score']
