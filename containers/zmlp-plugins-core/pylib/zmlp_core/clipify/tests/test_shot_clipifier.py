import logging
from unittest.mock import patch

from zmlp import ZmlpClient
from zmlp.analysis import Frame
from zmlp.analysis.proxy import store_proxy_media
from zmlp.analysis.testing import PluginUnitTestCase, TestAsset, zorroa_test_data
from zmlp_core.clipify.shot_clipify import ShotDetectionVideoClipifier

logging.basicConfig(level=logging.DEBUG)


class ShotDetectionVideoClipifierTests(PluginUnitTestCase):

    @patch.object(ZmlpClient, 'upload_file')
    @patch.object(ShotDetectionVideoClipifier, 'expand')
    def test_process(self, expand_patch, upload_patch):
        upload_patch.return_value = {
            "name": "video_1x1.m4v",
            "category": "proxy",
            "mimetype": "video/m4v",
            "attrs": {
                "width": 1,
                "height": 1
            }
        }

        video_path = zorroa_test_data('video/sample_ipad.m4v')
        frame = Frame(TestAsset(video_path))
        # Set preconditions
        frame.asset.set_attr('media.type', 'video')
        frame.asset.set_attr('clip.timeline', 'full')

        # We have to add a proxy to use ML, there is no source
        # fallback currently.
        store_proxy_media(frame.asset, video_path, (1, 1), type='video')
        processor = self.init_processor(ShotDetectionVideoClipifier(), {})
        processor.process(frame)

        assert expand_patch.call_count == 6

    @patch.object(ZmlpClient, 'upload_file')
    @patch.object(ShotDetectionVideoClipifier, 'expand')
    def test_process_no_clips(self, expand_patch, upload_patch):
        upload_patch.return_value = {
            "name": "video_1x1.webm",
            "category": "proxy",
            "mimetype": "video/webm",
            "attrs": {
                "width": 1,
                "height": 1
            }
        }

        video_path = zorroa_test_data('video/dc.webm')
        frame = Frame(TestAsset(video_path))
        # Set preconditions
        frame.asset.set_attr('media.type', 'video')
        frame.asset.set_attr('clip.timeline', 'full')

        # We have to add a proxy to use ML, there is no source
        # fallback currently.
        store_proxy_media(frame.asset, video_path, (1, 1), type='video')
        processor = self.init_processor(ShotDetectionVideoClipifier(), {})
        processor.process(frame)

        assert expand_patch.call_count == 0
