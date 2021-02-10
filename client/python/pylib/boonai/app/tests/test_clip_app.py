import copy
import os
import unittest
from unittest.mock import patch, Mock

from boonai import Asset, ZmlpClient, app_from_env
from boonai.entity import TimelineBuilder


class VideoClipAppTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()

    @patch.object(ZmlpClient, 'post', return_value={'created': 1})
    def test_create_clips(self, _):
        asset = Asset({'id': '123'})
        tl = TimelineBuilder(asset, 'foo')
        tl.add_clip('dogs', 0.0, 1.5, 'dog')

        res = self.app.clips.create_clips(tl)
        assert res['created'] == 1

    @patch.object(ZmlpClient, 'post')
    def test_create_clip(self, post_patch):
        post_patch.return_value = {
            'id': '12345',
            'timeline': 'foo-bar',
            'track': 'track'
        }
        asset = Asset({'id': '123'})
        clip = self.app.clips.create_clip(
            asset, 'foo-bar', 'bing', 1, 10, 'cats')

        assert clip.timeline == 'foo-bar'
        assert clip.track == 'track'

    @patch.object(ZmlpClient, 'post')
    def test_get_webvtt(self, post_patch):
        mockresponse = Mock()
        mockresponse.content = b'foo'

        post_patch.return_value = mockresponse
        asset = Asset({'id': '123'})
        vtt = self.app.clips.get_webvtt(asset)
        assert 'foo' == vtt

    @patch.object(ZmlpClient, 'post')
    def test_get_webvtt_to_file(self, post_patch):
        mockresponse = Mock()
        mockresponse.content = b'foo'
        post_patch.return_value = mockresponse

        path = "/tmp/some.vtt"
        try:
            os.unlink(path)
        except OSError:
            pass

        with open(path, "w") as fp:
            asset = Asset({'id': '123'})
            self.app.clips.get_webvtt(asset, fp)

        os.path.exists(path)
        with open(path, 'r') as fp:
            contents = fp.read()
        assert 'foo' == contents

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_scroll_search(self, post_patch, del_patch):
        scroll_result = copy.deepcopy(mock_clip_search_result)
        scroll_result['_scroll_id'] = 'abc123'

        post_patch.side_effect = [scroll_result, {'hits': {'hits': []}}]
        del_patch.return_value = {}
        for clip in self.app.clips.scroll_search():
            assert clip.id == 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg'
            assert clip.asset_id == '12345'
            assert clip.timeline == 'foo'
            assert clip.track == 'bar'

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_search(self, post_patch, del_patch):
        scroll_result = copy.deepcopy(mock_clip_search_result)
        post_patch.side_effect = [scroll_result, {'hits': {'hits': []}}]
        del_patch.return_value = {}
        for clip in self.app.clips.search():
            assert clip.id == 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg'
            assert clip.asset_id == '12345'
            assert clip.timeline == 'foo'
            assert clip.track == 'bar'

    @patch.object(ZmlpClient, 'get')
    def test_get_clip(self, get_patch):
        get_patch.return_value = {
            'id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
            'assetId': '12345',
            'timeline': 'foo',
            'track': 'bar',
            'start': 0.5,
            'stop': 1.5,
            'content': ['everybody dance now']
        }
        clip = self.app.clips.get_clip('abc123')
        assert clip.id == 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg'
        assert clip.asset_id == '12345'
        assert clip.timeline == 'foo'
        assert clip.track == 'bar'


mock_clip_search_result = {
    'took': 4,
    'timed_out': False,
    'hits': {
        'total': {'value': 2},
        'max_score': 0.2876821,
        'hits': [
            {
                '_index': 'litvqrkus86sna2w',
                '_type': 'asset',
                '_id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                '_score': 0.2876821,
                '_source': {
                    'clip': {
                        'assetId': '12345',
                        'timeline': 'foo',
                        'track': 'bar',
                        'start': 0.5,
                        'stop': 1.5,
                        'content': ["everybody dance now"]
                    }
                }
            }
        ]
    }
}
