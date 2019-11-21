import os

from unittest import TestCase
from unittest.mock import patch

from pixml.analysis import storage
from pixml.analysis.testing import zorroa_test_data
from pixml.rest import PixmlClient


class LocalFileCacheTests(TestCase):

    def setUp(self):
        self.lfc = storage.LocalFileCache()

    def tearDown(self):
        self.lfc.clear()

    def test_localize_http(self):
        path = self.lfc.localize_uri("https://i.imgur.com/WkomVeG.jpg")
        assert os.path.exists(path)
        assert os.path.getsize(path) == 267493

    def test_localize_gs(self):
        path = self.lfc.localize_uri("gs://zorroa-dev-data/image/pluto_2.1.jpg")
        assert os.path.exists(path)
        assert os.path.getsize(path) == 65649

    def test_get_path(self):
        path = self.lfc.get_path("spock", ".kirk")
        filename = "1a569625e9949f82ab1be5257ab2cab1f7524c6d.kirk"
        assert path.endswith(filename)

    def test_clear(self):
        path = self.lfc.localize_uri("https://i.imgur.com/WkomVeG.jpg")
        assert os.path.exists(path)
        self.lfc.clear()
        assert not os.path.exists(path)

    @patch.object(PixmlClient, 'stream')
    def test_localize_file_storage(self, post_patch):
        storage = {
            "name": "cat.jpg",
            "category": "proxy",
            "assetId": "123456"
        }
        post_patch.return_value = "/tmp/cat.jpg"
        path = self.lfc.localize_file_storage(storage)
        assert path.endswith("c7bc251d55d2cfb3f5b0c86d739877583556f890.jpg")

    @patch.object(PixmlClient, 'stream')
    def test_localize_file_storage_with_copy(self, post_patch):
        storage = {
            "name": "cat.jpg",
            "category": "proxy",
            "assetId": "123456"
        }
        post_patch.return_value = "/tmp/toucan.jpg"
        bird = zorroa_test_data("images/set01/toucan.jpg")
        path = self.lfc.localize_file_storage(storage, zorroa_test_data("images/set01/toucan.jpg"))
        assert os.path.getsize(path) == os.path.getsize(bird)

