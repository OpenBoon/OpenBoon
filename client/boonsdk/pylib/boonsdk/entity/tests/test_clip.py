import logging
import unittest

from boonsdk import TimelineBuilder, Asset

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class TimelineBulderTests(unittest.TestCase):

    def setUp(self):
        self.tl = TimelineBuilder(Asset({'id': '1234'}), 'label-detection')
        self.tl.add_clip('dogs', 0, 1, 'dog')

    def test_add_clip(self):
        clip = self.tl.tracks['dogs']['clips'][0]
        assert clip['content'] == ['dog']
        assert clip['start'] == 0
        assert clip['stop'] == 1

    def test_add_clip_with_bbox(self):
        bbox = [0.1, 0.2, 0.1, 0.2]
        self.tl.add_clip('cat', 0, 1, 'cat', bbox=bbox)
        clip = self.tl.tracks['cat']['clips'][0]
        assert clip['bbox'] == bbox

    def test_for_json(self):
        struct = self.tl.for_json()
        assert struct['name'] == 'label-detection'
        assert struct['assetId'] == '1234'
        assert struct['tracks'][0]['name'] == 'dogs'
