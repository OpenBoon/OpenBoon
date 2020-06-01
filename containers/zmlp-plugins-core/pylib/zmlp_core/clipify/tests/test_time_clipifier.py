import logging
from unittest.mock import patch

from zmlp_core.clipify.time_clipify import TimeBasedVideoClipifier
from zmlpsdk import StoredFile, Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_data

logging.basicConfig(level=logging.DEBUG)


class TimeBasedVideoClipifierTests(PluginUnitTestCase):

    @patch.object(TimeBasedVideoClipifier, 'expand')
    def test_process(self, expand_patch):
        proxy = {
            "id": "assets/id/proxy/video_1x1.m4v",
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
        frame.asset.set_attr('clip.track', 'full')
        frame.asset.set_attr('clip.stop', float(100))
        frame.asset.set_attr('files', [proxy])
        file_storage.cache.precache_file(StoredFile(proxy), video_path)

        # We have to add a proxy to use ML, there is no source
        # fallback currently.

        processor = self.init_processor(TimeBasedVideoClipifier(), {})
        processor.process(frame)

        assert expand_patch.call_count == 4
