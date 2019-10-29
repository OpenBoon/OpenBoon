#!/usr/bin/env python

import unittest

import os
import zorroa.zsdk as zsdk
import pytest

from pathlib2 import Path
from zorroa.zsdk import Clip
from zorroa.zsdk.testing import PluginUnitTestCase, zorroa_test_data


class AssetUnitTests(PluginUnitTestCase):

    def test_set_source(self):
        asset = zsdk.Asset("/tmp/foo(#).jpg")
        self.assertEquals("jpg", asset.get_attr("source.extension"))
        self.assertEquals("foo(#)", asset.get_attr("source.basename"))
        self.assertEquals("foo(#).jpg", asset.get_attr("source.filename"))
        self.assertEquals("/tmp", asset.get_attr("source.directory"))
        self.assertEquals("/tmp/foo(#).jpg", asset.get_attr("source.path"))

    def test_is_clip(self):
        asset = zsdk.Asset(zorroa_test_data("images/set01/faces.jpg"))
        clip = Clip(parent='1', type='proxy', start=1, stop=1)
        assert asset.is_clip() is False
        asset.set_attr('media.clip', clip)
        assert asset.is_clip() is True

    def test_source_path(self):
        source_path = zorroa_test_data("images/set01/faces.jpg")
        asset = zsdk.Asset(source_path)
        assert asset.source_path == source_path

    def test_create_local_cache_path(self):
        source_path = zorroa_test_data("images/set01/faces.jpg")
        asset = zsdk.Asset(source_path)
        local_path = Path(asset._Asset__source_handler._create_local_cache_path())
        assert local_path.suffix == '.jpg'
        assert local_path.parent.exists()

    def test_asset_init(self):
        s = zsdk.Asset(zorroa_test_data("images/set01/faces.jpg"))
        self.assertEquals(zorroa_test_data("images/set01/faces.jpg"), s.get_attr("source.path"))
        self.assertEquals("image/jpeg", s.get_attr("source.mediaType"))
        self.assertEquals("image", s.get_attr("source.type"))
        self.assertEquals("jpeg", s.get_attr("source.subType"))

    def test_asset_multiple_periods(self):
        s = zsdk.Asset(zorroa_test_data("images/set01/faces.0001.jpg"))
        self.assertEquals("faces.0001", s.get_attr("source.basename"))

    @unittest.skip('Not valid until after GCS refactor.')
    def test_gs_asset_init(self):
        path = 'gs://zorroa-test-data/gritty.jpg'
        asset = zsdk.Asset(path)
        source = asset.get_attr('source')
        assert source == {'path': path,
                          'directory': 'gs://zorroa-test-data',
                          'filename': 'gritty.jpg',
                          'timeCreated': source.get('timeCreated'),
                          'basename': 'gritty',
                          'extension': 'jpg',
                          'mediaType': 'image/jpeg',
                          'type': 'image',
                          'subType': 'jpeg',
                          'fileSize': 133503,
                          'exists': True}

    @unittest.skip('Not valid until after GCS refactor.')
    def test_gs_asset_does_not_exist(self):
        path = 'gs://zorroa-test-data/no-gritty.jpg'
        asset = zsdk.Asset(path)
        source = asset.get_attr('source')
        assert source == {'path': path,
                          'directory': 'gs://zorroa-test-data',
                          'filename': 'no-gritty.jpg',
                          'timeCreated': source.get('timeCreated'),
                          'basename': 'no-gritty',
                          'extension': 'jpg',
                          'mediaType': 'image/jpeg',
                          'type': 'image',
                          'subType': 'jpeg',
                          'fileSize': 0,
                          'exists': False}

    @unittest.skip('Not valid until after GCS refactor.')
    def test_local_source_path(self):
        # Verify http paths that are not cached on asset metadata.
        path = 'gs://zorroa-test-data/gritty.jpg'
        asset = zsdk.Asset(path)
        local_path = Path(asset.get_local_source_path())
        assert local_path.exists()

        # Verify http paths that are cached on asset metadata.
        local_path = Path(asset.get_local_source_path())
        assert local_path.exists()
        local_path.unlink()

        # Verify local paths.
        path = '/not/a/real/path.txt'
        asset = zsdk.Asset(path)
        local_path = asset.get_local_source_path()
        assert path == local_path

    @unittest.skip('Not valid until after GCS refactor.')
    def test_open_source(self):
        path = 'gs://zorroa-test-data/gritty.jpg'
        asset = zsdk.Asset(path)
        _file = asset.open_source()
        assert Path(_file.name).name == 'gritty.jpg'
        Path(asset.get_local_source_path()).unlink()

    def test_bad_scheme(self):
        path = 'blarg://zorroa-test-data/gritty.jpg'
        with pytest.raises(TypeError):
            zsdk.Asset(path)

    def test_from_document(self):
        data = {"document": {"irm": {"companyId": 25274}, "source": {
                    "path": zorroa_test_data("/images/set01/faces.jpg")}},
                "id": "e3573b90-a18b-4e13-8efc-ae9cd7fba143",
                "replace": False}
        document = zsdk.Document(data)
        asset = zsdk.Asset.from_document(document)
        assert asset.id == document.id
        assert asset.replace is False
        assert asset.get_attr('irm.companyId') == 25274

    def test_get_thumbnail_path_proxy(self):
        toucan = zorroa_test_data('images/set01/toucan.jpg')
        print(toucan)
        asset = zsdk.Asset(toucan)
        ofile = self.ofs.prepare("asset", asset.id, "gritty.jpg")
        with open(toucan, "rb") as fp:
            ofile.store(fp)

        asset.set_attr("proxies.proxies", [{
            'mimetype': 'image/jpeg',
            'width': 1920,
            'id': ofile.id,
            'height': 1080,
            'height': 1080
        }])

        path = asset.get_thumbnail_path()
        assert os.path.exists(path)
        assert "gritty.jpg" in path

    def test_get_thumbnail_path_source(self):
        toucan = zorroa_test_data('images/set01/toucan.jpg')
        asset = zsdk.Asset(toucan)
        path = asset.get_thumbnail_path()
        assert path == toucan

    def test_set_resolution(self):
        asset = zsdk.Asset()
        asset.set_resolution(1920, 1080)
        expected_media = {'width': 1920,
                          'height': 1080,
                          'orientation': 'landscape',
                          'aspect': 1.78}
        assert asset.get_attr('media') == expected_media

        # Check square orientation.
        asset.set_resolution(1080, 1080)
        assert asset.get_attr('media.orientation') == 'square'

        # Check portrait orientation
        asset.set_resolution(1080, 1920)
        assert asset.get_attr('media.orientation') == 'portrait'

    def test_add_proxy(self):
        proxy = {
            "id": "proxy___123___foo.jpg",
            "mimetype": "image/jpeg",
            "width": 1024,
            "height": 768
        }
        asset = zsdk.Asset()
        proxies = asset.add_proxy(proxy)
        assert proxy in proxies
        assert proxy in asset.get_attr("proxies.proxies")

    def test_add_proxy_missing_height(self):
        proxy = {
            "id": "proxy___123___foo.jpg",
            "mimetype": "image/jpeg",
            "width": 1024
        }

        asset = zsdk.Asset()
        with pytest.raises(ValueError):
            asset.add_proxy(proxy)

    def test_add_proxy_missing_width(self):
        proxy = {
            "id": "proxy___123___foo.jpg",
            "mimetype": "image/jpeg",
            "height": 1024
        }

        asset = zsdk.Asset()
        with pytest.raises(ValueError):
            asset.add_proxy(proxy)

    def test_add_proxy_missing_mimetype(self):
        proxy = {
            "id": "proxy___123___foo.jpg",
            "height": 1024,
            "width": 2048
        }

        asset = zsdk.Asset()
        with pytest.raises(ValueError):
            asset.add_proxy(proxy)

    def test_add_proxy_missing_id(self):
        proxy = {
            "mimetype": "image/jpeg",
            "height": 1024,
            "width": 2048
        }

        asset = zsdk.Asset()
        with pytest.raises(ValueError):
            asset.add_proxy(proxy)

    def test_find_proxies(self):
        proxy1 = {
            "id": "proxy___123___foo.jpg",
            "mimetype": "image/jpeg",
            "width": 1024,
            "height": 768
        }
        proxy2 = {
            "id": "proxy___123___bar.jpg",
            "mimetype": "image/png",
            "width": 1024,
            "height": 768
        }
        asset = zsdk.Asset()
        asset.add_proxy(proxy1)
        asset.add_proxy(proxy2)
        assert asset.proxies == asset._find_proxies(lambda p: True)
        assert asset.proxies == asset._find_proxies(lambda p: p["width"] == 1024 and
                                                    p["height"] == 768)
        assert [proxy1] == asset._find_proxies(lambda p: p["mimetype"] == "image/jpeg")
