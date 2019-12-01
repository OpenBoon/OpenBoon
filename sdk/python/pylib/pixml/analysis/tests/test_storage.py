import os
from unittest import TestCase
from unittest.mock import patch

import pytest

from pixml.analysis import storage
from pixml.analysis.testing import zorroa_test_data, TestAsset
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
    def test_localize_pixml_file(self, post_patch):
        pfile = {
            "name": "cat.jpg",
            "category": "proxy",
            "assetId": "123456"
        }
        post_patch.return_value = "/tmp/cat.jpg"
        path = self.lfc.localize_pixml_file(pfile)
        assert path.endswith("c7bc251d55d2cfb3f5b0c86d739877583556f890.jpg")

    @patch.object(PixmlClient, 'stream')
    def test_localize_pixml_file_with_copy(self, post_patch):
        pfile = {
            "name": "cat.jpg",
            "category": "proxy",
            "assetId": "123456"
        }
        post_patch.return_value = "/tmp/toucan.jpg"
        bird = zorroa_test_data("images/set01/toucan.jpg", uri=False)
        path = self.lfc.localize_pixml_file(pfile, bird)
        assert os.path.getsize(path) == os.path.getsize(bird)

    def test_localize_file_obj_with_uri(self):
        test_asset = TestAsset("https://i.imgur.com/WkomVeG.jpg")
        path = self.lfc.localize_remote_file(test_asset)
        assert os.path.exists(path)

    def test_localize_file_str(self):
        path = self.lfc.localize_remote_file("https://i.imgur.com/WkomVeG.jpg")
        assert os.path.exists(path)

    @patch.object(PixmlClient, 'stream')
    def test_localize_file_pixml_file_dict(self, post_patch):
        post_patch.return_value = "/tmp/toucan.jpg"
        pfile = {
            "name": "cat.jpg",
            "category": "proxy",
            "assetId": "123456"
        }
        path = self.lfc.localize_pixml_file(pfile)
        assert path.endswith("c7bc251d55d2cfb3f5b0c86d739877583556f890.jpg")

    def test_close(self):
        pfile = {
            "name": "cat.jpg",
            "category": "proxy",
            "assetId": "123456"
        }
        self.lfc.localize_pixml_file(pfile, zorroa_test_data("images/set01/toucan.jpg"))
        self.lfc.close()

        with pytest.raises(FileNotFoundError):
            self.lfc.localize_pixml_file(pfile, zorroa_test_data("images/set01/toucan.jpg"))


IMAGE_JPG = zorroa_test_data('images/set01/faces.jpg')
VIDEO_WEBM = zorroa_test_data('video/dc.webm')
VIDEO_MP4 = zorroa_test_data('video/sample_ipad.m4v')


class StorageFunctionTests(TestCase):

    @patch.object(PixmlClient, 'stream')
    def test__get_proxy_file(self, stream_patch):
        asset = TestAsset(IMAGE_JPG)
        asset.set_attr("files", [
            {
                "name": "proxy_200x200.jpg",
                "category": "proxy",
                "assetId": "12345",
                "mimetype": "image/jpeg",
                "attrs": {
                    "width": 200,
                    "height": 200
                }
            },
            {
                "name": "proxy_400x400.jpg",
                "category": "proxy",
                "assetId": "12345",
                "mimetype": "image/jpeg",
                "attrs": {
                    "width": 400,
                    "height": 400
                }
            },
            {
                "name": "proxy_400x400.mp4",
                "category": "proxy",
                "assetId": "12345",
                "mimetype": "video/mp4",
                "attrs": {
                    "width": 400,
                    "height": 400
                }
            },
            {
                "name": "proxy_500x500.mp4",
                "category": "proxy",
                "assetId": "12345",
                "mimetype": "video/mp4",
                "attrs": {
                    "width": 500,
                    "height": 500
                }
            }
        ])

        name, _ = storage.get_proxy_file(asset, min_width=300)
        assert name == "proxy_400x400.jpg"

        name, _ = storage.get_proxy_file(asset, mimetype="video/", min_width=350)
        assert name == "proxy_400x400.mp4"

        name, _ = storage.get_proxy_file(asset, mimetype="image/", min_width=1025, fallback=True)
        assert name == "source"

        with pytest.raises(ValueError):
            storage.get_proxy_file(asset, mimetype="video/", min_width=1025, fallback=False)

    @patch.object(PixmlClient, 'upload_file')
    def test_add_proxy_file(self, upload_patch):
        asset = TestAsset(IMAGE_JPG)
        upload_patch.return_value = {
            "name": "proxy_200x200.jpg",
            "category": "proxy",
            "assetId": "12345",
            "mimetype": "image/jpeg",
            "attrs": {
                "width": 200,
                "height": 200
            }
        }
        ## Should only be added to list once.
        storage.add_proxy_file(asset, IMAGE_JPG, (200, 200))
        storage.add_proxy_file(asset, IMAGE_JPG, (200, 200))

        upload_patch.return_value = {
            "name": "proxy_200x200.mp4",
            "category": "proxy",
            "assetId": "12345",
            "mimetype": "video/mp4",
            "attrs": {
                "width": 200,
                "height": 200
            }
        }
        storage.add_proxy_file(asset, VIDEO_MP4, (200, 200))
        assert 2 == len(asset.get_files())

