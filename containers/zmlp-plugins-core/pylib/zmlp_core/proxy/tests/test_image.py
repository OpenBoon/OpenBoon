import logging
from unittest.mock import patch

from zmlp import StoredFile
from zmlp_core.proxy.image import ImageProxyProcessor
from zmlpsdk import Frame
from zmlpsdk.storage import ProjectStorage
from zmlpsdk.testing import TestAsset, PluginUnitTestCase, zorroa_test_data

TOUCAN_PATH = zorroa_test_data("images/set01/toucan.jpg", uri=False)
TOUCAN = zorroa_test_data("images/set01/toucan.jpg")
BEER = zorroa_test_data("images/set02/beer_kettle_01.jpg")
VIDEO = zorroa_test_data("video/dc.webm")
TOUCAN = zorroa_test_data("images/set01/toucan.jpg")
TIFF = zorroa_test_data("office/multipage_tiff_small.tif")
EXR = zorroa_test_data("images/set06/grid-overscan.exr")

logging.basicConfig(level=logging.DEBUG)


class ProxyIngestorUnitTestCase(PluginUnitTestCase):
    def setUp(self):
        super(ProxyIngestorUnitTestCase, self).setUp()
        self.source_path = TOUCAN
        self.frame = Frame(TestAsset(self.source_path))
        self.frame.asset.set_attr("media.type", "image")
        self.frame.asset.set_attr('media.width', 512)
        self.frame.asset.set_attr('media.height', 341)
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

    @patch.object(ProjectStorage, 'store_file')
    def test_process_multi_page(self, store_patch):
        self.frame.asset.set_attr("clip.length", 5)
        self.frame.asset.set_attr("clip.start", 1)
        self.frame.asset.set_attr("source.path", TIFF)
        self.frame.asset.set_attr("media.length", 3)
        self.frame.asset.set_attr('media.width', 3264)
        self.frame.asset.set_attr('media.height', 2448)
        store_patch.side_effect = [StoredFile(self.storage_patch1),
                                   StoredFile(self.storage_patch2),
                                   StoredFile(self.storage_patch3)]
        self.processor.process(self.frame)
        assert len(self.frame.asset.get_attr('files')) == 3

    @patch.object(ProjectStorage, 'store_file')
    def test_process_hdr(self, store_patch):
        self.frame.asset.set_attr("clip.length", 5)
        self.frame.asset.set_attr("clip.start", 1)
        self.frame.asset.set_attr("source.path", EXR)
        self.frame.asset.set_attr("media.length", 3)
        self.frame.asset.set_attr('media.width', 3264)
        self.frame.asset.set_attr('media.height', 2448)
        store_patch.side_effect = [StoredFile(self.storage_patch1),
                                   StoredFile(self.storage_patch2),
                                   StoredFile(self.storage_patch3)]
        self.processor.process(self.frame)
        assert len(self.frame.asset.get_attr('files')) == 3

    @patch.object(ProjectStorage, 'store_file')
    def test_process_large(self, store_patch):
        self.frame.asset.set_attr("source.path", BEER)
        self.frame.asset.set_attr('media.width', 3264)
        self.frame.asset.set_attr('media.height', 2448)
        store_patch.side_effect = [StoredFile(self.storage_patch1),
                                   StoredFile(self.storage_patch2),
                                   StoredFile(self.storage_patch3)]
        self.processor.process(self.frame)
        assert len(self.frame.asset.get_attr('files')) == 3

    @patch.object(ProjectStorage, 'store_file')
    def test_process_small(self, post_patch):
        post_patch.side_effect = [
            StoredFile(self.storage_patch1),
            StoredFile(self.storage_patch2),
            StoredFile(self.storage_patch3)]
        self.processor.process(self.frame)
        assert len(self.frame.asset.get_attr('files')) == 2

    @patch.object(ProjectStorage, 'store_file')
    def test_process_super_small(self, post_patch):
        self.frame.asset.set_attr('media.width', 256)
        self.frame.asset.set_attr('media.height', 128)
        post_patch.side_effect = [
            StoredFile(self.storage_patch1),
            StoredFile(self.storage_patch2),
            StoredFile(self.storage_patch3)]
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

    def test_get_valid_sizes(self):
        assert self.processor._get_valid_sizes(800, 600, [1280, 512]) == [800, 512]
        assert self.processor._get_valid_sizes(100, 50, [1280, 512]) == [100]
