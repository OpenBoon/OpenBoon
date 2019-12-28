import pytest
from unittest import TestCase
from unittest.mock import patch

from zmlp.analysis.testing import zorroa_test_data, TestAsset
import zmlp.analysis.proxy
from zmlp.client import ZmlpClient

IMAGE_JPG = zorroa_test_data('images/set01/faces.jpg')
VIDEO_WEBM = zorroa_test_data('video/dc.webm')
VIDEO_MP4 = zorroa_test_data('video/sample_ipad.m4v')


class ProxyFunctionTests(TestCase):

    file_list = [
        {
            'name': 'proxy_200x200.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 200,
                'height': 200
            }
        },
        {
            'name': 'proxy_400x400.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 400,
                'height': 400
            }
        },
        {
            'name': 'proxy_400x400.mp4',
            'category': 'proxy',
            'mimetype': 'video/mp4',
            'attrs': {
                'width': 400,
                'height': 400
            }
        },
        {
            'name': 'proxy_500x500.mp4',
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
        assert '1246524d107aa3b91ccb1ea43c136d366156d2b1' in path

        path = zmlp.analysis.proxy.get_proxy_level(asset, 9)
        assert '9bf44aa1e82ab54ce7adc212b3918c6047849c15' in path

    @patch.object(ZmlpClient, 'stream')
    def test_get_proxy_min_width(self, stream_patch):
        asset = TestAsset(IMAGE_JPG, id='123456')
        asset.set_attr('files', self.file_list)

        path = zmlp.analysis.proxy.get_proxy_min_width(asset, 300)
        assert '9bf44aa1e82ab54ce7adc212b3918c6047849c15' in path

        path = zmlp.analysis.proxy.get_proxy_min_width(asset, 350, mimetype='video/')
        assert '5bedc72da42dd3e296e14c26eaba01c1568c71d0' in path

        path = zmlp.analysis.proxy.get_proxy_min_width(
            asset, 1025, mimetype='image/', fallback=True)
        assert 'faces.jpg' in path

        with pytest.raises(ValueError):
            zmlp.analysis.proxy.get_proxy_min_width(
                asset, 1025, mimetype='video/', fallback=False)

    @patch.object(ZmlpClient, 'upload_file')
    def test_add_proxy_file(self, upload_patch):
        asset = TestAsset(IMAGE_JPG)
        upload_patch.return_value = {
            'name': 'proxy_200x200.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 200,
                'height': 200
            }
        }
        # Should only be added to list once.
        zmlp.analysis.proxy.store_asset_proxy(asset, IMAGE_JPG, (200, 200))
        zmlp.analysis.proxy.store_asset_proxy(asset, IMAGE_JPG, (200, 200))

        upload_patch.return_value = {
            'name': 'proxy_200x200.mp4',
            'category': 'proxy',
            'mimetype': 'video/mp4',
            'attrs': {
                'width': 200,
                'height': 200
            }
        }
        zmlp.analysis.proxy.store_asset_proxy(asset, VIDEO_MP4, (200, 200))
        assert 2 == len(asset.get_files())
