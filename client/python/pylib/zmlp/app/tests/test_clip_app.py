import unittest
from unittest.mock import patch

from zmlp import Asset, ZmlpClient, app_from_env
from zmlp.entity import TimelineBuilder


class ClipAppTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()

    @patch.object(ZmlpClient, 'post', return_value={'created': 1})
    def test_create_clips_from_timeline(self, _):
        asset = Asset({'id': '123'})
        tl = TimelineBuilder(asset, 'foo')
        tl.add_clip('dogs', 0.0, 1.5, 'dog')

        res = self.app.clips.create_clips_from_timeline(tl)
        assert res['created'] == 1
