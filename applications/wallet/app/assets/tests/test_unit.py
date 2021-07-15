import cv2
import numpy as np
import pytest
from requests import Response
from boonsdk import Asset

from assets.utils import AssetBoxImager, crop_image_poly, get_largest_proxy


@pytest.fixture
def asset():
    return Asset({'id': 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C', 'document': {
        'system': {'jobId': '36147588-0235-125e-9732-0242ac13000b',
                   'dataSourceId': '36147587-0235-125e-9732-0242ac13000b',
                   'timeCreated': '2020-04-21T02:49:41.556331Z', 'state': 'Analyzed',
                   'projectId': '00000000-0000-0000-0000-000000000000',
                   'timeModified': '2020-04-21T22:27:32.579889Z',
                   'taskId': '36147589-0235-125e-9732-0242ac13000b'}, 'files': [
            {'size': 89643, 'name': 'image_650x434.jpg', 'mimetype': 'image/jpeg',
             'id': 'assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/proxy/image_650x434.jpg',
             'category': 'proxy', 'attrs': {'width': 650, 'height': 434}},
            {'size': 60713, 'name': 'image_512x341.jpg', 'mimetype': 'image/jpeg',
             'id': 'assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/proxy/image_512x341.jpg',
             'category': 'proxy', 'attrs': {'width': 512, 'height': 341}},
            {'size': 30882, 'name': 'image_320x213.jpg', 'mimetype': 'image/jpeg',
             'id': 'assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/proxy/image_320x213.jpg',
             'category': 'proxy', 'attrs': {'width': 320, 'height': 213}}],
        'source': {'path': 'gs://zorroa-dev-data/image/TIFF_1MB.tiff',
                   'extension': 'tiff', 'filename': 'TIFF_1MB.tiff',
                   'checksum': 1867533868, 'mimetype': 'image/tiff', 'filesize': 1131930},
        'metrics': {'pipeline': [
            {'executionTime': 1.02, 'module': 'standard', 'checksum': 2178814325,
             'executionDate': '2020-04-21T02:49:45.161782',
             'processor': 'boonai_core.core.PreCacheSourceFileProcessor'},
            {'executionTime': 3.49, 'module': 'standard', 'checksum': 117837444,
             'executionDate': '2020-04-21T02:49:48.915740',
             'processor': 'boonai_core.core.FileImportProcessor'},
            {'executionTime': 2.68, 'module': 'standard', 'checksum': 457707303,
             'executionDate': '2020-04-21T02:49:52.344155',
             'processor': 'boonai_core.proxy.ImageProxyProcessor'},
            {'module': 'standard', 'checksum': 482873147,
             'processor': 'boonai_core.proxy.VideoProxyProcessor'},
            {'executionTime': 4.9, 'module': 'standard', 'checksum': 4122678338,
             'executionDate': '2020-04-21T02:50:06.853707',
             'processor': 'boonai_analysis.similarity.ZviSimilarityProcessor'},
            {'executionTime': 5.54, 'module': 'zvi-object-detection',
             'checksum': 332797101, 'executionDate': '2020-04-21T22:27:26.028970',
             'processor': 'boonai_analysis.detect.ZmlpObjectDetectionProcessor'}]},
        'media': {'orientation': 'landscape', 'aspect': 1.5, 'width': 650, 'length': 1,
                  'type': 'image', 'height': 434}, 'analysis': {
            'zvi-object-detection': {'count': 1, 'type': 'labels', 'predictions': [
                {'score': 0.882, 'bbox': [0.068, 0.079, 0.904, 0.739],
                 'label': 'laptop'}, {'score': 0.882, 'bbox': [0.068, 0.079, 0.904, 0.739],
                 'label': 'laptop2'}]}, 'zvi': {  # noqa
                'tinyProxy': ['#f3dfc3', '#f4efd8', '#c18f46', '#ebdfbd', '#ccd3c0',
                              '#e7d4bb', '#beae8e', '#cabf9e', '#d2c09c']},
            'zvi-image-similarity': {'type': 'similarity',
                                     'simhash': 'NMGPEKEACJCHEDDBCHFAPBAHNPPPKMPOPCMODAPNFPCPOPBNEPCPPBHPPCBPDBPJHBGPPPNKOMBHBPNPAHLFCPPPFPGBKPPFDPPHDFAADNKGADBGPFFFGKABGPDLFPPLPPNPPPAAJBPPPLDPKAMCKANPAGCBPBPCEPEAKMHEHPBLKIIFJPOPBPPMGONPPGPFDPCJNPPMPIPPPPHPPCBJPHPGJDNBMGBAPAEBCFDPLPPAJLKANAPPACCPPEAMPADJMCAIPIKNNPDAPMPPCAAOPBADBDJPHGEKFGIDBPHDPPAMPPHLBGPPPHPEPPNAPPPAHOPCLAKPPPFAAHCPFCFPAPBPILPADJAFMJDPCPMCKBBBFBGPLPJMEDDMHPGHMMJPPABAJCAGCFJFNJPBPEPCPCINFEHPHCCPLPGPOIPPMPNIPNDDELAPILCEAPPAIPPDPPBGEFPJLFPJPIECMAEPABALFGLPPLFDPGDABPLMJDPDGBHFPPBPPCPFDPPDFJPAPCDGGGPPGPCPPFIKCHPPPBPLJBPPJIPHLPPKGKDPBCCLNGMAPFHACDINFPPEPBHDLCPGPFPPKPDAJPBEPPPPGPIADNAPABCKHIKPBDLPBPHPLAPFPPDBBEPEHPPPPFBJFANJPMPCFHMDEPDPMNIBDPPAHDBPPNECHLJAIDJFPOLPPBHLABEHPGPPKBAKIPJPBOPPBPHCBIPPAOPIPBAJHBMPPCMADHMPMFKPDEDOPBBAGNNACAADPHDCEKPDPGKHPBFPGJLPPINPPPBPPEANCPPAPBKBFGPAIPEFPFDJGPHAPCPIJHLCBEPPMPPCFPNEPIAPAAEPDHFBMHCNACPFFFDPBLMPPHEMAAIJODGCGCHIGDMAPHDNFEFMHPLPPBAKFBPPAPFPCGJNEDBOBPCLNIIIAEFGAGPGIEAFNPCCCBPKIDIPAPPPPOBPFPADEPPDCECMBFJPAKOEHPBKPGEOBFLNPFAIPPCKPCPHPAIPLPPLEJFFIMICPAPMEPKDNBDOFPJEPCAJEJKEMLHCGENAPEPACPPNPCCLDINPHCDBPHADKIFEPCIPKJIOFOPDEPGEDEPPGPDMBJKPBEHBEPLPFNPJGPJPBPHJHGHPAAEKDIEEAEAGJFPCGLPELPILCAPCFPELIICCPCLPJHCADFIPIGCFLIKPGPJGDCALPBDJPPEGCPLJFGEPBHDPPBPPPIKGDEEDENMBLLIGNECBPEFPBMJKCGPDPJOCNDNLANPKAPPPGPLBBBAJIGEADBEJFFNEGPNKPPPBPGEAELPKLDEGAANIFCLBPPPPNDJGDAFEEACPNEPKKPPKGAHLPDPGPBKPFBDIDDAMPPAIHPHHAPAEGOPCPJEABPPPBAKPLABPCPHNPAEMDPPPPDAJANPPDMPEGFIDEEAAJLGPHPHPCLDPDPFIDCAPPPHIPPPPEDHJDBBPHFKBAOPPPFGEPJPLPCDPBAJCPPDPEFGBFECPIAOGHAPEPAMPNPCPBAPODHNDCIPAGPPPFCPPDMFCLCEHPHAEACACIPCDPPPOMLAKMPPBGDPLFDBMFAPCMIKPHFPPCPPDFPPBPPKPPPPPFECBFIPKKEIPPPPBCKPLEPDEPKCHEPCEJDEKABHBOPPAAPIKABKHBPHPGPDBPHAPJKEDBPBBPLCPFDEEKHEPDFNPGJPAPPOPCCCJPPKPAEDIFPADNADAPDPPEMFPMPPLMPIPIADLPPPFINEPBBBAFBKIFPDPPPEBNPFBEMPFCCEGHEJIMACOCCPHBJPPNKPIPACDPBJPFKPAPIDNPCIGGIPHJGPCKELBLAAPLBBHHHPPCBHPLFAPPJPEEPLHPGPFAOOPPFPJHILPDECMJACKHHAPANLPGALJBNAFODHMJCPPFPJICDPOJBGPKFFPGIJPOHPJAIEPBBKCBIIMIPFPPPAGDDHHHIGGKHFJPPPPEKBGHDCHDPAGEPPIPDPIKALNPHJBGIPPNFINIDPPPEPPCBPPLMFCLGGPDGGLKINGBGKEPHPBPBFHAOBGAHCDGNEBDDCGPDCP'}},  # noqa
        'clip': {'sourceAssetId': 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C', 'stop': 1.0,
                 'pile': 'pUn6wBxUN7x9JxOxLkvruOyNdYA', 'start': 1.0, 'length': 1.0,
                 'type': 'page'}}})  # noqa


