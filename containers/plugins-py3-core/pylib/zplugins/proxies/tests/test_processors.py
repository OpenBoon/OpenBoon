import os

import pytest
from mock import patch
from pathlib2 import Path

import zsdk
from zplugins.proxies.processors import ProxyProcessor, ExistingProxyProcessor, \
    get_tiny_proxy_colors
from zsdk import Asset
from zsdk.testing import PluginUnitTestCase, zorroa_test_data

TOUCAN = zorroa_test_data("images/set01/toucan.jpg")
BEER = zorroa_test_data("images/set02/beer_kettle_01.jpg")
VIDEO = zorroa_test_data("video/dc.webm")


class ProxyIngestorUnitTestCase(PluginUnitTestCase):
    def setUp(self):
        self.source_path = TOUCAN
        self.frame = zsdk.Frame(Asset(self.source_path))
        self.frame.asset.set_attr('media.width', 512)
        self.frame.asset.set_attr('media.height', 341)
        self.frame.asset.set_attr('tmp.proxy_source_image', self.source_path)
        self.processor = self.init_processor(ProxyProcessor(), {})

    def test_init(self):
        with pytest.raises(ValueError) as error:
            self.init_processor(ProxyProcessor(), {'file_type': 'butts'})
        assert '"butts" is not a valid type' in error.value.args[0]

    def test_process(self):
        self.processor.process(self.frame)
        proxy_512 = self.ofs.prepare('asset', self.frame.asset, 'proxy_512x341.jpg')
        proxy_256 = self.ofs.prepare('asset', self.frame.asset, 'proxy_256x170.jpg')
        expected_proxies = [
            {'height': 341,
             'mimetype': 'image/jpeg',
             'id': proxy_512.id,
             'width': 512},
            {'height': 170,
             'mimetype': 'image/jpeg',
             'id': proxy_256.id,
             'width': 256}
        ]
        assert len(self.frame.asset.get_attr('proxies.tinyProxy')) == 9
        assert self.frame.asset.get_attr('proxies.proxies') == expected_proxies

    def test_skip_existing(self):

        frame = zsdk.Frame(Asset(self.source_path))
        frame.asset.set_attr('media.width', 1024)
        frame.asset.set_attr('media.height', 768)
        frame.asset.set_attr('tmp.proxy_source_image', self.source_path)

        processor = self.init_processor(ProxyProcessor(), {"sizes": [384]})
        processor.process(frame)
        assert len(frame.asset.get_attr("proxies.proxies")) == 1

        processor = self.init_processor(ProxyProcessor(), {"sizes": [512, 384]})
        processor.process(frame)
        assert len(frame.asset.get_attr("proxies.proxies")) == 2

        assert processor.created_proxy_count == 1

    @patch.object(ProxyProcessor, '_create_proxy_images')
    def test_no_process(self, proxy_patch):
        self.frame.asset.set_attr('proxies', True)
        assert proxy_patch.call_count == 0

    def test_create_proxy_images(self):
        self.maxDiff = None
        proxies = self.processor._create_proxy_images(self.frame.asset)
        assert len(proxies) == 2
        assert '%s_512x341.jpg' % self.frame.asset.id in proxies[0]
        assert '%s_256x170.jpg' % self.frame.asset.id in proxies[1]
        for proxy in proxies:
            assert Path(proxy).exists()

    def test_get_source_path(self):
        # Test correct field is in metadata.
        path = self.processor._get_source_path(self.frame.asset)
        assert path == self.source_path

        # Test fall back to source.path
        frame = zsdk.Frame(zsdk.Asset(self.source_path))
        path = self.processor._get_source_path(frame.asset)
        assert path == self.source_path

    def test_get_source_path_failure(self):
        # Videos can't be processed, so there is no valid source path.
        frame = zsdk.Frame(Asset(VIDEO))
        path = self.processor._get_source_path(frame.asset)
        assert not path

    def test_process_skip_video_with_no_proxy_source(self):
        # Videos can't be processed, so there is no valid source path.
        frame = zsdk.Frame(Asset(VIDEO))
        self.processor.process(frame)
        assert not frame.asset.get_attr("proxies.proxies")

    def test_process_asset_without_media_namespace(self):
        self.frame.asset.set_attr('media', {})
        self.processor.process(self.frame)
        proxy_512 = self.ofs.prepare('asset', self.frame.asset, 'proxy_512x341.jpg')
        proxy_256 = self.ofs.prepare('asset', self.frame.asset, 'proxy_256x170.jpg')
        expected_proxies = [
            {'height': 341,
             'mimetype': 'image/jpeg',
             'id': proxy_512.id,
             'width': 512},
            {'height': 170,
             'mimetype': 'image/jpeg',
             'id': proxy_256.id,
             'width': 256}
        ]

        assert len(self.frame.asset.get_attr('proxies.tinyProxy')) == 9
        assert self.frame.asset.get_attr('proxies.proxies') == expected_proxies

    def test_get_tiny_proxy_colors(self):
        colors = get_tiny_proxy_colors(self.source_path)
        assert len(colors) == 9

    def test_get_valid_sizes(self):
        assert self.processor._get_valid_sizes(800, 600) == [512, 256]
        assert self.processor._get_valid_sizes(100, 50) == [100]


class ExistingProxyIngestorUnitTestCase(PluginUnitTestCase):

    def test_processor_with_symlink(self):
        frame = zsdk.Frame(Asset(TOUCAN))
        search_dir = Path(BEER).parent
        word_dir = zorroa_test_data("word")
        args = {'search_directories': [str(search_dir), word_dir],
                'paths': [TOUCAN, '/does/not/exist.jpg'],
                'symlink': True}
        processor = self.init_processor(ExistingProxyProcessor(), args)
        processor.process(frame)
        proxies = frame.asset.proxies
        resolutions = ['%sx%s' % (p['width'], p['height']) for p in proxies]
        assert len(proxies) == 2
        assert '512x341' in resolutions
        assert '3264x2448' in resolutions

        assert os.path.islink(self.ofs.get(proxies[0]["id"]).path)
        assert os.path.islink(self.ofs.get(proxies[1]["id"]).path)

    def test_processor_with_copy(self):
        frame = zsdk.Frame(Asset(TOUCAN))
        search_dir = Path(BEER).parent
        word_dir = zorroa_test_data("word")
        args = {'search_directories': [str(search_dir), word_dir],
                'paths': [TOUCAN, '/does/not/exist.jpg'],
                'symlink': False}
        processor = self.init_processor(ExistingProxyProcessor(), args)
        processor.process(frame)
        proxies = frame.asset.proxies

        resolutions = ['%sx%s' % (p['width'], p['height']) for p in proxies]
        assert len(proxies) == 2
        assert '512x341' in resolutions
        assert '3264x2448' in resolutions

        assert not os.path.islink(self.ofs.get(proxies[0]["id"]).path)
        assert not os.path.islink(self.ofs.get(proxies[1]["id"]).path)

    def test_existing_video_proxy_symlink(self):
        frame = zsdk.Frame(Asset(VIDEO))
        search_dir = Path(VIDEO).parent
        args = {'search_directories': [str(search_dir)],
                'search_file_types': ["webm"],
                'symlink': True}
        processor = self.init_processor(ExistingProxyProcessor(), args)
        processor.process(frame)
        proxies = frame.asset.proxies

        resolutions = ['%sx%s' % (p['width'], p['height']) for p in proxies]
        assert len(proxies) == 1
        assert '1920x1080' in resolutions
        assert os.path.islink(self.ofs.get(proxies[0]["id"]).path)
