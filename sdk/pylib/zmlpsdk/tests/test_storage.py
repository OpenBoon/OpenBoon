import logging
import os
from unittest import TestCase
from unittest.mock import patch

import pytest
import urllib3
from minio.api import Minio

import zmlp
from zmlp.asset import StoredFile
from zmlp.dataset import DataSet
from zmlp.client import ZmlpClient
from zmlpsdk import storage
from zmlpsdk.testing import zorroa_test_data, TestAsset

logging.basicConfig(level=logging.DEBUG)


class FileCacheTests(TestCase):

    def setUp(self):
        os.environ['ZMLP_STORAGE_PIPELINE_URL'] = 'http://localhost:9000'
        self.lfc = storage.FileCache(zmlp.app_from_env())

    def tearDown(self):
        self.lfc.clear()

    def test_init_with_task_id(self):
        os.environ['ZMLP_TASK_ID'] = '1234abcd5678'
        try:
            cache = storage.FileCache(zmlp.app_from_env())
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

    def test_get_path_with_project_env(self):
        os.environ['ZMLP_PROJECT_ID'] = 'abc123'
        try:
            path = self.lfc.get_path('spock', '.kirk')
            filename = 'c85be874d0f9c380a790f583c2bec6633109386e.kirk'
            assert path.endswith(filename)
        finally:
            del os.environ['ZMLP_PROJECT_ID']

    def test_clear(self):
        path = self.lfc.localize_uri('https://i.imgur.com/WkomVeG.jpg')
        assert os.path.exists(path)
        self.lfc.clear()
        assert not os.path.exists(path)

    def test_close(self):
        self.lfc.localize_uri('https://i.imgur.com/WkomVeG.jpg')
        self.lfc.close()

        with pytest.raises(FileNotFoundError):
            self.lfc.localize_uri('https://i.imgur.com/WkomVeG.jpg')

    @patch.object(ZmlpClient, 'stream')
    def test_precache_file(self, post_patch):
        pfile = StoredFile({
            'name': 'cat.jpg',
            'category': 'proxy',
            'attrs': {},
            'id': 'assets/123456/proxy/cat.jpg'
        })
        post_patch.return_value = '/tmp/toucan.jpg'
        bird = zorroa_test_data('images/set01/toucan.jpg', uri=False)
        path = self.lfc.precache_file(pfile, bird)
        assert os.path.getsize(path) == os.path.getsize(bird)


class FileStorageTests(TestCase):

    def setUp(self):
        os.environ['ZMLP_STORAGE_PIPELINE_URL'] = 'http://localhost:9000'
        self.fs = storage.FileStorage()

    @patch.object(ZmlpClient, 'stream')
    def test_localize_remote_file(self, post_patch):
        pfile = {
            'name': 'cat.jpg',
            'category': 'source',
            'attrs': {},
            'id': 'assets/123456/source/cat.jpg'
        }
        post_patch.return_value = '/tmp/cat.jpg'
        asset = TestAsset(id='123456')
        asset.set_attr('files', [pfile])
        path = self.fs.localize_file(asset)
        assert path.endswith('34d8c2639fcb5f54c50d6a211aaf63f04f679bfb.jpg')

    def test_localize_file_obj_with_uri(self):
        test_asset = TestAsset('https://i.imgur.com/WkomVeG.jpg')
        path = self.fs.localize_file(test_asset)
        assert os.path.exists(path)

    def test_localize_file_str(self):
        path = self.fs.localize_file('https://i.imgur.com/WkomVeG.jpg')
        assert os.path.exists(path)

    @patch.object(Minio, 'get_object')
    def test_localize_internal_uri(self, get_object_patch):
        http = urllib3.PoolManager()
        r = http.request('GET', 'http://i.imgur.com/WkomVeG.jpg', preload_content=False)

        get_object_patch.return_value = r
        path = self.fs.localize_file('zmlp://internal/officer/pdf/proxy.1.jpg')

        assert os.path.exists(path)
        assert os.path.getsize(path) == 267493


