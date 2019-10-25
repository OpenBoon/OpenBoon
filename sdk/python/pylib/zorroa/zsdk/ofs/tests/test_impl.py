import os
import tempfile
import uuid

from unittest import TestCase

from mock import patch
from pathlib2 import Path
from requests import Response

from zorroa.client import ZHttpClient
from zorroa.zsdk import Document
from zorroa.zsdk.ofs.impl import ObjectFileSystem, ObjectFile

cwd = os.path.dirname(__file__)


class TestObjectFileSystem(TestCase):

    @classmethod
    def setUpClass(cls):
        client = ZHttpClient("http://localhost:8080")
        cls.ofs = ObjectFileSystem(client)

    def setUp(self):
        self.document = Document({'id': '9f0f8a1d-4719-5cf8-b427-4612c5597811'})

    @patch.object(ZHttpClient, 'post')
    def test_prepare(self, post_patch):
        object_file_data = {'uri': 'gs://some/bucket/gritty.png',
                            'scheme': 'gs',
                            'exists': True,
                            'size': 2048,
                            'mimeType': 'image/png',
                            'id': 'proxy/sickest-mascots/gritty.png'}
        post_patch.return_value = object_file_data
        f = self.ofs.prepare("asset", self.document, "gritty.png")
        self.assertTrue(str(f), f.path)

    @patch.object(ZHttpClient, 'post')
    @patch.object(ZHttpClient, 'get')
    def test_get(self, get_patch, post_patch):
        object_file_data = {'uri': 'gs://some/bucket/gritty.png',
                            'scheme': 'gs',
                            'exists': True,
                            'size': 2048,
                            'mimeType': 'image/png',
                            'id': 'proxy/sickest-mascots/gritty.png'}
        post_patch.return_value = object_file_data
        get_patch.return_value = object_file_data
        f1 = self.ofs.prepare("asset", self.document, "gritty.png")
        f2 = self.ofs.get(f1.id)
        self.assertEquals(f1, f2)

    @patch.object(ZHttpClient, 'post')
    def test_prepare_with_variants(self, post_patch):
        object_file_data = {'uri': 'gs://some/bucket/gritty_640_480.png',
                            'scheme': 'gs',
                            'exists': True,
                            'size': 2048,
                            'mimeType': 'image/png',
                            'id': 'proxy/sickest-mascots/gritty_640_480.png'}
        post_patch.return_value = object_file_data
        f = self.ofs.prepare("asset", self.document, "proxy_640_480.png")
        self.assertTrue(f.path.endswith("_640_480.png"))
        self.assertTrue(str(f), f.path)

    @patch.object(ZHttpClient, 'post')
    @patch.object(ZHttpClient, 'get')
    def test_prepare_with_variants_2(self, get_patch, post_patch):
        object_file_data = {'uri': 'gs://some/bucket/gritty_proxy.png',
                            'scheme': 'gs',
                            'exists': True,
                            'size': 2048,
                            'mimeType': 'image/png',
                            'id': 'proxy/sickest-mascots/gritty_640_480.png'}
        post_patch.return_value = object_file_data
        get_patch.return_value = object_file_data
        f1 = self.ofs.prepare("asset", self.document, "proxy_640_480.png")
        f2 = self.ofs.get(f1.id)
        self.assertEquals(f1, f2)

    @patch.object(ZHttpClient, 'post')
    @patch.object(ZHttpClient, 'get')
    def test_get_by_id(self, get_patch, post_patch):
        object_file_data = {'uri': 'gs://some/bucket/gritty_proxy.png',
                            'scheme': 'gs',
                            'exists': True,
                            'size': 2048,
                            'mimeType': 'image/png',
                            'id': 'proxy/sickest-mascots/gritty_640_480.png'}
        post_patch.return_value = object_file_data
        get_patch.return_value = object_file_data
        f1 = self.ofs.prepare("asset", self.document, "proxy_640_480.png")
        f2 = self.ofs.get(f1.id)
        self.assertEquals(f1, f2)


