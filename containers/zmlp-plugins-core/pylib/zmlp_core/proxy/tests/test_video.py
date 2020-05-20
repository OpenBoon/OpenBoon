import logging
from unittest.mock import patch

from zmlpsdk import Frame
from zmlpsdk.testing import TestAsset, PluginUnitTestCase, zorroa_test_data
from zmlp.client import ZmlpClient
from zmlp_core.proxy.video import ExtractVideoClipProxyProcessor, VideoProxyProcessor

logging.basicConfig()


class ExtractVideoClipProxyProcessorTests(PluginUnitTestCase):

    def setUp(self):
        self.movie_path = zorroa_test_data('video/FatManOnABike1914.mp4')
        self.frame = Frame(TestAsset(self.movie_path))
        self.processor = self.init_processor(ExtractVideoClipProxyProcessor(), {})

    @patch.object(ZmlpClient, 'upload_file')
    @patch('zmlp_core.proxy.video.store_asset_proxy')
    def test_process(self, store_patch, post_patch):
        post_patch.return_value = {
            "name": "video_512x341.jpg",
            "category": "proxy",
            "mimetype": "image/jpeg",
            "assetId": "12345",
            "attrs": {
                "width": 450,
                "height": 360
            }
        }
        asset = self.frame.asset
        asset.set_attr('media', {'width': 450, 'height': 360})
        asset.set_attr('clip', {'start': 10, 'stop': 15, 'length': 5, 'track': 'random'})
        self.processor.process(self.frame)

        call = store_patch.call_args_list[0]
        asset, path, size, type, attrs = call[0]

        assert self.frame.asset.id == asset.id
        assert path.endswith('.mp4')
        assert size == (450, 360)
        assert type == 'video'
        assert attrs['frames'] == 125
        assert attrs['frameRate'] == 25.0

    @patch('zmlp_core.proxy.video.store_asset_proxy')
    def test_process_skipped_no_clip(self, store_patch):
        # No clip defined so it was skipped
        asset = self.frame.asset
        asset.set_attr('media', {'width': 450, 'height': 360})
        self.processor.process(self.frame)
        assert len(store_patch.call_args_list) == 0

    @patch('zmlp_core.proxy.video.store_asset_proxy')
    def test_process_skipped_full(self, store_patch):
        # A clip is on the full track, so its skipped.
        asset = self.frame.asset
        asset.set_attr('media', {'width': 450, 'height': 360})
        asset.set_attr('clip', {'start': 10, 'stop': 15, 'length': 5, 'track': 'full'})
        self.processor.process(self.frame)
        assert len(store_patch.call_args_list) == 0


class VideoProxyProcessorTests(PluginUnitTestCase):

    def setUp(self):
        self.movie_path = zorroa_test_data('video/sample_ipad.m4v')
        self.frame = Frame(TestAsset(self.movie_path))
        self.processor = self.init_processor(VideoProxyProcessor(), {})

    @patch.object(ZmlpClient, 'upload_file')
    @patch('zmlp_core.proxy.video.store_asset_proxy')
    def test_process(self, store_patch, post_patch):
        post_patch.return_value = {
            "name": "video_640x360.jpg",
            "category": "proxy",
            "mimetype": "image/jpeg",
            "assetId": "12345",
            "attrs": {
                "width": 640,
                "height": 360
            }
        }
        asset = self.frame.asset
        asset.set_attr('media', {'width': 640, 'height': 360})
        asset.set_attr('clip', {'start': 0, 'stop': 15.05, 'length': 15.05, 'track': 'full'})
        self.processor.process(self.frame)

        call = store_patch.call_args_list[0]
        asset, path, size, type, attrs = call[0]

        assert self.frame.asset.id == asset.id
        assert path.endswith('.mp4')
        assert size == (640, 360)
        assert type == 'video'
        assert attrs['frames'] == 452
        assert attrs['frameRate'] == 29.97
