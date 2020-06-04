import json
import unittest

from zmlp import to_json
from zmlpsdk.timeline import Timeline


class TimelineTests(unittest.TestCase):

    def test_empty_timeline(self):
        tl = Timeline('foo')
        assert 'foo' == tl.name
        assert [] == tl.tracks

        jtl = json.loads(to_json(tl))

        assert 'foo' == jtl['name']
        assert {} == jtl['metadata']
        assert [] == jtl['tracks']

    def test_add_track(self):
        tl = Timeline('foo')
        track = tl.add_track('foo1', {'bing': 'bong'})

        assert 'foo1' == track.name
        assert 'foo1' == tl._tracks['foo1'].name

        # check the metadata was merged.
        track = tl.add_track('foo1', {'flim': 'flam'})
        assert 'flam' == track.metadata['flim']
        assert 'bong' == track.metadata['bing']

    def test_sorted_tracks_with_sort(self):
        tl = Timeline('foo')
        tl.add_track('a', sort=10)
        tl.add_track('b', sort=0)
        tl.add_track('c', sort=1)

        sorted = ['b', 'c', 'a']
        tracks = [tr.name for tr in tl.tracks]
        assert sorted == tracks

    def test_sorted_tracks_with_name(self):
        tl = Timeline('foo')
        tl.add_track('a')
        tl.add_track('b')
        tl.add_track('c')

        sorted = ['a', 'b', 'c']
        tracks = [tr.name for tr in tl.tracks]
        assert sorted == tracks

    def test_add_clip(self):
        tl = Timeline('foo')
        track = tl.add_track('foo1', {'bing': 'bong'})
        clip1 = track.add_clip(1, 5, {'bing': 'bong'})
        clip2 = track.add_clip(1, 5, {'flim': 'flam'})

        assert 'flam' == clip1.metadata['flim']
        assert 'bong' == clip1.metadata['bing']
        assert 'flam' == clip2.metadata['flim']
        assert 'bong' == clip2.metadata['bing']

    def test_clip_intersect(self):
        tl = Timeline('foo')
        track = tl.add_track('foo1', {'bing': 'bong'})
        clip1 = track.add_clip(1.01, 5.54)
        assert clip1.intersects(4.05)
        assert not clip1.intersects(1.0)
        assert clip1.intersects(5.54)
        assert not clip1.intersects(5.545)
