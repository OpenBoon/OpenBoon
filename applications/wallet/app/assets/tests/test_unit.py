import cv2
import numpy as np
import pytest
from requests import Response
from zmlp import Asset

from assets.utils import AssetBoxImager, crop_image_poly


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
             'processor': 'zmlp_core.core.PreCacheSourceFileProcessor'},
            {'executionTime': 3.49, 'module': 'standard', 'checksum': 117837444,
             'executionDate': '2020-04-21T02:49:48.915740',
             'processor': 'zmlp_core.core.FileImportProcessor'},
            {'executionTime': 2.68, 'module': 'standard', 'checksum': 457707303,
             'executionDate': '2020-04-21T02:49:52.344155',
             'processor': 'zmlp_core.proxy.ImageProxyProcessor'},
            {'module': 'standard', 'checksum': 482873147,
             'processor': 'zmlp_core.proxy.VideoProxyProcessor'},
            {'executionTime': 4.9, 'module': 'standard', 'checksum': 4122678338,
             'executionDate': '2020-04-21T02:50:06.853707',
             'processor': 'zmlp_analysis.similarity.ZviSimilarityProcessor'},
            {'executionTime': 5.54, 'module': 'zvi-object-detection',
             'checksum': 332797101, 'executionDate': '2020-04-21T22:27:26.028970',
             'processor': 'zmlp_analysis.detect.ZmlpObjectDetectionProcessor'}]},
        'media': {'orientation': 'landscape', 'aspect': 1.5, 'width': 650, 'length': 1,
                  'type': 'image', 'height': 434}, 'analysis': {
            'zvi-object-detection': {'count': 1, 'type': 'labels', 'predictions': [
                {'score': 0.882, 'bbox': [0.068, 0.079, 0.904, 0.739],
                 'label': 'laptop'}, {'score': 0.882, 'bbox': [0.068, 0.079, 0.904, 0.739],
                 'label': 'laptop2'}]}, 'zvi': {
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
            assert 'b64_image' in prediction
            assert prediction['b64_image'] == 'data:image/png;base64, iVBORw0KGgoAAAANSUhEUgAAAAoAAAAICAIAAABPmPnhAAAAI0lEQVQIHXXBAQEAAAABIP6PzgJV5CvyFfmKfEW+Il+Rr8g33SQX8fv7NasAAAAASUVORK5CYII='  # noqa

    def test_get_attr_with_box_images(self, imager, monkeypatch):
        monkeypatch.setattr(AssetBoxImager, '_download_image_from_zmlp',
                            mock_download_image_from_zmlp)
        analysis = imager.get_attr_with_box_images('analysis.zvi-object-detection', width=10)
        predictions = analysis['predictions']
        assert len(predictions) == 2
        for prediction in predictions:
            assert 'b64_image' in prediction
            assert prediction['b64_image'] == 'data:image/png;base64, iVBORw0KGgoAAAANSUhEUgAAAAoAAAAICAIAAABPmPnhAAAAI0lEQVQIHXXBAQEAAAABIP6PzgJV5CvyFfmKfEW+Il+Rr8g33SQX8fv7NasAAAAASUVORK5CYII='  # noqa


class TestCropImagePoly:
    def test_crop_image_poly(self):

        blank_image = np.zeros((100, 100, 3), np.uint8)
        cropped_image = crop_image_poly(blank_image, [-.1, 0, .5, .7], width=50)
        assert cropped_image.shape == (70, 50, 3)

        cropped_image = crop_image_poly(blank_image, [0.0026, 0.2903, 0.099, 0.2903, 0.099, 0.4755, 0.0026, 0.4755],
                                        width=50, draw=True)
        assert cropped_image.shape == (41, 50, 3)
