import pytest
import requests
from django.http import StreamingHttpResponse
from django.urls import reverse
from rest_framework import status
from zmlp import ZmlpClient, Asset

from assets.views import MetadataExportViewSet

pytestmark = pytest.mark.django_db


@pytest.fixture
def list_api_return():
    return {"took":8,"timed_out":False,"_shards":{"total":2,"successful":2,"skipped":0,"failed":0},"hits":{"total":{"value":2,"relation":"eq"},"max_score":1.0,"hits":[{"_index":"4w9bajznsa1yxbo4","_type":"_doc","_id":"vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C","_score":1.0,"_source":{"system":{"jobId":"8d2603f7-00d4-132f-8681-0242ac120009","dataSourceId":"8d2603f6-00d4-132f-8681-0242ac120009","timeCreated":"2020-03-03T21:54:02.002039Z","state":"Analyzed","projectId":"00000000-0000-0000-0000-000000000000","taskId":"8d2603f8-00d4-132f-8681-0242ac120009","timeModified":"2020-03-03T21:54:23.978500Z"},"source":{"path":"gs://zorroa-dev-data/image/TIFF_1MB.tiff","extension":"tiff","filename":"TIFF_1MB.tiff","mimetype":"image/tiff","filesize":1131930,"checksum":1867533868},"metrics":{"pipeline":[{"processor":"zmlp_core.core.processors.PreCacheSourceFileProcessor","module":"standard","checksum":1621235190,"executionTime":0.52,"executionDate":"2020-03-03T21:54:04.185632"},{"processor":"zmlp_core.image.importers.ImageImporter","module":"standard","checksum":1426657387,"executionTime":0.5,"executionDate":"2020-03-03T21:54:06.820102"},{"processor":"zmlp_core.office.importers.OfficeImporter","module":"standard","checksum":2001473853},{"processor":"zmlp_core.video.VideoImporter","module":"standard","checksum":3310423168},{"processor":"zmlp_core.core.processors.AssertAttributesProcessor","module":"standard","checksum":1841569083,"executionTime":0.0,"executionDate":"2020-03-03T21:54:08.449234"},{"processor":"zmlp_core.proxy.ImageProxyProcessor","module":"standard","checksum":457707303,"executionTime":0.89,"executionDate":"2020-03-03T21:54:09.394490"},{"processor":"zmlp_core.proxy.VideoProxyProcessor","module":"standard","checksum":482873147},{"processor":"zmlp_analysis.mxnet.ZviSimilarityProcessor","module":"standard","checksum":2479952423,"executionTime":2.07,"executionDate":"2020-03-03T21:54:20.533214"}]},"media":{"width":650,"height":434,"aspect":1.5,"orientation":"landscape","type":"image","length":1},"clip":{"type":"page","start":1.0,"stop":1.0,"length":1.0,"pile":"pUn6wBxUN7x9JxOxLkvruOyNdYA","sourceAssetId":"vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C"},"files":[{"name":"image_650x434.jpg","category":"proxy","mimetype":"image/jpeg","size":89643,"attrs":{"width":650,"height":434}},{"name":"image_512x341.jpg","category":"proxy","mimetype":"image/jpeg","size":60713,"attrs":{"width":512,"height":341}},{"name":"image_320x213.jpg","category":"proxy","mimetype":"image/jpeg","size":30882,"attrs":{"width":320,"height":213}}],"analysis":{"zvi":{"tinyProxy":["#f3dfc3","#f4efd8","#c18f46","#ebdfbd","#ccd3c0","#e7d4bb","#beae8e","#cabf9e","#d2c09c"],"similarity":{"simhash":"PBPBFHAOBGAHCDGNEBDDCGPDCP"}}}}},{"_index":"4w9bajznsa1yxbo4","_type":"_doc","_id":"XK6ra3Jl_xhtrqP0Nf8hNJ7JcIRfWXCE","_score":1.0,"_source":{"system":{"jobId":"8d2603f7-00d4-132f-8681-0242ac120009","dataSourceId":"8d2603f6-00d4-132f-8681-0242ac120009","timeCreated":"2020-03-03T21:54:02.149855Z","state":"Analyzed","projectId":"00000000-0000-0000-0000-000000000000","taskId":"8d2603f8-00d4-132f-8681-0242ac120009","timeModified":"2020-03-03T21:54:23.981546Z"},"source":{"path":"gs://zorroa-dev-data/image/mulipage.tif","extension":"tif","filename":"mulipage.tif","mimetype":"image/tiff","filesize":810405,"checksum":166113922},"metrics":{"pipeline":[{"processor":"zmlp_core.core.processors.PreCacheSourceFileProcessor","module":"standard","checksum":1621235190,"executionTime":0.41,"executionDate":"2020-03-03T21:54:04.594088"},{"processor":"zmlp_core.image.importers.ImageImporter","module":"standard","checksum":1426657387,"executionTime":0.29,"executionDate":"2020-03-03T21:54:07.112204"},{"processor":"zmlp_core.office.importers.OfficeImporter","module":"standard","checksum":2001473853},{"processor":"zmlp_core.video.VideoImporter","module":"standard","checksum":3310423168},{"processor":"zmlp_core.core.processors.AssertAttributesProcessor","module":"standard","checksum":1841569083,"executionTime":0.0,"executionDate":"2020-03-03T21:54:08.451394"},{"processor":"zmlp_core.proxy.ImageProxyProcessor","module":"standard","checksum":457707303,"executionTime":0.67,"executionDate":"2020-03-03T21:54:10.067415"},{"processor":"zmlp_core.proxy.VideoProxyProcessor","module":"standard","checksum":482873147},{"processor":"zmlp_analysis.mxnet.ZviSimilarityProcessor","module":"standard","checksum":2479952423,"executionTime":0.49,"executionDate":"2020-03-03T21:54:21.033848"}]},"media":{"width":800,"height":600,"aspect":1.33,"orientation":"landscape","type":"image","length":10},"clip":{"type":"page","start":1.0,"stop":1.0,"length":1.0,"pile":"vQz9dxEUpYvg0AVkXQV_xt2m2tg","sourceAssetId":"XK6ra3Jl_xhtrqP0Nf8hNJ7JcIRfWXCE"},"files":[{"name":"image_800x600.jpg","category":"proxy","mimetype":"image/jpeg","size":78408,"attrs":{"width":800,"height":600}},{"name":"image_512x384.jpg","category":"proxy","mimetype":"image/jpeg","size":42889,"attrs":{"width":512,"height":384}},{"name":"image_320x240.jpg","category":"proxy","mimetype":"image/jpeg","size":20676,"attrs":{"width":320,"height":240}}],"analysis":{"zvi":{"tinyProxy":["#f9f9f9","#ffffff","#ffffff","#dddddd","#d3d3d3","#ffffff","#ffffff","#ffffff","#ffffff"],"similarity":{"simhash":"AEKCPPAKBBPAOIPDPOGNEDJFDLA"}}}}}]}}  # noqa


