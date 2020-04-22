import logging
from unittest.mock import patch

from zmlp import ZmlpClient
from zmlp_core.clipify.shot_clipify import ShotDetectionVideoClipifier
from zmlp_core.util.media import store_asset_proxy
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_data
from zmlpsdk import Frame

logging.basicConfig(level=logging.DEBUG)


class ShotDetectionVideoClipifierTests(PluginUnitTestCase):

    @patch.object(ZmlpClient, 'upload_file')
    @patch.object(ShotDetectionVideoClipifier, 'expand')
    def test_process(self, expand_patch, upload_patch):
        file_storage = {
            "id": "assets/id/proxy/long.m4v",
            "name": "video_1x1.m4v",
            "category": "proxy",
            "mimetype": "video/m4v",
            "attrs": {
                "width": 1,
                "height": 1
            }
        }
        upload_patch.return_value = file_storage

        video_path = zorroa_test_data('video/sample_ipad.m4v')
        frame = Frame(TestAsset(video_path))
        # Set preconditions
        frame.asset.set_attr('media.type', 'video')
        frame.asset.set_attr('clip.timeline', 'full')

        # We have to add a proxy to use ML, there is no source
        # fallback currently.
        store_asset_proxy(frame.asset, video_path, (1, 1), type='video')
        processor = self.init_processor(ShotDetectionVideoClipifier(), {})
        processor.process(frame)

        assert expand_patch.call_count == 6

    @patch.object(ZmlpClient, 'upload_file')
    @patch.object(ShotDetectionVideoClipifier, 'expand')
    def test_process_no_clips(self, expand_patch, upload_patch):
        video_path = zorroa_test_data('video/dc.webm', False)
        frame = Frame(TestAsset(video_path))
        # Set preconditions
        frame.asset.set_attr('media.type', 'video')
        frame.asset.set_attr('clip.timeline', 'full')

        file_storage = {
            "id": "assets/{}/proxy/dc.webm".format(frame.asset.id),
            "name": "dc.webm",
            "category": "proxy",
            "mimetype": "video/m4v",
            "attrs": {
                "width": 1,
                "height": 1
            }
        }
        upload_patch.return_value = file_storage
        # We have to add a proxy to use ML, there is no source
        # fallback currently.

        store_asset_proxy(frame.asset, video_path, (1, 1), type='video')
        processor = self.init_processor(ShotDetectionVideoClipifier(), {})
        processor.process(frame)

        assert expand_patch.call_count == 0
