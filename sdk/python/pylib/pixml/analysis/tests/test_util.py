import pytest
from unittest.mock import patch

from pixml.analysis.testing import zorroa_test_data, TestAsset
from pixml.analysis.util import add_proxy_file, get_proxy_file
from pixml.rest import PixmlClient

IMAGE_JPG = zorroa_test_data('images/set01/faces.jpg')
VIDEO_WEBM = zorroa_test_data('video/dc.webm')
VIDEO_MP4 = zorroa_test_data('video/sample_ipad.m4v')


@patch.object(PixmlClient, 'stream')
def test__get_proxy_file(stream_patch):
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

    name, _ = get_proxy_file(asset, min_width=300)
    assert name == "proxy_400x400.jpg"

    name, _ = get_proxy_file(asset, mimetype="video/", min_width=350)
    assert name == "proxy_400x400.mp4"

    name, _ = get_proxy_file(asset, mimetype="image/", min_width=1025, fallback=True)
    assert name == "source"

    with pytest.raises(ValueError):
        get_proxy_file(asset, mimetype="video/", min_width=1025, fallback=False)



@patch.object(PixmlClient, 'upload_file')
def test_add_proxy_file(upload_patch):
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
    add_proxy_file(asset, IMAGE_JPG, (200, 200))
    add_proxy_file(asset, IMAGE_JPG, (200, 200))

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
    add_proxy_file(asset, VIDEO_MP4, (200, 200))
    assert 2 == len(asset.get_files())