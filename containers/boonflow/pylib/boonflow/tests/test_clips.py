import logging
from unittest import TestCase

from boonflow.clips import ClipTracker
from boonflow.testing import TestAsset

logging.basicConfig(level=logging.DEBUG)


class TestClipTracker(TestCase):

    def test_clip_tracker_append(self):
        c = ClipTracker(TestAsset(), "pets")
        c.append(1, {"dog": 1, "cat": 0.9, "cow": 0.8})
        c.append(3, {"dog": 1, "cat": 0.9, "cow": 0.8})
        c.append(4, {"dog": 1.0, "cow": 0.9})

        tl = c.timeline
        assert 1 == len(tl.tracks['cat']['clips'])

    def test_build_timeline(self):
        c = ClipTracker(TestAsset(), "pets")
        c.append(1, {"dog": 1.0, "cat": 0.80, "cow": 0.76})
        c.append(3, {"dog": 1.0, "cat": 0.86, "cow": 1.0})
        c.append(4, {"dog": 1.0, "cow": 0.85})

        tl = c.build_timeline(5)
        assert 1 == len(tl.tracks['dog']['clips'])
        assert 1 == len(tl.tracks['cow']['clips'])
        assert 1 == tl.tracks['cow']['clips'][0]['start']
        assert 5 == tl.tracks['cow']['clips'][0]['stop']
        assert 1.0 == tl.tracks['dog']['clips'][0]['score']
        assert 0.86 == tl.tracks['cat']['clips'][0]['score']
        assert 1.0 == tl.tracks['cow']['clips'][0]['score']

    def test_build_timeline_list(self):
        c = ClipTracker(TestAsset(), "pets")
        c.append(1, ['dog', 'cat', 'cow'])
        c.append(3, ['dog', 'cat', 'cow'])
        c.append(4, ['dog', 'cow'])

        tl = c.build_timeline(5)
        assert 1.0 == tl.tracks['dog']['clips'][0]['score']
        assert 1.0 == tl.tracks['cat']['clips'][0]['score']
        assert 1.0 == tl.tracks['cow']['clips'][0]['score']
