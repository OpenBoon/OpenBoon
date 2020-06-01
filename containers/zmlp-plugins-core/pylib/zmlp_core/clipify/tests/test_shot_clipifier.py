import logging
from unittest.mock import patch

from zmlp import StoredFile
from zmlp_core.clipify.shot_clipify import ShotDetectionVideoClipifier
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_data

logging.basicConfig(level=logging.DEBUG)


class ShotDetectionVideoClipifierTests(PluginUnitTestCase):

    @patch.object(ShotDetectionVideoClipifier, 'expand')
    def test_process(self, expand_patch):
        proxy = {
            "id": "assets/id/proxy/long.m4v",
            "name": "video_1x1.m4v",
            "category": "proxy",
            "mimetype": "video/m4v",
            "attrs": {
                "width": 1,
                "height": 1
            }
        }
        video_path = zorroa_test_data('video/sample_ipad.m4v')
        file_storage.cache.precache_file(StoredFile(proxy), video_path)
        frame = Frame(TestAsset(video_path))

        # Set preconditions
        frame.asset.set_attr('media.type', 'video')
        frame.asset.set_attr('clip.track', 'full')
        frame.asset.set_attr('files', [proxy])

        processor = self.init_processor(ShotDetectionVideoClipifier(), {})
        processor.process(frame)

        assert expand_patch.call_count == 6

    @patch.object(ShotDetectionVideoClipifier, 'expand')
    def test_process_no_clips(self, expand_patch):
        video_path = zorroa_test_data('video/dc.webm', False)
        frame = Frame(TestAsset(video_path))
        proxy = {
            "id": "assets/{}/proxy/dc.webm".format(frame.asset.id),
            "name": "dc.webm",
            "category": "proxy",
            "mimetype": "video/m4v",
            "attrs": {
                "width": 1,
                "height": 1
            }
        }

        # Set preconditions
        frame.asset.set_attr('media.type', 'video')
        frame.asset.set_attr('clip.track', 'full')
        frame.asset.set_attr('files', [proxy])
        file_storage.cache.precache_file(StoredFile(proxy), video_path)

        processor = self.init_processor(ShotDetectionVideoClipifier(), {})
        processor.process(frame)
        assert expand_patch.call_count == 0
