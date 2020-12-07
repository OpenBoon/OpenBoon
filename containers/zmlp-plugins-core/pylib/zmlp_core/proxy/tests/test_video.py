import logging
from unittest.mock import patch

from zmlpsdk import Frame
from zmlpsdk.testing import TestAsset, PluginUnitTestCase, zorroa_test_data
from zmlp.client import ZmlpClient
from zmlp_core.proxy.video import VideoProxyProcessor

logging.basicConfig(level=logging.INFO)


class VideoProxyProcessorTests(PluginUnitTestCase):

    def setUp(self):
        self.processor = self.init_processor(VideoProxyProcessor(), {})

    def test_get_transcode_op_transcode(self):
        movie_path = zorroa_test_data('video/1324_CAPS_23.0_030.00_15_MISC.mov')
        frame = Frame(TestAsset(movie_path))
        frame.asset.set_attr('media.videoCodec', 'mjpeg')
        op = self.processor.get_transcoding_process(frame.asset)
        self.assertEquals('TRANSCODE', op)

    def test_get_transcode_op_transcode_too_large(self):
        movie_path = zorroa_test_data('video/out.mp4')
        frame = Frame(TestAsset(movie_path))
        frame.asset.set_attr('media.videoCodec', 'h264')
        frame.asset.set_attr('media.width', 2368)
        frame.asset.set_attr('media.height', 1080)
        op = self.processor.get_transcoding_process(frame.asset)
        self.assertEquals('TRANSCODE', op)

    def test_get_transcode_op_copy(self):
        movie_path = zorroa_test_data('video/out.mp4')
        frame = Frame(TestAsset(movie_path))
        frame.asset.set_attr('media.videoCodec', 'h264')
        frame.asset.set_attr('media.width', 1280)
        frame.asset.set_attr('media.height', 1080)
        op = self.processor.get_transcoding_process(frame.asset)
        self.assertEquals('COPY', op)

    def test_get_transcode_op_optimize(self):
        movie_path = zorroa_test_data('video/ted_talk.mp4')
        frame = Frame(TestAsset(movie_path))
        frame.asset.set_attr('media.videoCodec', 'h264')
        frame.asset.set_attr('media.width', 854)
        frame.asset.set_attr('media.height', 480)
        op = self.processor.get_transcoding_process(frame.asset)
        self.assertEquals('OPTIMIZE', op)

    @patch.object(ZmlpClient, 'upload_file')
    @patch('zmlp_core.proxy.video.store_media_proxy')
    def test_process_transcode(self, store_patch, post_patch):
        movie_path = zorroa_test_data('video/test_P1113171.mov')
        frame = Frame(TestAsset(movie_path))

        post_patch.return_value = {
            "name": "video_640x360.jpg",
            "category": "proxy",
            "mimetype": "image/jpeg",
            "assetId": "12345",
            "attrs": {
                "width": 1280,
                "height": 720
            }
        }

        asset = frame.asset
        asset.set_attr('media', {'videoCodec': 'mjpeg', 'width': 3840, 'height': 2160})
        asset.set_attr('clip', {'start': 0, 'stop': 15.05, 'length': 15.05, 'track': 'full'})
        self.processor.process(frame)

        call = store_patch.call_args_list[0]
        asset, path, ptype, size, attrs = call[0]

        assert frame.asset.id == asset.id
        assert path.endswith('.mp4')
        assert ptype == 'video'
        assert attrs['transcode'] == 'full'

    @patch.object(ZmlpClient, 'upload_file')
    @patch('zmlp_core.proxy.video.store_media_proxy')
    def test_process_copy(self, store_patch, post_patch):
        movie_path = zorroa_test_data('video/out.mp4')
        frame = Frame(TestAsset(movie_path))

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
        asset = frame.asset
        asset.set_attr('media', {'videoCodec': 'h264', 'width': 640, 'height': 360})
        asset.set_attr('clip', {'start': 0, 'stop': 15.05, 'length': 15.05, 'track': 'full'})
        self.processor.process(frame)

        call = store_patch.call_args_list[0]
        asset, path, ptype, size, attrs = call[0]

        assert frame.asset.id == asset.id
        assert path.endswith('.mp4')
        assert ptype == 'video'
        assert attrs['transcode'] == 'none'

    @patch.object(ZmlpClient, 'upload_file')
    @patch('zmlp_core.proxy.video.store_media_proxy')
    def test_process_optimize(self, store_patch, post_patch):
        movie_path = zorroa_test_data('video/ted_talk.mp4')
        frame = Frame(TestAsset(movie_path))

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
        asset = frame.asset
        asset.set_attr('media', {'videoCodec': 'h264', 'width': 640, 'height': 360})
        asset.set_attr('clip', {'start': 0, 'stop': 15.05, 'length': 15.05, 'track': 'full'})
        self.processor.process(frame)

        call = store_patch.call_args_list[0]
        asset, path, ptype, size, attrs = call[0]

        assert frame.asset.id == asset.id
        assert path.endswith('.mp4')
        assert 'video' == ptype
        assert attrs['transcode'] == 'optimize'
