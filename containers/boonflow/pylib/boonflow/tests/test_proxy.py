from unittest import TestCase
from unittest.mock import patch

import boonflow.proxy as proxy
from boonsdk.client import BoonClient
from boonflow.testing import test_path, TestAsset
from boonflow import file_storage

IMAGE_JPG = test_path('images/set01/faces.jpg')
VIDEO_WEBM = test_path('video/dc.webm')
VIDEO_MP4 = test_path('video/sample_ipad.m4v')


class ProxyFunctionTests(TestCase):
    file_list = [
        {
            'id': "assets/123456/proxy/image_200x200.jpg",
            'name': 'image_200x200.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 200,
                'height': 200
            }
        },
        {
            'id': "assets/123456/proxy/image_400x400.jpg",
            'name': 'image_400x400.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 400,
                'height': 400
            }
        },
        {
            'id': "assets/123456/proxy/video_400x400.mp4",
            'name': 'video_400x400.mp4',
            'category': 'proxy',
            'mimetype': 'video/mp4',
            'attrs': {
                'width': 400,
                'height': 400
            }
        },
        {
            'id': "assets/123456/proxy/video_500x500.mp4",
            'name': 'video_500x500.mp4',
            'category': 'proxy',
            'mimetype': 'video/mp4',
            'attrs': {
                'width': 500,
                'height': 500
            }
        },
        {
            'id': "assets/123456/audio/audio_proxy.flac",
            'name': 'audio_proxy.flac',
            'category': 'audio',
            'mimetype': 'audio/flac',
            'attrs': {
            }
        }
    ]

    @patch.object(file_storage.assets, 'get_native_uri', return_value="gs://foo/audio.flac")
    def test_get_audio_proxy(self, _):
        asset = TestAsset(IMAGE_JPG, id='123456')
        asset.set_attr('files', self.file_list)
        audio = proxy.get_audio_proxy(asset, False)
        assert audio

    def test_get_audio_proxy_not_exist(self):
        asset = TestAsset(IMAGE_JPG, id='123456')
        audio = proxy.get_audio_proxy(asset, False)
        assert not audio

    @patch.object(file_storage, 'localize_file')
    def test_get_audio_proxy_auto_create_no_audio(self, localize_patch):
        localize_patch.return_value = test_path("video/no_audio.mp4")
        asset = TestAsset(IMAGE_JPG, id='123456')
        audio = proxy.get_audio_proxy(asset, True)
        assert not audio

    @patch.object(file_storage.assets, 'store_file')
    @patch.object(file_storage, 'localize_file')
    def test_get_audio_proxy_auto_create_audio(self, localize_patch, store_patch):
        localize_patch.return_value = test_path("video/ted_talk.mp4")
        store_patch.return_value = True
        asset = TestAsset(IMAGE_JPG, id='123456')
        audio = proxy.get_audio_proxy(asset, True)
        assert audio

    def test_calculate_noralized_bbox(self):
        rect = proxy.calculate_normalized_bbox(1000, 1000, [
            0, 0, 500, 500, 200, 200
        ])
        assert [0.0, 0.0, 0.5, 0.5, 0.2, 0.2] == rect

    def test_calculate_pixel_bbox(self):
        rect = proxy.calculate_normalized_bbox(1000, 1000, [
            0, 0, 500, 500, 200, 200
        ])
        assert [0.0, 0.0, 0.5, 0.5, 0.2, 0.2] == rect

    @patch.object(BoonClient, 'stream')
    def test_get_proxy_level(self, stream_patch):
        asset = TestAsset(IMAGE_JPG, id='123456')
        asset.set_attr('files', self.file_list)

        prx1 = proxy.get_proxy_level(asset, 0)
        assert 'image_200x200.jpg' == prx1.name

        prx1 = proxy.get_proxy_level(asset, 9)
        assert 'image_400x400.jpg' == prx1.name

    @patch.object(BoonClient, 'stream')
    def test_get_proxy_level_path(self, stream_patch):
        asset = TestAsset(IMAGE_JPG, id='123456')
        asset.set_attr('files', self.file_list)

        path = proxy.get_proxy_level_path(asset, 0)
        assert '6942633d44e2d734460b5855926e1b47eea67d86.jpg' in path

        path = proxy.get_proxy_level_path(asset, 9)
        assert 'd64a279e098c9bda4a8156d9c60e3337f7d96b31.jpg' in path

    @patch.object(TestAsset, 'get_files')
    @patch('boonflow.proxy.file_storage.localize_file')
    def test_get_ocr_proxy(self, storage_patch, get_files_patch):
        image_path = test_path('images/set09/nvidia_manual_page.jpg')
        storage_patch.return_value = image_path
        asset = TestAsset(image_path)
        asset.set_attr('files', [
            {
                'category': 'ocr-proxy',
                'name': 'ocr-proxy.jpg',
                'mimetype': 'image/jpeg'
            }
        ])
        proxy.get_ocr_proxy_image(asset)
        get_files_patch.assert_called_with(category='ocr-proxy')
