import logging
import os
from unittest import TestCase
from unittest.mock import patch

import pytest
import urllib3
from minio.api import Minio

from zmlp.analysis import storage
from zmlp.analysis.testing import zorroa_test_data, TestAsset
from zmlp.client import ZmlpClient

logging.basicConfig(level=logging.DEBUG)


class LocalFileCacheTests(TestCase):

    def setUp(self):
        os.environ['ZMLP_PIPELINE_STORAGE_URL'] = "http://localhost:9000"
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

    def test_localize_uri_http(self):
        path = self.lfc.localize_uri('https://i.imgur.com/WkomVeG.jpg')
        assert os.path.exists(path)
        assert os.path.getsize(path) == 267493

    def test_localize_uri_gs(self):
        path = self.lfc.localize_uri('gs://zorroa-dev-data/image/pluto_2.1.jpg')
        assert os.path.exists(path)
        assert os.path.getsize(path) == 65649

    def test_localize_uri_local_path(self):
        local_file = zorroa_test_data('images/set01/toucan.jpg', uri=False)
        path = self.lfc.localize_uri(local_file)
        assert os.path.exists(path)
        assert os.path.getsize(path) == 97221

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
    def test_localize_remote_file(self, post_patch):
        pfile = {
            'name': 'cat.jpg',
            'category': 'source'
        }
        post_patch.return_value = '/tmp/cat.jpg'
        asset = TestAsset(id='123456')
        asset.set_attr('files', [pfile])
        path = self.lfc.localize_remote_file(asset)
        assert path.endswith('3c25baa7cf0b59d64c0179a1e0030072444eac3b.jpg')

    def test_localize_file_obj_with_uri(self):
        test_asset = TestAsset('https://i.imgur.com/WkomVeG.jpg')
        path = self.lfc.localize_remote_file(test_asset)
        assert os.path.exists(path)

    def test_localize_file_str(self):
        path = self.lfc.localize_remote_file('https://i.imgur.com/WkomVeG.jpg')
        assert os.path.exists(path)

    @patch.object(Minio, 'get_object')
    def test_localize_internal_uri(self, get_object_patch):
        http = urllib3.PoolManager()
        r = http.request('GET', 'http://i.imgur.com/WkomVeG.jpg', preload_content=False)

        get_object_patch.return_value = r
        path = self.lfc.localize_remote_file('zmlp://internal/officer/pdf/proxy.1.jpg')

        assert os.path.exists(path)
        assert os.path.getsize(path) == 267493

    def test_close(self):
        pfile = {
            'name': 'cat.jpg',
            'category': 'proxy'
        }
        self.lfc.assets.localize_file(TestAsset(), pfile,
                                      zorroa_test_data('images/set01/toucan.jpg'))
        self.lfc.close()

        with pytest.raises(FileNotFoundError):
            self.lfc.assets.localize_file(TestAsset(), pfile,
                                          zorroa_test_data('images/set01/toucan.jpg'))


class TestAssetStorage(TestCase):

    def setUp(self):
        os.environ['ZMLP_PIPELINE_STORAGE_URL'] = "http://localhost:9000"
        self.lfc = storage.LocalFileCache()

    def tearDown(self):
        self.lfc.clear()

    @patch.object(ZmlpClient, 'upload_file')
    def test_store_file(self, upload_patch):
        upload_patch.return_value = {
            'name': 'cat.jpg',
            'category': 'proxy'
        }
        asset = TestAsset(id='123456')
        result = self.lfc.assets.store_file(
            asset, zorroa_test_data('images/set01/toucan.jpg', uri=False), 'test')
        assert 'cat.jpg' == result['name']
        assert 'proxy' == result['category']

    @patch.object(ZmlpClient, 'stream')
    def test_localize_file_with_copy(self, post_patch):
        pfile = {
            'name': 'cat.jpg',
            'category': 'proxy'
        }
        post_patch.return_value = '/tmp/toucan.jpg'
        bird = zorroa_test_data('images/set01/toucan.jpg', uri=False)
        path = self.lfc.assets.localize_file(TestAsset(id='123456'), pfile, bird)
        assert os.path.getsize(path) == os.path.getsize(bird)


    @patch.object(ZmlpClient, 'stream')
    def test_localize_file(self, post_patch):
        post_patch.return_value = '/tmp/toucan.jpg'
        pfile = {
            'name': 'cat.jpg',
            'category': 'proxy'
        }
        path = self.lfc.assets.localize_file(TestAsset(id='123456'), pfile)
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
        self.lfc.assets.localize_file(asset, pfile)
        assert "assets/bingo/files" in post_patch.call_args_list[0][0][0]


class TestProjectStorage(TestCase):

    def setUp(self):
        os.environ['ZMLP_PIPELINE_STORAGE_URL'] = "http://localhost:9000"
        self.lfc = storage.LocalFileCache()

    def tearDown(self):
        self.lfc.clear()

    @patch.object(ZmlpClient, 'upload_file')
    def test_store_file_with_rename(self, upload_patch):
        upload_patch.return_value = {
            'name': 'celebs.dat',
            'category': 'face_model',
            'entity': 'model'
        }
        path = os.path.dirname(__file__) + "/fake_model.dat"
        asset = TestAsset(id='123456')
        result = self.lfc.projects.store_file(path, "model", "face_model", "celebs.dat")
        assert 'celebs.dat' == result['name']
        assert 'face_model' == result['category']

    @patch.object(ZmlpClient, 'upload_file')
    def test_store_file(self, upload_patch):
        upload_patch.return_value = {
            'name': 'fake_model.dat',
            'category': 'fake'
        }
        path = os.path.dirname(__file__) + '/fake_model.dat'
        result = self.lfc.projects.store_file(path, 'model', 'fake', 'fake_model.dat')
        assert 'fake_model.dat' == result['name']
        assert 'fake' == result['category']