class TestAssetStorage(TestCase):

    def setUp(self):
        os.environ['ZMLP_STORAGE_PIPELINE_URL'] = 'http://localhost:9000'
        self.fs = storage.FileStorage()
        self.pfile_dict = {
            'name': 'cat.jpg',
            'category': 'source',
            'attrs': {},
            'id': 'assets/123456/source/cat.jpg'
        }

    def tearDown(self):
        self.fs.cache.clear()

    @patch.object(ZmlpClient, 'upload_file')
    def test_store_file(self, upload_patch):
        upload_patch.return_value = {
            'name': 'cat.jpg',
            'category': 'proxy',
            'attrs': {},
            'id': 'assets/123456/proxy/cat.jpg'
        }
        asset = TestAsset(id='123456')
        result = self.fs.assets.store_file(
            zorroa_test_data('images/set01/toucan.jpg', uri=False), asset, 'test')
        assert 'cat.jpg' == result.name
        assert 'proxy' == result.category

    @patch.object(ZmlpClient, 'upload_file')
    def test_store_blob(self, upload_patch):
        upload_patch.return_value = {
            'name': 'vid-int-moderation.json',
            'category': 'google',
            'attrs': {},
            'id': 'assets/123456/google/vid-int-moderation.json',
        }
        asset = TestAsset(id='123456')
        result = self.fs.assets.store_blob(
            b'{"jo": "boo"}', asset, 'google', 'vid-int-moderation.json')
        assert 'google' == result.category
        assert 'vid-int-moderation.json' == result.name

    @patch.object(ZmlpClient, 'upload_file')
    def test_store_blob_no_ext(self, upload_patch):
        upload_patch.return_value = StoredFile({
            'name': 'vid-int-moderation.json',
            'category': 'google',
            'attrs': {},
            'id': 'assets/123456/google/vid-int-moderation.json',
        })
        asset = TestAsset(id='123456')
        with pytest.raises(ValueError):
            self.fs.assets.store_blob(
                asset, '{"jo": "boo"}', 'google', 'vid-int-moderation')

    @patch.object(ZmlpClient, 'get')
    def test_get_native_uri(self, get_patch):
        get_patch.return_value = {'uri': 'gs://hulk-hogan'}
        pfile = StoredFile({
            'name': 'cat.jpg',
            'category': 'proxy',
            'attrs': {},
            'id': 'assets/123456/proxy/cat.jpg'
        })
        uri = self.fs.assets.get_native_uri(pfile)
        assert 'gs://hulk-hogan' == uri


class TestProjectStorage(TestCase):

    def setUp(self):
        os.environ['ZMLP_STORAGE_PIPELINE_URL'] = 'http://localhost:9000'
        self.fs = storage.FileStorage()

    def tearDown(self):
        self.fs.cache.clear()

    @patch.object(ZmlpClient, 'upload_file')
    def test_store_file_with_rename(self, upload_patch):
        upload_patch.return_value = {
            'id': 'datasets/12345/face_model/celebs.dat',
            'name': 'celebs.dat',
            'category': 'face_model',
            'entity': 'assets'
        }
        path = os.path.dirname(__file__) + '/fake_model.dat'
        ds = DataSet({"id": "12345"})
        result = self.fs.projects.store_file(
            path, ds, 'face_model', rename='celebs.dat')
        assert 'celebs.dat' == result.name
        assert 'face_model' == result.category

    @patch.object(ZmlpClient, 'upload_file')
    def test_store_file(self, upload_patch):
        upload_patch.return_value = {
            'id': "asset/foo/fake/fake_model.dat",
            'name': 'fake_model.dat',
            'category': 'fake'
        }
        ds = DataSet({"id": "12345"})
        path = os.path.dirname(__file__) + '/fake_model.dat'
        result = self.fs.projects.store_file(path, ds, 'model', 'fake_model.dat')
        assert 'fake_model.dat' == result.name
        assert 'fake' == result.category

    @patch.object(ZmlpClient, 'stream')
    def test_localize_file(self, post_patch):
        post_patch.return_value = '/tmp/toucan.jpg'
        pfile = StoredFile({
            'name': 'cat.jpg',
            'category': 'proxy',
            'attrs': {},
            'id': 'assets/123456/proxy/cat.jpg'
        })
        path = self.fs.localize_file(pfile)
        assert path.endswith('b9430537beae3fe8e6ba2e11667f0ccc9be82a28.jpg')

    @patch.object(ZmlpClient, 'stream')
    def test_localize_asset_file_with_asset_override(self, post_patch):
        pfile = StoredFile({
            'name': 'cat.jpg',
            'category': 'proxy',
            'attrs': {},
            'id': 'assets/123456/proxy/cat.jpg'
        })
        post_patch.return_value = '/tmp/cat.jpg'
        self.fs.projects.localize_file(pfile)
        assert 'assets/123456/proxy/cat.jpg' in post_patch.call_args_list[0][0][0]
