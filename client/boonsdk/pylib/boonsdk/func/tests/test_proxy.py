import io
from unittest import TestCase
from unittest.mock import patch

from boonsdk import Asset
from boonsdk.func import get_proxy_image, get_proxy_record
from boonsdk.func.env import BoonFunctionClient


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

    def test_get_proxy_record(self):
        asset = Asset({"id": "123", "document": {}})
        asset.set_attr('files', self.file_list)

        prx1 = get_proxy_record(asset, 0)
        assert 'image_200x200.jpg' == prx1.name

        prx1 = get_proxy_record(asset, 9)
        assert 'image_400x400.jpg' == prx1.name

    @patch.object(BoonFunctionClient, 'download_file')
    def test_get_proxy_image(self, stream_patch):
        stream_patch.return_value = io.BytesIO(b'blah')
        asset = Asset({"id": "123", "document": {}})
        asset.set_attr('files', self.file_list)

        prx1 = get_proxy_image(asset, 0)
        assert prx1.read() == b'blah'
