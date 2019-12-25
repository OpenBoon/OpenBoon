import os
import urllib3
import logging
from unittest import TestCase
from unittest.mock import patch

import pytest
from minio.api import Minio

from zmlp.analysis import storage
from zmlp.analysis.testing import zorroa_test_data, TestAsset
from zmlp.client import ZmlpClient

logging.basicConfig(level=logging.DEBUG)


class LocalFileCacheTests(TestCase):

    def setUp(self):
        os.environ['MLSTORAGE_URL'] = "http://localhost:9000"
        self.lfc = storage.LocalFileCache()

    def tearDown(self):
        self.lfc.clear()

    def test_init_with_task_id(self):
        os.environ['ZMLP_TASK_ID'] = '1234abcd5678'
        try:
            cache = storage.LocalFileCache()
            path = cache.localize_uri('https://i.imgur.com/WkomVeG.jpg')
            assert os.environ['ZMLP_TASK_ID'] in path
        finally:
            del os.environ['ZMLP_TASK_ID']
            cache.close()

    def test_localize_http(self):
        path = self.lfc.localize_uri('https://i.imgur.com/WkomVeG.jpg')
        assert os.path.exists(path)
        assert os.path.getsize(path) == 267493

    def test_localize_gs(self):
        path = self.lfc.localize_uri('gs://zorroa-dev-data/image/pluto_2.1.jpg')
        assert os.path.exists(path)
        assert os.path.getsize(path) == 65649

    def test_get_path(self):
        path = self.lfc.get_path('spock', '.kirk')
        filename = '1a569625e9949f82ab1be5257ab2cab1f7524c6d.kirk'
        assert path.endswith(filename)

    def test_clear(self):
        path = self.lfc.localize_uri('https://i.imgur.com/WkomVeG.jpg')
        assert os.path.exists(path)
        self.lfc.clear()
        assert not os.path.exists(path)

    @patch.object(ZmlpClient, 'stream')
    def test_localize_asset_file(self, post_patch):
        pfile = {
            'name': 'cat.jpg',
            'category': 'proxy'
        }
        post_patch.return_value = '/tmp/cat.jpg'
        path = self.lfc.localize_asset_file(TestAsset(id='123456'), pfile)
        assert path.endswith('c7bc251d55d2cfb3f5b0c86d739877583556f890.jpg')

    @patch.object(ZmlpClient, 'stream')
    def test_localize_asset_file_with_asset_override(self, post_patch):
        pfile = {
            'name': 'cat.jpg',
            'category': 'proxy',
            'sourceAssetId': 'bingo'
        }
        post_patch.return_value = '/tmp/cat.jpg'
        asset = TestAsset(id='123456')
        self.lfc.localize_asset_file(asset, pfile)
        assert "assets/bingo/files" in post_patch.call_args_list[0][0][0]

    @patch.object(ZmlpClient, 'stream')
    def test_localize_asset_source_file(self, post_patch):
        pfile = {
            'name': 'cat.jpg',
            'category': 'source'
        }
        post_patch.return_value = '/tmp/cat.jpg'
        asset = TestAsset(id='123456')
        asset.set_attr('files', [pfile])
        path = self.lfc.localize_remote_file(asset)
        assert path.endswith('3c25baa7cf0b59d64c0179a1e0030072444eac3b.jpg')

    @patch.object(ZmlpClient, 'stream')
    def test_localize_asset_file_with_copy(self, post_patch):
        pfile = {
            'name': 'cat.jpg',
            'category': 'proxy'
        }
        post_patch.return_value = '/tmp/toucan.jpg'
        bird = zorroa_test_data('images/set01/toucan.jpg', uri=False)
        path = self.lfc.localize_asset_file(TestAsset(id='123456'), pfile, bird)
        assert os.path.getsize(path) == os.path.getsize(bird)

    def test_localize_file_obj_with_uri(self):
        test_asset = TestAsset('https://i.imgur.com/WkomVeG.jpg')
        path = self.lfc.localize_remote_file(test_asset)
        assert os.path.exists(path)

    def test_localize_file_str(self):
        path = self.lfc.localize_remote_file('https://i.imgur.com/WkomVeG.jpg')
        assert os.path.exists(path)

    @patch.object(Minio, 'get_object')
    def test_localize_mlstorage_uri(self, get_object_patch):
        http = urllib3.PoolManager()
        r = http.request('GET', 'http://i.imgur.com/WkomVeG.jpg', preload_content=False)

        get_object_patch.return_value = r
        path = self.lfc.localize_remote_file('zmlp://ml-storage/officer/pdf/proxy.1.jpg')

        assert os.path.exists(path)
        assert os.path.getsize(path) == 267493

    @patch.object(ZmlpClient, 'stream')
    def test_localize_asset_file_dict(self, post_patch):
        post_patch.return_value = '/tmp/toucan.jpg'
        pfile = {
            'name': 'cat.jpg',
            'category': 'proxy'
        }
        path = self.lfc.localize_asset_file(TestAsset(id='123456'), pfile)
        assert path.endswith('c7bc251d55d2cfb3f5b0c86d739877583556f890.jpg')

    def test_close(self):
        pfile = {
            'name': 'cat.jpg',
            'category': 'proxy'
        }
        self.lfc.localize_asset_file(TestAsset(), pfile,
                                     zorroa_test_data('images/set01/toucan.jpg'))
        self.lfc.close()

        with pytest.raises(FileNotFoundError):
            self.lfc.localize_asset_file(TestAsset(), pfile,
                                         zorroa_test_data('images/set01/toucan.jpg'))


IMAGE_JPG = zorroa_test_data('images/set01/faces.jpg')
VIDEO_WEBM = zorroa_test_data('video/dc.webm')
VIDEO_MP4 = zorroa_test_data('video/sample_ipad.m4v')


class StorageFunctionTests(TestCase):

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

        path = storage.get_proxy_level(asset, 0)
        assert '1246524d107aa3b91ccb1ea43c136d366156d2b1' in path

        path = storage.get_proxy_level(asset, 9)
        assert '9bf44aa1e82ab54ce7adc212b3918c6047849c15' in path

    @patch.object(ZmlpClient, 'stream')
    def test_get_proxy_min_width(self, stream_patch):
        asset = TestAsset(IMAGE_JPG, id='123456')
        asset.set_attr('files', self.file_list)

        path = storage.get_proxy_min_width(asset, 300)
        assert '9bf44aa1e82ab54ce7adc212b3918c6047849c15' in path

        path = storage.get_proxy_min_width(asset, 350, mimetype='video/')
        assert '5bedc72da42dd3e296e14c26eaba01c1568c71d0' in path

        path = storage.get_proxy_min_width(asset, 1025, mimetype='image/', fallback=True)
        assert 'faces.jpg' in path

        with pytest.raises(ValueError):
            storage.get_proxy_min_width(asset, 1025, mimetype='video/', fallback=False)

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
        storage.add_proxy_file(asset, IMAGE_JPG, (200, 200))
        storage.add_proxy_file(asset, IMAGE_JPG, (200, 200))

        upload_patch.return_value = {
            'name': 'proxy_200x200.mp4',
            'category': 'proxy',
            'mimetype': 'video/mp4',
            'attrs': {
                'width': 200,
                'height': 200
            }
        }
        storage.add_proxy_file(asset, VIDEO_MP4, (200, 200))
        assert 2 == len(asset.get_files())