@pytest.fixture
def detail_api_return():
    return {"id":"vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C","document":{"system":{"jobId":"8d2603f7-00d4-132f-8681-0242ac120009","dataSourceId":"8d2603f6-00d4-132f-8681-0242ac120009","timeCreated":"2020-03-03T21:54:02.002039Z","state":"Analyzed","projectId":"00000000-0000-0000-0000-000000000000","timeModified":"2020-03-03T21:54:23.978500Z","taskId":"8d2603f8-00d4-132f-8681-0242ac120009"},"files":[{"size":89643,"name":"image_650x434.jpg","mimetype":"image/jpeg","category":"proxy","attrs":{"width":650,"height":434}},{"size":60713,"name":"image_512x341.jpg","mimetype":"image/jpeg","category":"proxy","attrs":{"width":512,"height":341}},{"size":30882,"name":"image_320x213.jpg","mimetype":"image/jpeg","category":"proxy","attrs":{"width":320,"height":213}}],"source":{"path":"gs://zorroa-dev-data/image/TIFF_1MB.tiff","extension":"tiff","filename":"TIFF_1MB.tiff","checksum":1867533868,"mimetype":"image/tiff","filesize":1131930},"metrics":{"pipeline":[{"executionTime":0.52,"module":"standard","checksum":1621235190,"executionDate":"2020-03-03T21:54:04.185632","processor":"zmlp_core.core.processors.PreCacheSourceFileProcessor"},{"executionTime":0.5,"module":"standard","checksum":1426657387,"executionDate":"2020-03-03T21:54:06.820102","processor":"zmlp_core.image.importers.ImageImporter"},{"module":"standard","checksum":2001473853,"processor":"zmlp_core.office.importers.OfficeImporter"},{"module":"standard","checksum":3310423168,"processor":"zmlp_core.video.VideoImporter"},{"executionTime":0.0,"module":"standard","checksum":1841569083,"executionDate":"2020-03-03T21:54:08.449234","processor":"zmlp_core.core.processors.AssertAttributesProcessor"},{"executionTime":0.89,"module":"standard","checksum":457707303,"executionDate":"2020-03-03T21:54:09.394490","processor":"zmlp_core.proxy.ImageProxyProcessor"},{"module":"standard","checksum":482873147,"processor":"zmlp_core.proxy.VideoProxyProcessor"},{"executionTime":2.07,"module":"standard","checksum":2479952423,"executionDate":"2020-03-03T21:54:20.533214","processor":"zmlp_analysis.mxnet.ZviSimilarityProcessor"}]},"media":{"orientation":"landscape","aspect":1.5,"width":650,"length":1,"type":"image","height":434},"analysis":{"zvi":{"similarity":{"simhash":"PBPBFHAOBGAHCDGNEBDDCGPDCP"},"tinyProxy":["#f3dfc3","#f4efd8","#c18f46","#ebdfbd","#ccd3c0","#e7d4bb","#beae8e","#cabf9e","#d2c09c"]}},"clip":{"sourceAssetId":"vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C","stop":1.0,"pile":"pUn6wBxUN7x9JxOxLkvruOyNdYA","start":1.0,"length":1.0,"type":"page"}},"analyzed":True}  # noqa


