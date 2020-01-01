from unittest import TestCase
from unittest.mock import patch

import pytest

import zmlp.analysis.proxy
from zmlp.analysis.storage import file_storage
from zmlp.analysis.testing import zorroa_test_data, TestAsset
from zmlp.client import ZmlpClient

IMAGE_JPG = zorroa_test_data('images/set01/faces.jpg')
VIDEO_WEBM = zorroa_test_data('video/dc.webm')
VIDEO_MP4 = zorroa_test_data('video/sample_ipad.m4v')


class ProxyFunctionTests(TestCase):
    file_list = [
        {
            'name': 'image_200x200.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 200,
                'height': 200
            }
        },
        {
            'name': 'image_400x400.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 400,
                'height': 400
            }
        },
        {
            'name': 'video_400x400.mp4',
            'category': 'proxy',
            'mimetype': 'video/mp4',
            'attrs': {
                'width': 400,
                'height': 400
            }
        },
        {
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

        path = zmlp.analysis.proxy.get_proxy_level(asset, 0)
        assert '4b6f2919eb95dca550bd50deb5e84b25aec42ccc' in path

        path = zmlp.analysis.proxy.get_proxy_level(asset, 9)
        assert '760e2adca79e16645154b8a3ece4c6fc35b46663' in path

    @patch.object(ZmlpClient, 'stream')
    def test_get_proxy_min_width(self, stream_patch):
        asset = TestAsset(IMAGE_JPG, id='123456')
        asset.set_attr('files', self.file_list)

        path = zmlp.analysis.proxy.get_proxy_min_width(asset, 300)
        assert '760e2adca79e16645154b8a3ece4c6fc35b46663' in path

        path = zmlp.analysis.proxy.get_proxy_min_width(asset, 350, mimetype='video/')
        assert 'c8edf96cf98802cd6ac2acce12cad792cec2cb02' in path

        path = zmlp.analysis.proxy.get_proxy_min_width(
            asset, 1025, mimetype='image/', fallback=True)
        assert 'faces.jpg' in path

        with pytest.raises(ValueError):
            zmlp.analysis.proxy.get_proxy_min_width(
                asset, 1025, mimetype='video/', fallback=False)

    @patch.object(ZmlpClient, 'upload_file')
    def test_store_proxy_media_unique(self, upload_patch):
        asset = TestAsset(IMAGE_JPG)
        upload_patch.return_value = {
            'name': 'image_200x200.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 200,
                'height': 200
            }
        }
        # Should only be added to list once.
        zmlp.analysis.proxy.store_proxy_media(asset, IMAGE_JPG, (200, 200))
        zmlp.analysis.proxy.store_proxy_media(asset, IMAGE_JPG, (200, 200))

        upload_patch.return_value = {
            'name': 'image_200x200.mp4',
            'category': 'proxy',
            'mimetype': 'video/mp4',
            'attrs': {
                'width': 200,
                'height': 200
            }
        }
        zmlp.analysis.proxy.store_proxy_media(asset, VIDEO_MP4, (200, 200))
        assert 2 == len(asset.get_files())

    @patch.object(file_storage, 'store_asset_file')
    @patch.object(ZmlpClient, 'upload_file')
    def test_store_proxy_media_with_attrs(self, upload_patch, store_file_patch):
        upload_patch.return_value = {}

        asset = TestAsset(IMAGE_JPG)
        asset.set_attr('tmp.image_proxy_source_attrs', {'king': 'kong'})
        zmlp.analysis.proxy.store_proxy_media(
            asset, IMAGE_JPG, (200, 200), attrs={'foo': 'bar'})

        # Merges args from both the proxy_source_attrs attr and
        # args passed into store_proxy_media
        args, kwargs = store_file_patch.call_args_list[0]
        assert kwargs['attrs']['king'] == 'kong'
        assert kwargs['attrs']['width'] == 200
        assert kwargs['attrs']['height'] == 200
        assert kwargs['attrs']['foo'] == 'bar'
