import os

import pytest
from unittest.mock import patch

from pixml.analysis import Frame
from pixml.analysis.testing import TestAsset, PluginUnitTestCase, zorroa_test_data
from pixml.rest import PixmlClient

from pixml_core.proxy.processors import ProxyProcessor, get_tiny_proxy_colors

TOUCAN_PATH = zorroa_test_data("images/set01/toucan.jpg", uri=False)
TOUCAN = zorroa_test_data("images/set01/toucan.jpg")
BEER = zorroa_test_data("images/set02/beer_kettle_01.jpg")
VIDEO = zorroa_test_data("video/dc.webm")


class ProxyIngestorUnitTestCase(PluginUnitTestCase):
    def setUp(self):
        self.source_path = TOUCAN
        self.frame = Frame(TestAsset(self.source_path))
        self.frame.asset.set_attr("media.type", "image")
        self.frame.asset.set_attr('media.width', 512)
        self.frame.asset.set_attr('media.height', 341)
        self.frame.asset.set_attr('tmp.proxy_source_image', TOUCAN_PATH)
        self.processor = self.init_processor(ProxyProcessor(), {})

        self.storage_patch = {
            "name": "proxy_512x341.jpg",
            "category": "proxy",
            "mimetype": "image/jpeg",
            "assetId": "12345",
            "attrs": {
                "width": 512,
                "height": 341
            }
        }

    def test_init(self):
        with pytest.raises(ValueError) as error:
            self.init_processor(ProxyProcessor(), {'file_type': 'butts'})
        assert '"butts" is not a valid type' in error.value.args[0]

    @patch.object(PixmlClient, 'upload_file')
    def test_process(self, post_patch):
        post_patch.return_value = self.storage_patch
        self.processor.process(self.frame)
        assert len(self.frame.asset.get_attr('analysis.pixelml.tinyProxy')) == 9
        assert len(self.frame.asset.get_attr('files')) == 2

    @patch.object(PixmlClient, 'upload_file')
    def test_skip_existing(self, post_patch):
        post_patch.return_value = self.storage_patch
        frame = Frame(TestAsset(self.source_path))
        frame.asset.set_attr('media.width', 1024)
        frame.asset.set_attr('media.height', 768)
        frame.asset.set_attr('tmp.proxy_source_image', TOUCAN_PATH)

        processor = self.init_processor(ProxyProcessor(), {"sizes": [384]})
        processor.process(frame)
        assert len(frame.asset.get_attr("files")) == 1

        processor = self.init_processor(ProxyProcessor(), {"sizes": [512, 384]})
        processor.process(frame)
        assert len(frame.asset.get_attr("files")) == 2

        assert processor.created_proxy_count == 1

    @patch.object(ProxyProcessor, '_create_proxy_images')
    def test_no_process(self, proxy_patch):
        self.frame.asset.set_attr('proxies', True)
        assert proxy_patch.call_count == 0

    def test_create_proxy_images(self):
        self.maxDiff = None
        proxies = self.processor._create_proxy_images(self.frame.asset)
        assert len(proxies) == 2

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
        assert not frame.asset.get_attr("proxies.proxies")

    @patch.object(PixmlClient, 'upload_file')
    def test_process_asset_without_media_namespace(self, post_patch):
        post_patch.return_value = self.storage_patch
        self.frame.asset.set_attr('media', {})
        self.processor.process(self.frame)

        assert len(self.frame.asset.get_attr('analysis.pixelml.tinyProxy')) == 9
        assert len(self.frame.asset.get_attr('files')) == 2

    def test_get_tiny_proxy_colors(self):
        colors = get_tiny_proxy_colors(TOUCAN_PATH)
        assert len(colors) == 9

    def test_get_valid_sizes(self):
        assert self.processor._get_valid_sizes(800, 600) == [512, 256]
        assert self.processor._get_valid_sizes(100, 50) == [100]
