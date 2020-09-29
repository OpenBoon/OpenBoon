import unittest
from unittest.mock import patch

from zmlp.client import ZmlpClient
from zmlp_core.util import media
from zmlpsdk import file_storage, StoredFile
from zmlpsdk.storage import ProjectStorage
from zmlpsdk.testing import zorroa_test_data, TestAsset

IMAGE_EXR = zorroa_test_data('images/set06/SquaresSwirls.exr', False)
IMAGE_PSD = zorroa_test_data('images/set06/psd_123.psd', False)
IMAGE_JPG = zorroa_test_data('images/set02/beer_kettle_01.jpg', False)
VIDEO_WEBM = zorroa_test_data('video/dc.webm', False)
VIDEO_MP4 = zorroa_test_data('video/FatManOnABike1914.mp4', False)
VIDEO_MXF = zorroa_test_data('mxf/freeMXF-mxf1.mxf', False)
VIDEO_MOV = zorroa_test_data('video/1324_CAPS_23.0_030.00_15_MISC.mov', False)


def test_get_image_metadata():
    metadata = media.get_image_metadata(IMAGE_JPG)
    assert metadata["width"] == "3264"
    assert metadata["height"] == "2448"


def test_get_video_metadata():
    metadata = media.get_video_metadata(VIDEO_MP4)
    assert 25.0 == metadata['frameRate']
    assert 3611 == metadata['frames']
    assert 450 == metadata['width']
    assert 360 == metadata['height']
    assert 144.45 == metadata['length']


@patch('zmlp_core.util.media.check_output')
def test_get_image_metadata_invalid_chars(check_out_patch):
    xml = """
    <ImageSpec version="20">
    <attrib name="oiio:ColorSpace" type="string">sRGB</attrib>
    <attrib name="jpeg:subsampling" type="string">4:4:4</attrib>
    <attrib name="IPTC:Caption" type="string">&#05;4.2.7</attrib>
    <attrib name="ImageDescription" type="string">&#05;4.2.7</attrib>
    <attrib name="XResolution" type="float">75</attrib>
    <attrib name="YResolution" type="float">75</attrib>
    <attrib name="ResolutionUnit" type="string">in</attrib>
    </ImageSpec>
    """

    check_out_patch.return_value = xml
    metadata = media.get_image_metadata(IMAGE_JPG)
    assert metadata["IPTC"]["Caption"] == "4.2.7"
    assert metadata["ImageDescription"] == "4.2.7"


def test_media_size_video():
    size = media.media_size(VIDEO_WEBM)
    assert size[0] == 1920
    assert size[1] == 1080

    size = media.media_size(VIDEO_MP4)
    assert size[0] == 450
    assert size[1] == 360

    size = media.media_size(VIDEO_MXF)
    assert size[0] == 720
    assert size[1] == 576


def test_media_size_image():
    size = media.media_size(IMAGE_JPG)
    assert size[0] == 3264
    assert size[1] == 2448

    size = media.media_size(IMAGE_PSD)
    assert size[0] == 257
    assert size[1] == 126

    size = media.media_size(IMAGE_EXR)
    assert size[0] == 1000
    assert size[1] == 1000


def test_get_output_dimension():
    # Test width being the longest edge.
    width, height = media.get_output_dimension(256, 512, 341)
    assert width == 256
    assert height == 170

    width, height = media.get_output_dimension(256, 341, 512)
    assert width == 170
    assert height == 256


def test_ffprobe_mp4():
    probe = media.ffprobe(VIDEO_MP4)
    assert len(probe["streams"]) == 2
    stream0 = probe["streams"][0]
    assert stream0["pix_fmt"] == "yuv420p"
    assert probe["format"]["duration"] == "144.450000"
    assert probe["format"]["size"] == "13168719"


