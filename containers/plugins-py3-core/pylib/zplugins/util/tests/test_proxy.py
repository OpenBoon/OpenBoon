import os
import tempfile

from unittest.mock import patch

import zorroa.zsdk as zsdk
from zorroa.zclient import ZClient
from zplugins.util.proxy import add_proxy_file, add_proxy_link, add_proxy
from zorroa.zsdk.testing import zorroa_test_data

IMAGE_JPG = zorroa_test_data('images/set01/faces.jpg')
VIDEO_WEBM = zorroa_test_data('video/dc.webm')
VIDEO_MP4 = zorroa_test_data('video/sample_ipad.m4v')


@patch.object(ZClient, 'post')
@patch.object(ZClient, 'get')
def test_add_proxy(get_patch, post_patch):
    tmp_dir = tempfile.mkdtemp("ofs", "zorroa")
    asset = zsdk.Asset(IMAGE_JPG)

    # Copied image proxy
    object_file_data = {'uri': tmp_dir + '/gritty_proxy.png',
                        'scheme': 'file',
                        'exists': True,
                        'size': 2048,
                        'mimeType': 'image/png',
                        'id': 'proxy/sickest-mascots/gritty_640_480.png'}
    post_patch.return_value = object_file_data
    get_patch.return_value = object_file_data
    add_proxy(asset, IMAGE_JPG, symlink=False)

    # Symlinked video proxy
    object_file_data = {'uri': tmp_dir + '/gritty_proxy.wemb',
                        'scheme': 'file',
                        'exists': True,
                        'size': 2048,
                        'mimeType': 'video/webm',
                        'id': 'proxy/sickest-mascots/gritty_640_480.webm'}
    post_patch.return_value = object_file_data
    get_patch.return_value = object_file_data
    symlinked = add_proxy(asset, VIDEO_WEBM, symlink=True)

    symlinked_path = zsdk.ofs.get_ofs().get(symlinked["id"]).path
    assert os.path.islink(symlinked_path)

    assert len(asset.proxies) == 2
    assert asset.proxies[0]['height'] == 339
    assert asset.proxies[0]['width'] == 512
    assert asset.proxies[0]['mimetype'] == 'image/jpeg'
    assert asset.proxies[1]['height'] == 1080
    assert asset.proxies[1]['width'] == 1920
    assert asset.proxies[1]['mimetype'] == 'video/webm'


@patch.object(ZClient, 'post')
@patch.object(ZClient, 'get')
def test_add_proxy_file(get_patch, post_patch):
    tmp_dir = tempfile.mkdtemp("ofs", "zorroa")
    object_file_data = {'uri': tmp_dir + '/gritty_proxy.png',
                        'scheme': 'file',
                        'exists': True,
                        'size': 2048,
                        'mimeType': 'image/png',
                        'id': 'proxy/sickest-mascots/gritty_640_480.png'}

    post_patch.return_value = object_file_data
    get_patch.return_value = object_file_data
    asset = zsdk.Asset(IMAGE_JPG)
    add_proxy_file(asset, IMAGE_JPG)
    add_proxy_file(asset, IMAGE_JPG)
    add_proxy_file(asset, VIDEO_MP4)
    assert len(asset.proxies) == 2
    assert asset.proxies[0]['height'] == 339
    assert asset.proxies[0]['width'] == 512
    assert asset.proxies[0]['mimetype'] == 'image/jpeg'
    assert asset.proxies[1]['height'] == 360
    assert asset.proxies[1]['width'] == 640
    assert asset.proxies[1]['mimetype'] in ['video/x-m4v', 'video/mp4']


@patch.object(ZClient, 'post')
@patch.object(ZClient, 'get')
def test_add_proxy_link(get_patch, post_patch):
    tmp_dir = tempfile.mkdtemp("ofs", "zorroa")
    object_file_data = {'uri': tmp_dir + os.path.basename(IMAGE_JPG),
                        'scheme': 'file',
                        'exists': True,
                        'size': 2048,
                        'mimeType': 'image/png',
                        'id': 'proxy__foo__faces.jpg'}
    post_patch.return_value = object_file_data
    get_patch.return_value = object_file_data

    asset = zsdk.Asset(IMAGE_JPG)
    add_proxy_link(asset, IMAGE_JPG)
    proxy = add_proxy_link(asset, IMAGE_JPG)

    proxy_path = zsdk.ofs.get_ofs().get(proxy["id"]).path
    assert os.path.islink(proxy_path)

    assert len(asset.proxies) == 1
    assert asset.proxies[0]['height'] == 339
    assert asset.proxies[0]['width'] == 512
    assert asset.proxies[0]['mimetype'] == 'image/jpeg'