@pytest.fixture
def imager(asset):
    return AssetBoxImager(asset, None)


def mock_download_image_from_zmlp(*args, **kwargs):
    image = np.zeros([128, 128, 1], dtype=np.uint8)
    image.fill(255)
    image_str = cv2.imencode('.jpg', image)[1]
    response = Response()
    response.status_code = 200
    response._content = image_str
    return response


class TestAssetBoxImager:
    def test_image(self, monkeypatch, imager):
        monkeypatch.setattr(AssetBoxImager, '_download_image_from_zmlp',
                            mock_download_image_from_zmlp)
        image = imager.image
        assert image.shape[0] == 128
        assert image.shape[1] == 128

    def test_add_box_images(self, monkeypatch, asset, imager):
        monkeypatch.setattr(AssetBoxImager, '_download_image_from_zmlp',
                            mock_download_image_from_zmlp)
        analysis = asset.get_attr('analysis.zvi-object-detection')
        imager._add_box_images(analysis, 10)
        predictions = analysis['predictions']
        assert len(predictions) == 2
        for prediction in predictions:
            assert 'b64Image' in prediction
            assert prediction['b64Image'] == 'data:image/png;base64, iVBORw0KGgoAAAANSUhEUgAAAAoAAAAICAIAAABPmPnhAAAAI0lEQVQIHXXBAQEAAAABIP6PzgJV5CvyFfmKfEW+Il+Rr8g33SQX8fv7NasAAAAASUVORK5CYII='  # noqa

    def test_get_attr_with_box_images(self, imager, monkeypatch):
        monkeypatch.setattr(AssetBoxImager, '_download_image_from_zmlp',
                            mock_download_image_from_zmlp)
        analysis = imager.get_attr_with_box_images('analysis.zvi-object-detection', width=10)
        predictions = analysis['predictions']
        assert len(predictions) == 2
        for prediction in predictions:
            assert 'b64Image' in prediction
            assert prediction['b64Image'] == 'data:image/png;base64, iVBORw0KGgoAAAANSUhEUgAAAAoAAAAICAIAAABPmPnhAAAAI0lEQVQIHXXBAQEAAAABIP6PzgJV5CvyFfmKfEW+Il+Rr8g33SQX8fv7NasAAAAASUVORK5CYII='  # noqa