class ObjectFileUnitTestCase(TestCase):
    @classmethod
    def setUpClass(cls):
        client = ZHttpClient("http://localhost:8080")
        cls.ofs = ObjectFileSystem(client)

    def setUp(self):
        super(ObjectFileUnitTestCase, self).setUp()
        data = {'uri': 'gs://some/bucket/gritty.jpg',
                'scheme': 'gs',
                'mediaType': 'image/jpg',
                'id': 'proxy/sickest-mascots/gritty.jpg'}
        self.gcs_object_file = ObjectFile(data, self.ofs)

    def delete_path(self, expected_path):
        try:
            if expected_path.exists():
                expected_path.unlink()
            assert not expected_path.exists()
        except AttributeError:
            if os.path.exists(expected_path):
                os.unlink(expected_path)
                assert not os.path.exists(expected_path)

    def test_mimetype(self):
        assert self.gcs_object_file.mediatype == 'image/jpg'

    @patch.object(ZHttpClient, 'get')
    def test_stat(self, get_patch):
        get_patch.return_value = {"size": 1111, "exists": True, "mediaType": "image/jpeg"}
        stat = self.gcs_object_file.stat()
        assert stat["size"] == 1111
        assert stat["exists"]
        assert stat["mediaType"] == "image/jpeg"

    @patch.object(ZHttpClient, 'get')
    def test_exists(self, get_patch):
        get_patch.return_value = {"size": 1111, "exists": True, "mediaType": "image/jpeg"}
        assert self.gcs_object_file.exists()

    @patch.object(ZHttpClient, 'get')
    def test_sync_local(self, get_patch):
        # Make sure that file to download does not exist already.
        expected_path = self.gcs_object_file.path
        self.delete_path(expected_path)

        get_patch.return_value = {
            'uri': 'https://storage.cloud.google.com/zorroa-dev-data/image/cat.gif'}
        path = self.gcs_object_file.sync_local()
        assert path == expected_path
        assert Path(path).exists()
        Path(path).unlink()

    @patch.object(ZHttpClient, 'get')
    def test_open_gcs_file(self, client_patch):
        # Make sure that file to download does not exist already.
        expected_path = self.gcs_object_file.path
        self.delete_path(expected_path)

        client_patch.return_value = {
            'uri': 'https://storage.cloud.google.com/zorroa-dev-data/image/cat.gif'}
        _file = self.gcs_object_file.open()
        path = Path(_file.name)
        assert path.exists()
        _file.close()
        assert path == Path(expected_path)
        path.unlink()

    @patch.object(ZHttpClient, 'get')
    @patch('requests.put')
    def test_store_gcs_file(self, post_patch, client_patch):
        client_patch.return_value = {
            'uri': 'https://storage.googleapis.com/zorroa-test-data/gritty.jpg',
        }
        response = Response()
        response.status_code = 200
        post_patch.return_value = response
        local_cache_path = self.gcs_object_file.path
        self.delete_path(local_cache_path)
        file_to_upload = Path('/tmp/ofs-test-file.txt')
        with file_to_upload.open('w') as _file:
            _file.write(u'Hey there!')
        self.gcs_object_file.store(str(file_to_upload))
        lcp = Path(local_cache_path)
        assert lcp.exists()
        assert lcp.read_text() == 'Hey there!'
        file_to_upload.unlink()
        lcp.unlink()

    def test_storage_type(self):
        # Check GS
        assert self.gcs_object_file.storage_type == "gs"

        # Check File
        data = {'uri': 'file://mr/gritty.jpg',
                'scheme': 'file',
                'mediaType': 'image/jpg',
                'id': 'proxy/sickest-mascots/gritty.jpg'}
        ofs_file = ObjectFile(data, self.ofs)
        assert ofs_file.storage_type == "file"

    def test_mkdirs(self):
        tmp_root = tempfile.gettempdir()
        asset_id = uuid.uuid4()
        data = {'uri': 'file:///%s/%s/gritty.jpg' % (tmp_root, asset_id),
                'scheme': 'file',
                'mediaType': 'image/jpg',
                'id': 'proxy/sickest-mascots/gritty.jpg'}
        ofs_file = ObjectFile(data, self.ofs)
        assert not os.path.exists(os.path.dirname(ofs_file.path))
        ofs_file.mkdirs()
        assert os.path.exists(os.path.dirname(ofs_file.path))
        # Make sure we don't throw
        ofs_file.mkdirs()
