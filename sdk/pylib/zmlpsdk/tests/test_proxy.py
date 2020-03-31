from unittest import TestCase
from unittest.mock import patch

import zmlpsdk.proxy
from zmlp.client import ZmlpClient
from zmlpsdk.storage import file_storage
from zmlpsdk.testing import zorroa_test_data, TestAsset

IMAGE_JPG = zorroa_test_data('images/set01/faces.jpg')
VIDEO_WEBM = zorroa_test_data('video/dc.webm')
VIDEO_MP4 = zorroa_test_data('video/sample_ipad.m4v')


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
        }
    ]

    @patch.object(ZmlpClient, 'stream')
    def test_get_proxy_level(self, stream_patch):
        asset = TestAsset(IMAGE_JPG, id='123456')
        asset.set_attr('files', self.file_list)

        prx1 = zmlpsdk.proxy.get_proxy_level(asset, 0)
        assert 'image_200x200.jpg' == prx1.name

        prx1 = zmlpsdk.proxy.get_proxy_level(asset, 9)
        assert 'image_400x400.jpg' == prx1.name

    @patch.object(ZmlpClient, 'stream')
    def test_get_proxy_level_path(self, stream_patch):
        asset = TestAsset(IMAGE_JPG, id='123456')
        asset.set_attr('files', self.file_list)

        path = zmlpsdk.proxy.get_proxy_level_path(asset, 0)
        assert '6942633d44e2d734460b5855926e1b47eea67d86.jpg' in path

        path = zmlpsdk.proxy.get_proxy_level_path(asset, 9)
        assert 'd64a279e098c9bda4a8156d9c60e3337f7d96b31.jpg' in path

    @patch.object(ZmlpClient, 'upload_file')
    def test_store_asset_proxy_unique(self, upload_patch):
        asset = TestAsset(IMAGE_JPG)
        upload_patch.return_value = {
            'id': 'assets/123456/proxy/image_200x200.jpg',
            'name': 'image_200x200.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 200,
                'height': 200
            }
        }
        # Should only be added to list once.
        zmlpsdk.proxy.store_asset_proxy(asset, IMAGE_JPG, (200, 200))
        zmlpsdk.proxy.store_asset_proxy(asset, IMAGE_JPG, (200, 200))

        upload_patch.return_value = {
            'id': 'assets/123456/proxy/image_200x200.mp4',
            'name': 'image_200x200.mp4',
            'category': 'proxy',
            'mimetype': 'video/mp4',
            'attrs': {
                'width': 200,
                'height': 200
            }
        }
        zmlpsdk.proxy.store_asset_proxy(asset, VIDEO_MP4, (200, 200))
        assert 2 == len(asset.get_files())

    @patch.object(file_storage.assets, 'store_file')
    @patch.object(ZmlpClient, 'upload_file')
    def test_store_asset_proxy_with_attrs(self, upload_patch, store_file_patch):
        upload_patch.return_value = {}

        asset = TestAsset(IMAGE_JPG)
        asset.set_attr('tmp.image_proxy_source_attrs', {'king': 'kong'})
        zmlpsdk.proxy.store_asset_proxy(
            asset, IMAGE_JPG, (200, 200), attrs={'foo': 'bar'})

        # Merges args from both the proxy_source_attrs attr and
        # args passed into store_proxy_media
        args, kwargs = store_file_patch.call_args_list[0]
        assert kwargs['attrs']['king'] == 'kong'
        assert kwargs['attrs']['width'] == 200
        assert kwargs['attrs']['height'] == 200
        assert kwargs['attrs']['foo'] == 'bar'
