import logging
from unittest.mock import patch

import pytest

from zmlp import ZmlpClient, StoredFile
from zmlp_core.proxy.image import ImageProxyProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import TestAsset, PluginUnitTestCase, zorroa_test_data

TOUCAN_PATH = zorroa_test_data("images/set01/toucan.jpg", uri=False)
TOUCAN = zorroa_test_data("images/set01/toucan.jpg")
BEER = zorroa_test_data("images/set02/beer_kettle_01.jpg")
VIDEO = zorroa_test_data("video/dc.webm")

logging.basicConfig()


class ProxyIngestorUnitTestCase(PluginUnitTestCase):
    def setUp(self):
        self.source_path = TOUCAN
        self.frame = Frame(TestAsset(self.source_path))
        self.frame.asset.set_attr("media.type", "image")
        self.frame.asset.set_attr('media.width', 512)
        self.frame.asset.set_attr('media.height', 341)
        self.frame.asset.set_attr('tmp.proxy_source_image', TOUCAN)
        self.processor = self.init_processor(ImageProxyProcessor(), {})

        self.storage_patch1 = {
            "id": "foo/bar/proxy/proxy_512x341.jpg",
            "name": "proxy_512x341.jpg",
            "category": "proxy",
            "mimetype": "image/jpeg",
            "assetId": "12345",
            "attrs": {
                "width": 512,
                "height": 341
            }
        }

        self.storage_patch2 = self.storage_patch1.copy()
        self.storage_patch2["id"] = "foo/bar/proxy/proxy_256x256.jpg"
        self.storage_patch3 = self.storage_patch1.copy()
        self.storage_patch3["id"] = "foo/bar/web-proxy/web-proxy.jpg"

    def test_init(self):
        with pytest.raises(ValueError) as error:
            self.init_processor(ImageProxyProcessor(), {'file_type': 'butts'})
        assert '"butts" is not a valid type' in error.value.args[0]

    @patch.object(ZmlpClient, 'upload_file')
    def test_process_large(self, post_patch):
        self.frame.asset.set_attr("source.path", BEER)
        self.frame.asset.set_attr('media.width', 3264)
        self.frame.asset.set_attr('media.height', 2448)
        post_patch.side_effect = [self.storage_patch1, self.storage_patch2, self.storage_patch3]
        self.processor.process(self.frame)
        assert len(self.frame.asset.get_attr('files')) == 3

    @patch.object(ZmlpClient, 'upload_file')
    def test_process_small(self, post_patch):
        post_patch.side_effect = [self.storage_patch1, self.storage_patch2, self.storage_patch3]
        self.processor.process(self.frame)
        assert len(self.frame.asset.get_attr('files')) == 2

    @patch.object(ImageProxyProcessor, '_create_proxy_images')
    def test_no_process(self, proxy_patch):
        self.frame.asset.set_attr('proxies', True)
        assert proxy_patch.call_count == 0

    def test_create_proxy_images(self):
        self.maxDiff = None
        proxies = self.processor._create_proxy_images(self.frame.asset)
        assert len(proxies) == 1

    def test_get_source_path(self):
        # Test correct field is in metadata.
        # This will return a temp source path set in setUp
        path = self.processor._get_source_path(self.frame.asset)
        assert "file://" + path == self.source_path

        # This will attempt to localize a file:/// uri and simple
        # return the path portion of the uri.
        frame = Frame(TestAsset(self.source_path))
        frame.asset.set_attr("media.type", "image")
        path = self.processor._get_source_path(frame.asset)

        assert "file://" + path == self.source_path

    def test_get_source_path_failure(self):
        # Videos can't be processed, so there is no valid source path.
        frame = Frame(TestAsset(VIDEO))
        path = self.processor._get_source_path(frame.asset)
        assert not path

    def test_process_skip_video_with_no_proxy_source(self):
        # Videos can't be processed, so there is no valid source path.
        frame = Frame(TestAsset(VIDEO))
        self.processor.process(frame)
        assert not frame.asset.get_attr("files")

    @patch.object(ZmlpClient, 'upload_file')
    def test_process_asset_without_media_namespace(self, post_patch):
        post_patch.side_effect = [self.storage_patch1, self.storage_patch2, self.storage_patch3]
        self.frame.asset.set_attr('media', {})
        self.processor.process(self.frame)

        assert len(self.frame.asset.get_attr('files')) == 2

    @patch.object(ZmlpClient, 'upload_file')
    def test_create_web_optimized_proxy(self, post_patch):
        post_patch.return_value = self.storage_patch1
        prx = self.processor.make_web_optimized_proxy(self.frame.asset,
                                                      TOUCAN_PATH, (100, 100))
        assert StoredFile(self.storage_patch1) == prx

    def test_get_valid_sizes(self):
        assert self.processor._get_valid_sizes(800, 600) == [800, 512]
        assert self.processor._get_valid_sizes(100, 50) == [100]