def test_ffprobe_mxf():
    probe = media.ffprobe(VIDEO_MXF)
    assert len(probe["streams"]) == 1
    stream0 = probe["streams"][0]
    assert stream0["pix_fmt"] == "yuv420p"
    assert probe["format"]["duration"] == "10.720000"
    assert probe["format"]["size"] == "2815388"


def test_ffprobe_webm():
    probe = media.ffprobe(VIDEO_WEBM)
    assert len(probe["streams"]) == 1
    stream0 = probe["streams"][0]
    assert stream0["pix_fmt"] == "yuv420p"
    assert probe["format"]["duration"] == "11.466000"
    assert probe["format"]["size"] == "8437109"


def test_ffprobe_mov():
    probe = media.ffprobe(VIDEO_MOV)
    assert len(probe["streams"]) == 1
    stream0 = probe["streams"][0]
    assert stream0["pix_fmt"] == "yuvj420p"
    assert probe["format"]["duration"] == "0.041667"
    assert probe["format"]["size"] == "73981"


def test_media_info_class():
    info = media.MediaInfo(VIDEO_MP4)
    assert info.is_streamable()

    info = media.MediaInfo(VIDEO_MOV)
    assert not info.is_streamable()


class ProxyFunctionTests(unittest.TestCase):
    file_list = [
        {
            'id': "assets/123456/proxy/image_200x200.jpg",
            'name': 'image_200x200.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 200,
                'height': 200
            }
        },
        {
            'id': "assets/123456/proxy/image_400x400.jpg",
            'name': 'image_400x400.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 400,
                'height': 400
            }
        },
        {
            'id': "assets/123456/proxy/video_400x400.mp4",
            'name': 'video_400x400.mp4',
            'category': 'proxy',
            'mimetype': 'video/mp4',
            'attrs': {
                'width': 400,
                'height': 400
            }
        },
        {
            'id': "assets/123456/proxy/video_500x500.mp4",
            'name': 'video_500x500.mp4',
            'category': 'proxy',
            'mimetype': 'video/mp4',
            'attrs': {
                'width': 500,
                'height': 500
            }
        }
    ]

    @patch.object(ProjectStorage, 'store_file')
    def test_store_media_proxy_unique(self, store_patch):
        asset = TestAsset(IMAGE_JPG)
        store_patch.return_value = StoredFile({
            'id': 'assets/123456/proxy/image_200x200.jpg',
            'name': 'image_200x200.jpg',
            'category': 'proxy',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 200,
                'height': 200
            }
        })

        # Should only be added to list once.
        media.store_media_proxy(asset, IMAGE_JPG, (200, 200))
        media.store_media_proxy(asset, IMAGE_JPG, (200, 200))

        store_patch.return_value = StoredFile({
            'id': 'assets/123456/proxy/image_200x200.mp4',
            'name': 'image_200x200.mp4',
            'category': 'proxy',
            'mimetype': 'video/mp4',
            'attrs': {
                'width': 200,
                'height': 200
            }
        })

        media.store_media_proxy(asset, VIDEO_MP4, (200, 200))
        assert 2 == len(asset.get_files())

    @patch.object(file_storage.assets, 'store_file')
    @patch.object(ZmlpClient, 'upload_file')
    def test_store_media_proxy_with_attrs(self, upload_patch, store_file_patch):
        upload_patch.return_value = {}

        asset = TestAsset(IMAGE_JPG)
        asset.set_attr('tmp.image_proxy_source_attrs', {'king': 'kong'})
        media.store_media_proxy(
            asset, IMAGE_JPG, 'image', size=(200, 200), attrs={'foo': 'bar'})

        # Merges args from both the proxy_source_attrs attr and
        # args passed into store_proxy_media
        args, kwargs = store_file_patch.call_args_list[0]
        assert kwargs['attrs']['king'] == 'kong'
        assert kwargs['attrs']['width'] == 200
        assert kwargs['attrs']['height'] == 200
        assert kwargs['attrs']['foo'] == 'bar'