class TestAssetViewSet:

    def test_get_list(self, project, zvi_project_user, api_client, monkeypatch, list_api_return):

        def mock_api_response(*args, **kwargs):
            return list_api_return

        monkeypatch.setattr(ZmlpClient, 'post', mock_api_response)
        api_client.force_authenticate(zvi_project_user)
        api_client.force_login(zvi_project_user)
        response = api_client.get(reverse('asset-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        content = response.json()
        assert content['count'] == 2
        assert len(content['results']) == 2
        assert 'next' in content
        assert 'previous' in content

    def test_get_detail(self, project, zvi_project_user, api_client, monkeypatch,
                        detail_api_return):

        def mock_api_response(*args, **kwargs):
            return detail_api_return

        monkeypatch.setattr(ZmlpClient, 'get', mock_api_response)
        api_client.force_authenticate(zvi_project_user)
        api_client.force_login(zvi_project_user)
        id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        response = api_client.get(reverse('asset-detail', kwargs={'project_pk': project.id,
                                                                  'pk': id}))
        assert response.status_code == 200
        content = response.json()
        assert content['id'] == id
        assert 'metadata' in content
        assert 'url' in content
        assert content['url'] == 'http://testserver/api/v1/projects/6abc33f0-4acf-4196-95ff-4cbb7f640a06/assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/'  # noqa
        assert content['metadata']['files'][0]['url'] == 'http://testserver/api/v1/projects/6abc33f0-4acf-4196-95ff-4cbb7f640a06/assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/files/category/proxy/name/image_650x434.jpg'  # noqa

    def test_list_and_detail_resources_match(self, project, zvi_project_user, api_client,
                                             monkeypatch, detail_api_return, list_api_return):

        def mock_list_response(*args, **kwargs):
            return list_api_return

        def mock_detail_response(*args, **kwargs):
            return detail_api_return

        monkeypatch.setattr(ZmlpClient, 'post', mock_list_response)
        api_client.force_authenticate(zvi_project_user)
        api_client.force_login(zvi_project_user)
        response = api_client.get(reverse('asset-list', kwargs={'project_pk': project.id}))
        list_content = response.json()

        monkeypatch.setattr(ZmlpClient, 'get', mock_detail_response)
        id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        response = api_client.get(reverse('asset-detail', kwargs={'project_pk': project.id,
                                                                  'pk': id}))
        detail_content = response.json()

        assert list_content['results'][0] == detail_content


class TestAssetSearch:

    @pytest.fixture
    def response_data(self):
        return {'took': 6, 'timed_out': False, '_shards': {'total': 1, 'successful': 1, 'skipped': 0, 'failed': 0}, 'hits': {'total': {'value': 1, 'relation': 'eq'}, 'max_score': 1.0, 'hits': [{'_index': 'g2z7et4hcveue1fu', '_type': '_doc', '_id': 'XK6ra3Jl_xhtrqP0Nf8hNJ7JcIRfWXCE', '_score': 1.0, '_source': {'system': {'jobId': 'f1ccbf4c-8dee-12fa-a4be-0242ac12000b', 'dataSourceId': 'f1ccbf4b-8dee-12fa-a4be-0242ac12000b', 'timeCreated': '2020-03-31T19:00:39.707636Z', 'state': 'Analyzed', 'projectId': '00000000-0000-0000-0000-000000000000', 'taskId': 'f1ccbf4d-8dee-12fa-a4be-0242ac12000b', 'timeModified': '2020-03-31T19:09:09.702663Z'}, 'source': {'path': 'gs://zorroa-dev-data/image/mulipage.tif', 'extension': 'tif', 'filename': 'mulipage.tif', 'mimetype': 'image/tiff', 'filesize': 810405, 'checksum': 166113922}, 'metrics': {'pipeline': [{'processor': 'zmlp_core.core.PreCacheSourceFileProcessor', 'module': 'standard', 'checksum': 2178814325, 'executionTime': 8.52, 'executionDate': '2020-03-31T19:00:50.934852'}, {'processor': 'zmlp_core.core.FileImportProcessor', 'module': 'standard', 'checksum': 117837444, 'executionTime': 2.16, 'executionDate': '2020-03-31T19:03:41.662065'}, {'processor': 'zmlp_core.proxy.ImageProxyProcessor', 'module': 'standard', 'checksum': 457707303, 'executionTime': 3.85, 'executionDate': '2020-03-31T19:07:16.649375'}, {'processor': 'zmlp_core.proxy.VideoProxyProcessor', 'module': 'standard', 'checksum': 482873147}, {'processor': 'zmlp_analysis.mxnet.ZviSimilarityProcessor', 'module': 'standard', 'checksum': 2479952423, 'executionTime': 7.37, 'executionDate': '2020-03-31T19:08:58.305102'}]}, 'media': {'width': 800, 'height': 600, 'aspect': 1.33, 'orientation': 'landscape', 'type': 'image', 'length': 10}, 'clip': {'type': 'page', 'start': 1.0, 'stop': 1.0, 'length': 1.0, 'pile': 'vQz9dxEUpYvg0AVkXQV_xt2m2tg', 'sourceAssetId': 'XK6ra3Jl_xhtrqP0Nf8hNJ7JcIRfWXCE'}, 'files': [{'name': 'image_800x600.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 78408, 'attrs': {'width': 800, 'height': 600}}, {'name': 'image_512x384.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 42889, 'attrs': {'width': 512, 'height': 384}}, {'name': 'image_320x240.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 20676, 'attrs': {'width': 320, 'height': 240}}], 'analysis': {'zvi': {'tinyProxy': ['#f9f9f9', '#ffffff', '#ffffff', '#dddddd', '#d3d3d3', '#ffffff', '#ffffff', '#ffffff', '#ffffff'], 'similarity': {'simhash': 'APPPBPPEJMFKPCPGPMBBBDCPPAFPNPGAPCPPLAPLBMACAAPEPPBIPGAPAPBPBCPGAPEOPAOHAPALNIBPIOLIEPBDDBPACAPDBCCPOPPAPADAEDGACPPFBPBAGPPNJLPPPHAHPAPKCAPPGABKIPGBFGCPFBPDNCPBBIPNKBAAPDMAGANDLABOBIAEFFMFEBPPBHJGPPPPPIMNEAPGPPAPNAPCPPLOCPCDGBCKPPFLPGCCLPPPDPPPLAMEJOAAEBAAFIJGCDPFBJDAJILFBABEPGPBCPEAJPAPPPGMPABBPELPPGAGJPPPAACIMLABPCDPFEGADFKDPMCAAJLPEBEDAJFPKBLJCGGBPBPCAALBPGINGBECICIMKAPPDIHCNPABGDPMDIACAJDAJPKAJPEHGBJPAADPCPLEBPAAGBAPELDPPEPMPPPGPHEBBAAPGMOAAPFPFCAAPAIAPMCEPCPPPNLGDCHLGAIAAFDPGPJDJFPGPKFJDNGPEPHPIDPPCCPAJAABHPPJMABPBPBDIAPHCDPPACPAPCPECPFNBOLJPANBAEJFFAFPBDCJFOIPAOBMPAEPBPFCAINAHAAFIEPEPCBHIEBDPIPDPPPBADAIPCNIACMCALBDPPPIPPKAAIPCBPPAEBCEEKOCEALFPPGAFPNFAIAIBIPCPBPFPBDPECCABPEJGAPPADLPPFFPPAAIBCCAPADGAPPEPIPDPAKPOCPNLCAABPLBAPNABEABJPHHHPAHPBHAFBJDPCPPAPPCPEAGDPOFHPACDEAPPBFPBLBPCOADAAADAPIPPPBEAADMDBAAPEFAABPCAACPCMIBPHPPAHPDBBBLIAEELMHOLBEEPPFEDPBEPPPDBGAPGBEFHGAAPBPDIICOBPCOFFPPHAAPGPNCABCIIBKHFGHEPABAGFOKCOPPAOHFBPBPHHAHEPPPKCPGPLCDOEAHGGHLDIPNFKPBACAPAPPDLMEBIAGOAFEAABAPEKDOBLPCCBBLALGPAPAGPJPEPEHHEEBLHBPAPLADDAPDDPPCHPLCPIPILAFCPNFDPAGHPICPPDMOBADPPBPLFJFAPEPPGBBPPFPAPPPNCPBBHOHADPPOJEPPCABBEHPAPLPFOFPMAAPHAIPAHPLDFPPHEPPAIPFIBHFCPPPPPPMGPHAGDAPMCBEPPAPEDPPCAPDDCJBPCBPOLAPPCIFPHOPJCAAPPPMEAPOPAPDPFPLPCPBPCCFPPCACDFPMMPPHAEPMGAPAAPPAPIPAALACAGCAPPPPPIBMAAGLHPCBLGFPPBBABBPPFPPPKPCCPPPBCODNPCKPFBEPPAJHJABBAADCFABAOKPPDPMMCAPBGKPAGACIEAKHAPCBPPLBABBPBIBPABGFPJPAAPJNCBKCACAKPDDAALAAOINPBAPIAPPAAPJBPBPNPDPLFBFKMPFEPPGCCPFLPEIPAEEPPCACPPAPPEFPPPAGKBPPPPFAHHACCBPILCPAABCFPFAPAJBPAHPNKPPAPCBPAPLHAEDCAICPCPCDPPFFABKBPODPPFPNPPEPAAAAIACMFOBFHAPCPPAANIPPJIJPAAIPEKACBEDPAHPPPPPBLJBICBCPADNBPPPPPEDCBABCKAPOEMCANPPPCNDGGBDAIDAPCBBLDEHPHIPPKIPMCNAAPAOMICPKBPFPBGCEPNFNAAKPIPKJMAPAAIAPMLPACPPPMPDCFHEADGPBPDMPDDCDOGABKFANEBMNNPFIAPGIKAPIBCPPADADPPCPJHCBPPCMHPKOCMDDPBPPIPABLFPPMNCFPAAPBOKPJPAEKJHPCEABEPBPFPPBGIHAPGNPHPBABFKMPGEPLGBLDCBCPBPLPEPBAFBPBBBDCAIAAABKCPCFPDPGIFPJPFCDPLPMKPPENPCPPBJHMPLPOBEPGAHBNFPPAMBHDLKCOADPDPHPBOEPPDPDIHDEKPPAMIPBPDKFJPPLAIPPNHPAPPACPACPBPPIPODACDKAHCPDBPPAPAAGDOPBPEPDAAEKCPPAKBBPAOIPDPOGNEDJFDLA'}}}}}]}}  # noqa

    def test_search(self, login, zmlp_project_user, monkeypatch,
                    api_client, response_data, project):

        def mock_data(*args, **kwargs):
            body = args[-1]
            assert body['from'] == 0
            assert body['size'] == 1
            assert body['query']['prefix']['source.mimetype']['value'] == 'image'
            return response_data

        monkeypatch.setattr(ZmlpClient, 'post', mock_data)
        body = {
            "size": 1,
            "query": {
                "prefix": {
                    "source.mimetype": {
                        "value": "image"
                        }
                    }
                }
            }
        response = api_client.post(reverse('asset-search', kwargs={'project_pk': project.id}), body)
        search_results = response.json()['results']
        assert response.status_code == status.HTTP_200_OK
        assert search_results[0]['id'] == 'XK6ra3Jl_xhtrqP0Nf8hNJ7JcIRfWXCE'
        assert search_results[0]['metadata']['source']['mimetype'] == 'image/tiff'

    def test_search_no_query(self, login, zmlp_project_user, monkeypatch,
                             api_client, response_data, project):
        def mock_data(*args, **kwargs):
            body = args[-1]
            assert body['from'] == 2
            assert body['size'] == 1
            assert 'query' not in body
            return response_data

        monkeypatch.setattr(ZmlpClient, 'post', mock_data)
        body = {
            "size": 1,
            "from": 2
            }
        response = api_client.post(reverse('asset-search', kwargs={'project_pk': project.id}), body)
        search_results = response.json()['results']
        assert response.status_code == status.HTTP_200_OK
        assert search_results[0]['id'] == 'XK6ra3Jl_xhtrqP0Nf8hNJ7JcIRfWXCE'
        assert search_results[0]['metadata']['source']['mimetype'] == 'image/tiff'

    def test_search_addtl_terms(self, login, zmlp_project_user, monkeypatch,
                                api_client, response_data, project):
        def mock_data(*args, **kwargs):
            body = args[-1]
            assert body['timeout'] == 10
            assert body['collapse'] == 'true'
            assert body['suggest'] == 'suggestion'
            assert body['aggs'] == {'some': {'cool': 'stuff'}}
            assert body['size'] == 1
            assert 'query' not in body
            assert 'extra' not in body
            return response_data

        monkeypatch.setattr(ZmlpClient, 'post', mock_data)
        body = {
            "size": 1,
            "from": 2,
            "timeout": 10,
            'collapse': 'true',
            'suggest': 'suggestion',
            'aggs': {'some': {'cool': 'stuff'}}
        }
        response = api_client.post(reverse('asset-search', kwargs={'project_pk': project.id}), body)
        assert response.status_code == status.HTTP_200_OK


class TestSourceFileViewSet:

    def test_get_source(self, project, zvi_project_user, api_client, monkeypatch):

        def mock_streamer(*args, **kwargs):
            for x in range(0, 1024):
                yield x

        monkeypatch.setattr(requests, 'get', mock_streamer)
        api_client.force_authenticate(zvi_project_user)
        api_client.force_login(zvi_project_user)
        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        filename = 'TIFF_1MB.tiff'
        response = api_client.get(reverse('source-detail', kwargs={'project_pk': project.id,
                                                                   'asset_pk': asset_id,
                                                                   'pk': filename}))
        assert isinstance(response, StreamingHttpResponse)


class TestFileNameViewSet:

    def test_get_proxy(self, project, zvi_project_user, api_client, monkeypatch):

        def mock_streamer(*args, **kwargs):
            for x in range(0, 1024):
                yield x

        monkeypatch.setattr(requests, 'get', mock_streamer)
        api_client.force_authenticate(zvi_project_user)
        api_client.force_login(zvi_project_user)
        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        filename = 'TIFF_1MB.tiff'
        response = api_client.get(reverse('file_name-detail', kwargs={'project_pk': project.id,
                                                                      'asset_pk': asset_id,
                                                                      'category_pk': 'proxy',
                                                                      'pk': filename}))
        assert isinstance(response, StreamingHttpResponse)


class TestMetadataExportView:

    def test_post(self, login, api_client, monkeypatch, project):

        def mock_search_for_assets(*args, **kwargs):
            return [
                Asset({'id': '1', 'document': {'resolution': {'width': 10, 'height': 10}}}),
                Asset({'id': '2', 'document': {'resolution': {'width': 20, 'height': 20}}}),
                Asset({'id': '4', 'document': {'resolution': {'width': 30, 'height': 30},
                                               'extra_field': True}}),
            ]

        monkeypatch.setattr(MetadataExportViewSet, '_search_for_assets', mock_search_for_assets)
        result = api_client.post(reverse('export-list', kwargs={'project_pk': project.id}), {})
        assert result.status_code == 200
        assert result.accepted_media_type == 'text/csv'
        assert result.content == b'extra_field,id,resolution.height,resolution.width\r\n,1,10,10\r\n,2,20,20\r\nTrue,4,30,30\r\n'  # noqa
        assert result.charset == 'utf-8'