class TestCropImagePoly:
    def test_crop_image_poly(self):

        blank_image = np.zeros((100, 100, 3), np.uint8)
        cropped_image = crop_image_poly(blank_image, [-.1, 0, .5, .7], width=50)
        assert cropped_image.shape == (70, 50, 3)

        cropped_image = crop_image_poly(blank_image,
                                        [0.0026, 0.2903, 0.099, 0.2903,
                                         0.099, 0.4755, 0.0026, 0.4755],
                                        width=50, draw=True)
        assert cropped_image.shape == (100, 50, 3)

        blank_image = np.zeros((350, 590, 3), np.uint8)
        cropped_image = crop_image_poly(blank_image,
                                        [0.7578, 0.5278, 0.9366, 0.5278,
                                         0.9366, 0.8183, 0.7578, 0.8183],
                                        width=56, thickness=0)
        assert cropped_image.shape == (54, 56, 3)


class TestAssetModifierHelpers:

    @pytest.fixture
    def proxies(self):
        return [
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/image_500x400.jpg",
                "name": "image_500x400.jpg",
                "category": "proxy",
                "mimetype": "image/jpeg",
                "size": 132785,
                "attrs": {
                    "width": 500,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/image_900x400.jpg",
                "name": "image_900x400.jpg",
                "category": "proxy",
                "mimetype": "image/jpeg",
                "size": 132785,
                "attrs": {
                    "width": 900,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/web-proxy/image_600x400.jpg",
                "name": "image_600x400.jpg",
                "category": "proxy",
                "mimetype": "image/jpeg",
                "size": 53303,
                "attrs": {
                    "width": 600,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/image_100x400.jpg",
                "name": "image_100x400.jpg",
                "category": "proxy",
                "mimetype": "image/jpeg",
                "size": 132785,
                "attrs": {
                    "width": 100,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/video_500x400.mp4",
                "name": "video_500x400.mp4",
                "category": "proxy",
                "mimetype": "video/mpeg",
                "size": 132785,
                "attrs": {
                    "width": 500,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/video_900x400.mp4",
                "name": "video_900x400.mp4",
                "category": "proxy",
                "mimetype": "video/mpeg",
                "size": 132785,
                "attrs": {
                    "width": 900,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/web-proxy/video_600x400.mp4",
                "name": "video_600x400.mp4",
                "category": "proxy",
                "mimetype": "video/mpeg",
                "size": 53303,
                "attrs": {
                    "width": 600,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/video_100x400.mp4",
                "name": "video_100x400.mp4",
                "category": "proxy",
                "mimetype": "video/mpeg",
                "size": 132785,
                "attrs": {
                    "width": 100,
                    "height": 400
                }
            },
        ]

    @pytest.fixture
    def web_proxies(self):
        return [
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/web-proxy_500x400.jpg",
                "name": "web-proxy_500x400.jpg",
                "category": "web-proxy",
                "mimetype": "image/jpeg",
                "size": 132785,
                "attrs": {
                    "width": 500,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/web-proxy_900x400.jpg",
                "name": "web-proxy_900x400.jpg",
                "category": "web-proxy",
                "mimetype": "image/jpeg",
                "size": 132785,
                "attrs": {
                    "width": 900,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/web-proxy/web-proxy_600x400.jpg",
                "name": "web-proxy_600x400.jpg",
                "category": "web-proxy",
                "mimetype": "image/jpeg",
                "size": 53303,
                "attrs": {
                    "width": 600,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/web-proxy_100x400.jpg",
                "name": "web-proxy_100x400.jpg",
                "category": "web-proxy",
                "mimetype": "image/jpeg",
                "size": 132785,
                "attrs": {
                    "width": 100,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/web-proxy_500x400.mp4",
                "name": "web-proxy_500x400.mp4",
                "category": "web-proxy",
                "mimetype": "video/mpeg",
                "size": 132785,
                "attrs": {
                    "width": 500,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/web-proxy_900x400.mp4",
                "name": "web-proxy_900x400.mp4",
                "category": "web-proxy",
                "mimetype": "video/mpeg",
                "size": 132785,
                "attrs": {
                    "width": 900,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/web-proxy/web-proxy_600x400.mp4",
                "name": "web-proxy_600x400.mp4",
                "category": "web-proxy",
                "mimetype": "video/mpeg",
                "size": 53303,
                "attrs": {
                    "width": 600,
                    "height": 400
                }
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/web-proxy_100x400.mp4",
                "name": "web-proxy_100x400.mp4",
                "category": "web-proxy",
                "mimetype": "video/mpeg",
                "size": 132785,
                "attrs": {
                    "width": 100,
                    "height": 400
                }
            },
        ]

    @pytest.fixture
    def data_files(self):
        return [
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/timeline.dat",
                "name": "timeline.dat",
                "category": "data",
                "mimetype": "application/octet-stream",
                "size": 132785,
                "attrs": {}
            },
            {
                "id": "assets/3Dtt85Q6gdOLq-qQCA9c9eZwyj6oWfwK/proxy/data.gz",
                "name": "data.gz",
                "category": "timeline",
                "mimetype": "application/gzip",
                "size": 132785,
                "attrs": {
                    "width": 900,
                    "height": 400
                }
            },
        ]

    def test_get_largest_image_proxy_all_files(self, proxies, web_proxies, data_files):
        _files = proxies + web_proxies + data_files
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'image')
        assert proxy['name'] == "web-proxy_900x400.jpg"

    def test_get_largest_image_proxy_proxies_and_web(self, proxies, web_proxies):
        _files = proxies + web_proxies
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'image')
        assert proxy['name'] == "web-proxy_900x400.jpg"

    def test_get_largest_image_proxy_proxies_and_data(self, proxies, data_files):
        _files = proxies + data_files
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'image')
        assert proxy['name'] == "image_900x400.jpg"

    def test_get_largest_image_proxy_web_and_data(self, web_proxies, data_files):
        _files = web_proxies + data_files
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'image')
        assert proxy['name'] == "web-proxy_900x400.jpg"

    def test_get_largest_image_proxy_proxies_only(self, proxies):
        _files = proxies
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'image')
        assert proxy['name'] == "image_900x400.jpg"

    def test_get_largest_image_proxy_web_only(self, web_proxies):
        _files = web_proxies
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'image')
        assert proxy['name'] == "web-proxy_900x400.jpg"

    def test_get_largest_image_proxy_data_files(self, data_files):
        _files = data_files
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'image')
        assert proxy is None

    def test_get_largest_image_proxy_no_files(self):
        item = {'metadata': {'files': []}}
        proxy = get_largest_proxy(item, 'image')
        assert proxy is None

    def test_get_largest_video_proxy_all_files(self, proxies, web_proxies, data_files):
        _files = proxies + web_proxies + data_files
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'video')
        assert proxy['name'] == "web-proxy_900x400.mp4"

    def test_get_largest_video_proxy_proxies_and_web(self, proxies, web_proxies):
        _files = proxies + web_proxies
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'video')
        assert proxy['name'] == "web-proxy_900x400.mp4"

    def test_get_largest_video_proxy_proxies_and_data(self, proxies, data_files):
        _files = proxies + data_files
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'video')
        assert proxy['name'] == "video_900x400.mp4"

    def test_get_largest_video_proxy_web_and_data(self, web_proxies, data_files):
        _files = web_proxies + data_files
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'video')
        assert proxy['name'] == "web-proxy_900x400.mp4"

    def test_get_largest_video_proxy_proxies_only(self, proxies):
        _files = proxies
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'video')
        assert proxy['name'] == "video_900x400.mp4"

    def test_get_largest_video_proxy_web_only(self, web_proxies):
        _files = web_proxies
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'video')
        assert proxy['name'] == "web-proxy_900x400.mp4"

    def test_get_largest_video_proxy_data_files(self, data_files):
        _files = data_files
        item = {'metadata': {'files': _files}}
        proxy = get_largest_proxy(item, 'video')
        assert proxy is None

    def test_get_largest_video_proxy_no_files(self):
        item = {'metadata': {'files': []}}
        proxy = get_largest_proxy(item, 'video')
        assert proxy is None
