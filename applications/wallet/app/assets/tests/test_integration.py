import pytest
import requests
from django.http import StreamingHttpResponse
from django.urls import reverse
from rest_framework import status
from rest_framework.response import Response
from boonsdk import BoonClient

from assets.utils import AssetBoxImager
from assets.views import AssetViewSet
from searches.filters import LabelConfidenceFilter
from wallet.tests.utils import check_response

pytestmark = pytest.mark.django_db


@pytest.fixture
def list_api_return():
    return {"took":8,"timed_out":False,"_shards":{"total":2,"successful":2,"skipped":0,"failed":0},"hits":{"total":{"value":2,"relation":"eq"},"max_score":1.0,"hits":[{"_index":"4w9bajznsa1yxbo4","_type":"_doc","_id":"vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C","_score":1.0,"_source":{"system":{"jobId":"8d2603f7-00d4-132f-8681-0242ac120009","dataSourceId":"8d2603f6-00d4-132f-8681-0242ac120009","timeCreated":"2020-03-03T21:54:02.002039Z","state":"Analyzed","projectId":"00000000-0000-0000-0000-000000000000","taskId":"8d2603f8-00d4-132f-8681-0242ac120009","timeModified":"2020-03-03T21:54:23.978500Z"},"source":{"path":"gs://zorroa-dev-data/image/TIFF_1MB.tiff","extension":"tiff","filename":"TIFF_1MB.tiff","mimetype":"image/tiff","filesize":1131930,"checksum":1867533868},"metrics":{"pipeline":[{"processor":"boonai_core.core.processors.PreCacheSourceFileProcessor","module":"standard","checksum":1621235190,"executionTime":0.52,"executionDate":"2020-03-03T21:54:04.185632"},{"processor":"boonai_core.image.importers.ImageImporter","module":"standard","checksum":1426657387,"executionTime":0.5,"executionDate":"2020-03-03T21:54:06.820102"},{"processor":"boonai_core.office.importers.OfficeImporter","module":"standard","checksum":2001473853},{"processor":"boonai_core.video.VideoImporter","module":"standard","checksum":3310423168},{"processor":"boonai_core.core.processors.AssertAttributesProcessor","module":"standard","checksum":1841569083,"executionTime":0.0,"executionDate":"2020-03-03T21:54:08.449234"},{"processor":"boonai_core.proxy.ImageProxyProcessor","module":"standard","checksum":457707303,"executionTime":0.89,"executionDate":"2020-03-03T21:54:09.394490"},{"processor":"boonai_core.proxy.VideoProxyProcessor","module":"standard","checksum":482873147},{"processor":"boonai_analysis.mxnet.ZviSimilarityProcessor","module":"standard","checksum":2479952423,"executionTime":2.07,"executionDate":"2020-03-03T21:54:20.533214"}]},"media":{"width":650,"height":434,"aspect":1.5,"orientation":"landscape","type":"image","length":1},"clip":{"type":"page","start":1.0,"stop":1.0,"length":1.0,"pile":"pUn6wBxUN7x9JxOxLkvruOyNdYA","sourceAssetId":"vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C"},"files":[{"name":"image_650x434.jpg","category":"proxy","mimetype":"image/jpeg","size":89643,"attrs":{"width":650,"height":434}},{"name":"image_512x341.jpg","category":"proxy","mimetype":"image/jpeg","size":60713,"attrs":{"width":512,"height":341}},{"name":"image_320x213.jpg","category":"proxy","mimetype":"image/jpeg","size":30882,"attrs":{"width":320,"height":213}}],"analysis":{"zvi":{"tinyProxy":["#f3dfc3","#f4efd8","#c18f46","#ebdfbd","#ccd3c0","#e7d4bb","#beae8e","#cabf9e","#d2c09c"],"similarity":{"simhash":"PBPBFHAOBGAHCDGNEBDDCGPDCP"}}}}},{"_index":"4w9bajznsa1yxbo4","_type":"_doc","_id":"XK6ra3Jl_xhtrqP0Nf8hNJ7JcIRfWXCE","_score":1.0,"_source":{"system":{"jobId":"8d2603f7-00d4-132f-8681-0242ac120009","dataSourceId":"8d2603f6-00d4-132f-8681-0242ac120009","timeCreated":"2020-03-03T21:54:02.149855Z","state":"Analyzed","projectId":"00000000-0000-0000-0000-000000000000","taskId":"8d2603f8-00d4-132f-8681-0242ac120009","timeModified":"2020-03-03T21:54:23.981546Z"},"source":{"path":"gs://zorroa-dev-data/image/mulipage.tif","extension":"tif","filename":"mulipage.tif","mimetype":"image/tiff","filesize":810405,"checksum":166113922},"metrics":{"pipeline":[{"processor":"boonai_core.core.processors.PreCacheSourceFileProcessor","module":"standard","checksum":1621235190,"executionTime":0.41,"executionDate":"2020-03-03T21:54:04.594088"},{"processor":"boonai_core.image.importers.ImageImporter","module":"standard","checksum":1426657387,"executionTime":0.29,"executionDate":"2020-03-03T21:54:07.112204"},{"processor":"boonai_core.office.importers.OfficeImporter","module":"standard","checksum":2001473853},{"processor":"boonai_core.video.VideoImporter","module":"standard","checksum":3310423168},{"processor":"boonai_core.core.processors.AssertAttributesProcessor","module":"standard","checksum":1841569083,"executionTime":0.0,"executionDate":"2020-03-03T21:54:08.451394"},{"processor":"boonai_core.proxy.ImageProxyProcessor","module":"standard","checksum":457707303,"executionTime":0.67,"executionDate":"2020-03-03T21:54:10.067415"},{"processor":"boonai_core.proxy.VideoProxyProcessor","module":"standard","checksum":482873147},{"processor":"boonai_analysis.mxnet.ZviSimilarityProcessor","module":"standard","checksum":2479952423,"executionTime":0.49,"executionDate":"2020-03-03T21:54:21.033848"}]},"media":{"width":800,"height":600,"aspect":1.33,"orientation":"landscape","type":"image","length":10},"clip":{"type":"page","start":1.0,"stop":1.0,"length":1.0,"pile":"vQz9dxEUpYvg0AVkXQV_xt2m2tg","sourceAssetId":"XK6ra3Jl_xhtrqP0Nf8hNJ7JcIRfWXCE"},"files":[{"name":"image_800x600.jpg","category":"proxy","mimetype":"image/jpeg","size":78408,"attrs":{"width":800,"height":600}},{"name":"image_512x384.jpg","category":"proxy","mimetype":"image/jpeg","size":42889,"attrs":{"width":512,"height":384}},{"name":"image_320x240.jpg","category":"proxy","mimetype":"image/jpeg","size":20676,"attrs":{"width":320,"height":240}}],"analysis":{"zvi":{"tinyProxy":["#f9f9f9","#ffffff","#ffffff","#dddddd","#d3d3d3","#ffffff","#ffffff","#ffffff","#ffffff"],"similarity":{"simhash":"AEKCPPAKBBPAOIPDPOGNEDJFDLA"}}}}}]}}  # noqa


@pytest.fixture
def detail_api_return():
    return {"id":"vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C","document":{"system":{"jobId":"8d2603f7-00d4-132f-8681-0242ac120009","dataSourceId":"8d2603f6-00d4-132f-8681-0242ac120009","timeCreated":"2020-03-03T21:54:02.002039Z","state":"Analyzed","projectId":"00000000-0000-0000-0000-000000000000","timeModified":"2020-03-03T21:54:23.978500Z","taskId":"8d2603f8-00d4-132f-8681-0242ac120009"},"files":[{"size":89643,"name":"image_650x434.jpg","mimetype":"image/jpeg","category":"proxy","attrs":{"width":650,"height":434}},{"size":60713,"name":"image_512x341.jpg","mimetype":"image/jpeg","category":"proxy","attrs":{"width":512,"height":341}},{"size":30882,"name":"image_320x213.jpg","mimetype":"image/jpeg","category":"proxy","attrs":{"width":320,"height":213}}],"source":{"path":"gs://zorroa-dev-data/image/TIFF_1MB.tiff","extension":"tiff","filename":"TIFF_1MB.tiff","checksum":1867533868,"mimetype":"image/tiff","filesize":1131930},"metrics":{"pipeline":[{"executionTime":0.52,"module":"standard","checksum":1621235190,"executionDate":"2020-03-03T21:54:04.185632","processor":"boonai_core.core.processors.PreCacheSourceFileProcessor"},{"executionTime":0.5,"module":"standard","checksum":1426657387,"executionDate":"2020-03-03T21:54:06.820102","processor":"boonai_core.image.importers.ImageImporter"},{"module":"standard","checksum":2001473853,"processor":"boonai_core.office.importers.OfficeImporter"},{"module":"standard","checksum":3310423168,"processor":"boonai_core.video.VideoImporter"},{"executionTime":0.0,"module":"standard","checksum":1841569083,"executionDate":"2020-03-03T21:54:08.449234","processor":"boonai_core.core.processors.AssertAttributesProcessor"},{"executionTime":0.89,"module":"standard","checksum":457707303,"executionDate":"2020-03-03T21:54:09.394490","processor":"boonai_core.proxy.ImageProxyProcessor"},{"module":"standard","checksum":482873147,"processor":"boonai_core.proxy.VideoProxyProcessor"},{"executionTime":2.07,"module":"standard","checksum":2479952423,"executionDate":"2020-03-03T21:54:20.533214","processor":"boonai_analysis.mxnet.ZviSimilarityProcessor"}]},"media":{"orientation":"landscape","aspect":1.5,"width":650,"length":1,"type":"image","height":434},"analysis":{"zvi":{"similarity":{"simhash":"PBPBFHAOBGAHCDGNEBDDCGPDCP"},"tinyProxy":["#f3dfc3","#f4efd8","#c18f46","#ebdfbd","#ccd3c0","#e7d4bb","#beae8e","#cabf9e","#d2c09c"]}},"clip":{"sourceAssetId":"vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C","stop":1.0,"pile":"pUn6wBxUN7x9JxOxLkvruOyNdYA","start":1.0,"length":1.0,"type":"page"}},"analyzed":True}  # noqa


class TestAssetViewSet:

    def test_get_list(self, project, zmlp_project_user, api_client, monkeypatch, list_api_return):

        def mock_api_response(*args, **kwargs):
            return list_api_return

        monkeypatch.setattr(BoonClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('asset-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        content = response.json()
        assert content['count'] == 2
        assert len(content['results']) == 2
        assert 'next' in content
        assert 'previous' in content

    def test_get_detail(self, project, zmlp_project_user, api_client, monkeypatch,
                        detail_api_return):

        def mock_api_response(*args, **kwargs):
            return detail_api_return

        monkeypatch.setattr(BoonClient, 'get', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        response = api_client.get(reverse('asset-detail', kwargs={'project_pk': project.id,
                                                                  'pk': id}))
        assert response.status_code == 200
        content = response.json()
        assert content['id'] == id
        assert 'metadata' in content

    def test_list_and_detail_resources_match(self, project, zmlp_project_user, api_client,
                                             monkeypatch, detail_api_return, list_api_return):

        def mock_list_response(*args, **kwargs):
            return list_api_return

        def mock_detail_response(*args, **kwargs):
            return detail_api_return

        monkeypatch.setattr(BoonClient, 'post', mock_list_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('asset-list', kwargs={'project_pk': project.id}))
        list_content = response.json()

        monkeypatch.setattr(BoonClient, 'get', mock_detail_response)
        id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        response = api_client.get(reverse('asset-detail', kwargs={'project_pk': project.id,
                                                                  'pk': id}))
        detail_content = response.json()

        assert list_content['results'][0] == detail_content

    def test_delete(self, login, project, api_client, monkeypatch):
        id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

        def mock_response(*args, **kwargs):
            return {'type': 'asset',
                    'id': 'delete',
                    'op': id,
                    'success': True}

        monkeypatch.setattr(BoonClient, 'delete', mock_response)
        response = api_client.delete(reverse('asset-detail', kwargs={'project_pk': project.id,
                                                                     'pk': id}))
        check_response(response)

    def test_bad_delete(self, login, project, api_client, monkeypatch):
        id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

        def mock_response(*args, **kwargs):
            return {'type': 'asset',
                    'id': 'delete',
                    'op': id,
                    'success': False}

        monkeypatch.setattr(BoonClient, 'delete', mock_response)
        response = api_client.delete(reverse('asset-detail', kwargs={'project_pk': project.id,
                                                                     'pk': id}))
        content = check_response(response, status.HTTP_500_INTERNAL_SERVER_ERROR)
        assert content['detail'] == ['Resource deletion failed.']

    def test_signed_url(self, login, project, api_client, monkeypatch):

        def mock_detail_response(*args, **kwargs):
            return Response(status=status.HTTP_200_OK, data={'id': 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C', 'metadata': {'system': {'jobId': '8d2603f7-00d4-132f-8681-0242ac120009', 'dataSourceId': '8d2603f6-00d4-132f-8681-0242ac120009', 'timeCreated': '2020-03-03T21:54:02.002039Z', 'state': 'Analyzed', 'projectId': '00000000-0000-0000-0000-000000000000', 'timeModified': '2020-03-03T21:54:23.978500Z', 'taskId': '8d2603f8-00d4-132f-8681-0242ac120009'}, 'files': [{'id': 'this/is/the/id.jpg', 'size': 89643, 'name': 'image_650x434.jpg', 'mimetype': 'image/jpeg', 'category': 'proxy', 'attrs': {'width': 650, 'height': 434}}, {'size': 60713, 'name': 'image_512x341.jpg', 'mimetype': 'image/jpeg', 'category': 'proxy', 'attrs': {'width': 512, 'height': 341}}, {'size': 30882, 'name': 'image_320x213.jpg', 'mimetype': 'image/jpeg', 'category': 'proxy', 'attrs': {'width': 320, 'height': 213}}], 'source': {'path': 'gs://zorroa-dev-data/image/TIFF_1MB.tiff', 'extension': 'tiff', 'filename': 'TIFF_1MB.tiff', 'checksum': 1867533868, 'mimetype': 'image/tiff', 'filesize': 1131930}, 'metrics': {'pipeline': [{'executionTime': 0.52, 'module': 'standard', 'checksum': 1621235190, 'executionDate': '2020-03-03T21:54:04.185632', 'processor': 'boonai_core.core.processors.PreCacheSourceFileProcessor'}, {'executionTime': 0.5, 'module': 'standard', 'checksum': 1426657387, 'executionDate': '2020-03-03T21:54:06.820102', 'processor': 'boonai_core.image.importers.ImageImporter'}, {'module': 'standard', 'checksum': 2001473853, 'processor': 'boonai_core.office.importers.OfficeImporter'}, {'module': 'standard', 'checksum': 3310423168, 'processor': 'boonai_core.video.VideoImporter'}, {'executionTime': 0.0, 'module': 'standard', 'checksum': 1841569083, 'executionDate': '2020-03-03T21:54:08.449234', 'processor': 'boonai_core.core.processors.AssertAttributesProcessor'}, {'executionTime': 0.89, 'module': 'standard', 'checksum': 457707303, 'executionDate': '2020-03-03T21:54:09.394490', 'processor': 'boonai_core.proxy.ImageProxyProcessor'}, {'module': 'standard', 'checksum': 482873147, 'processor': 'boonai_core.proxy.VideoProxyProcessor'}, {'executionTime': 2.07, 'module': 'standard', 'checksum': 2479952423, 'executionDate': '2020-03-03T21:54:20.533214', 'processor': 'boonai_analysis.mxnet.ZviSimilarityProcessor'}]}, 'media': {'orientation': 'landscape', 'aspect': 1.5, 'width': 650, 'length': 1, 'type': 'image', 'height': 434}, 'analysis': {'zvi': {'similarity': {'simhash': 'PBPBFHAOBGAHCDGNEBDDCGPDCP'}, 'tinyProxy': ['#f3dfc3', '#f4efd8', '#c18f46', '#ebdfbd', '#ccd3c0', '#e7d4bb', '#beae8e', '#cabf9e', '#d2c09c']}}, 'clip': {'sourceAssetId': 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C', 'stop': 1.0, 'pile': 'pUn6wBxUN7x9JxOxLkvruOyNdYA', 'start': 1.0, 'length': 1.0, 'type': 'page'}}})  # noqa

        monkeypatch.setattr(AssetViewSet, 'retrieve', mock_detail_response)

        def mock_response(*args, **kwargs):
            return {
                'uri': 'http://minio:9000/project-storage/projects/00000000-0000-0000-0000-000000000000/assets/AjXYVpaVeLsOgpenKKSW8oDB5YuOTWDs/web-proxy/web-proxy.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20200602T013235Z&X-Amz-SignedHeaders=host&X-Amz-Expires=3599&X-Amz-Credential=qwerty123%2F20200602%2FUS_WEST_2%2Fs3%2Faws4_request&X-Amz-Signature=acbf9a9b0668b29315f262713742648c1943299e63cb1ea5e6145cf27ad4f95f',  # noqa
                'mediaType': 'image/jpeg'}

        monkeypatch.setattr(BoonClient, 'get', mock_response)
        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        response = api_client.get(reverse('asset-signed-url',
                                          kwargs={'project_pk': project.id,
                                                  'pk': asset_id}))
        content = check_response(response)
        assert 'uri' in content
        assert 'mediaType' in content

    def test_signed_url_with_fallback(self, login, project, api_client, monkeypatch):

        def mock_detail_response(*args, **kwargs):
            return Response(status=status.HTTP_200_OK, data={'id': 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C', 'metadata': {'system': {'jobId': '8d2603f7-00d4-132f-8681-0242ac120009', 'dataSourceId': '8d2603f6-00d4-132f-8681-0242ac120009', 'timeCreated': '2020-03-03T21:54:02.002039Z', 'state': 'Analyzed', 'projectId': '00000000-0000-0000-0000-000000000000', 'timeModified': '2020-03-03T21:54:23.978500Z', 'taskId': '8d2603f8-00d4-132f-8681-0242ac120009'}, 'files': [], 'source': {'path': 'gs://zorroa-dev-data/image/TIFF_1MB.tiff', 'extension': 'tiff', 'filename': 'TIFF_1MB.tiff', 'checksum': 1867533868, 'mimetype': 'image/tiff', 'filesize': 1131930}, 'metrics': {'pipeline': [{'executionTime': 0.52, 'module': 'standard', 'checksum': 1621235190, 'executionDate': '2020-03-03T21:54:04.185632', 'processor': 'boonai_core.core.processors.PreCacheSourceFileProcessor'}, {'executionTime': 0.5, 'module': 'standard', 'checksum': 1426657387, 'executionDate': '2020-03-03T21:54:06.820102', 'processor': 'boonai_core.image.importers.ImageImporter'}, {'module': 'standard', 'checksum': 2001473853, 'processor': 'boonai_core.office.importers.OfficeImporter'}, {'module': 'standard', 'checksum': 3310423168, 'processor': 'boonai_core.video.VideoImporter'}, {'executionTime': 0.0, 'module': 'standard', 'checksum': 1841569083, 'executionDate': '2020-03-03T21:54:08.449234', 'processor': 'boonai_core.core.processors.AssertAttributesProcessor'}, {'executionTime': 0.89, 'module': 'standard', 'checksum': 457707303, 'executionDate': '2020-03-03T21:54:09.394490', 'processor': 'boonai_core.proxy.ImageProxyProcessor'}, {'module': 'standard', 'checksum': 482873147, 'processor': 'boonai_core.proxy.VideoProxyProcessor'}, {'executionTime': 2.07, 'module': 'standard', 'checksum': 2479952423, 'executionDate': '2020-03-03T21:54:20.533214', 'processor': 'boonai_analysis.mxnet.ZviSimilarityProcessor'}]}, 'media': {'orientation': 'landscape', 'aspect': 1.5, 'width': 650, 'length': 1, 'type': 'image', 'height': 434}, 'analysis': {'zvi': {'similarity': {'simhash': 'PBPBFHAOBGAHCDGNEBDDCGPDCP'}, 'tinyProxy': ['#f3dfc3', '#f4efd8', '#c18f46', '#ebdfbd', '#ccd3c0', '#e7d4bb', '#beae8e', '#cabf9e', '#d2c09c']}}, 'clip': {'sourceAssetId': 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C', 'stop': 1.0, 'pile': 'pUn6wBxUN7x9JxOxLkvruOyNdYA', 'start': 1.0, 'length': 1.0, 'type': 'page'}}})  # noqa

        monkeypatch.setattr(AssetViewSet, 'retrieve', mock_detail_response)

        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        response = api_client.get(reverse('asset-signed-url',
                                          kwargs={'project_pk': project.id,
                                                  'pk': asset_id}))
        content = check_response(response)
        assert content['uri'] == '/icons/fallback_3x.png'
        assert content['mediaType'] == 'image/png'


class TestTimelines:

    def test_timelines_logged_out(self, project, api_client):
        asset_id = '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5'
        response = api_client.get(reverse('asset-timelines',
                                          kwargs={'project_pk': project.id,
                                                  'pk': asset_id}))
        content = check_response(response, status.HTTP_403_FORBIDDEN)
        assert content == {'detail': ['Unauthorized.']}

    def test_timelines_empty_response(self, login, project, api_client, monkeypatch):

        def mock_response(*args, **kwargs):
            return []  # noqa

        monkeypatch.setattr(AssetViewSet, '_zmlp_get_all_content_from_es_search', mock_response)
        asset_id = '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5'
        response = api_client.get(reverse('asset-timelines',
                                          kwargs={'project_pk': project.id,
                                                  'pk': asset_id}))
        content = check_response(response)
        assert content == []

    def test_timeline(self, login, project, api_client, monkeypatch):

        def mock_response(*args, **kwargs):
            return [{'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'gY6ZV7AVKyIAGdcDopKJy6S4Jl2LKNQX', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.904, 'stop': 19.453, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 0.033, 'length': 19.42, 'timeline': 'gcp-video-logo-detection', 'track': 'AAMCO Transmissions', 'content': ['AAMCO Transmissions']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'e7sRs6KWejPJwScDB9VipWDWXoyA8m3E', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.967, 'stop': 2.936, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 1.735, 'length': 1.201, 'timeline': 'gcp-video-logo-detection', 'track': 'Patagonia', 'content': ['Patagonia']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'ShVFwPsVKP7h76Ml30x4qh3zoSrEFQgc', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.967, 'stop': 9.743, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 3.537, 'length': 6.206, 'timeline': 'gcp-video-logo-detection', 'track': 'Patagonia', 'content': ['Patagonia']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'bM62oaRK82OzUUNpjCyA3Xo2mf0tvQpU', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.967, 'stop': 10.344, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 10.244, 'length': 0.1, 'timeline': 'gcp-video-logo-detection', 'track': 'Patagonia', 'content': ['Patagonia']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': '0jtcS42wmk6dwSbRiW3Oi9v46QY4W9Yz', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.967, 'stop': 13.847, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 13.447, 'length': 0.4, 'timeline': 'gcp-video-logo-detection', 'track': 'Patagonia', 'content': ['Patagonia']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'b4lo4jKOhU0lyqPJ1NTQCnOZGLO9TOwt', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.967, 'stop': 15.048, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 14.748, 'length': 0.3, 'timeline': 'gcp-video-logo-detection', 'track': 'Patagonia', 'content': ['Patagonia']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'hFAwdiwtZs0WovYQ2RhOTREHUVy8KQud', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.967, 'stop': 19.453, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 15.949, 'length': 3.504, 'timeline': 'gcp-video-logo-detection', 'track': 'Patagonia', 'content': ['Patagonia']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': '43fHeEBKapcIOEQQyhiNNMrvd2zBY1hl', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.885, 'stop': 1.935, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 0.033, 'length': 1.902, 'timeline': 'gcp-video-object-detection', 'track': 'person', 'content': ['person']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'R83rxMfDt3_cOLjR7IqH-wjx0366YnzG', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.872, 'stop': 0.334, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 0.033, 'length': 0.301, 'timeline': 'gcp-video-object-detection', 'track': 'car', 'content': ['car']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'l38Em6MQkL4KjQsNTmNVW-2ePau2ZSUF', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.847, 'stop': 1.935, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 1.635, 'length': 0.3, 'timeline': 'gcp-video-object-detection', 'track': 'car', 'content': ['car']}}}]

        monkeypatch.setattr(AssetViewSet, '_zmlp_get_all_content_from_es_search', mock_response)
        asset_id = '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5'
        response = api_client.get(reverse('asset-timelines',
                                          kwargs={'project_pk': project.id,
                                                  'pk': asset_id}))
        content = check_response(response)
        assert content == [
            {'timeline': 'gcp-video-logo-detection',
             'tracks': [
                 {
                     'track': 'AAMCO Transmissions',
                     'hits': [
                         {'start': 0.033, 'stop': 19.453, 'highlight': False}
                     ]
                 },
                 {
                     'track': 'Patagonia',
                     'hits': [
                         {'start': 1.735, 'stop': 2.936, 'highlight': False},
                         {'start': 3.537, 'stop': 9.743, 'highlight': False},
                         {'start': 10.244, 'stop': 10.344, 'highlight': False},
                         {'start': 13.447, 'stop': 13.847, 'highlight': False},
                         {'start': 14.748, 'stop': 15.048, 'highlight': False},
                         {'start': 15.949, 'stop': 19.453, 'highlight': False}
                     ]
                 }
             ]},
            {'timeline': 'gcp-video-object-detection',
             'tracks': [
                 {
                     'track': 'person',
                     'hits': [
                         {'start': 0.033, 'stop': 1.935, 'highlight': False}
                     ]
                 },
                 {
                     'track': 'car',
                     'hits': [
                         {'start': 0.033, 'stop': 0.334, 'highlight': False},
                         {'start': 1.635, 'stop': 1.935, 'highlight': False}
                     ]
                 }
             ]}
        ]

    def test_timelines_with_query(self, login, project, api_client, monkeypatch):

        def mock_response(*args, **kwargs):
            if kwargs.get('search_filter'):
                return [{"_index":"psqvrhj2nweamqb5","_type":"_doc","_id":"gY6ZV7AVKyIAGdcDopKJy6S4Jl2LKNQX","_score":0.0,"_routing":"161cSlllD5EP-mma5nw1Rk_xDDLVDrs5","_source":{"clip":{"score":0.904,"stop":19.453,"assetId":"161cSlllD5EP-mma5nw1Rk_xDDLVDrs5","start":0.033,"length":19.42,"timeline":"gcp-video-logo-detection","track":"AAMCO Transmissions","content":["AAMCO Transmissions"]}}}]  # noqa
            else:
                return [{'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'gY6ZV7AVKyIAGdcDopKJy6S4Jl2LKNQX', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.904, 'stop': 19.453, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 0.033, 'length': 19.42, 'timeline': 'gcp-video-logo-detection', 'track': 'AAMCO Transmissions', 'content': ['AAMCO Transmissions']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'e7sRs6KWejPJwScDB9VipWDWXoyA8m3E', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.967, 'stop': 2.936, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 1.735, 'length': 1.201, 'timeline': 'gcp-video-logo-detection', 'track': 'Patagonia', 'content': ['Patagonia']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'ShVFwPsVKP7h76Ml30x4qh3zoSrEFQgc', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.967, 'stop': 9.743, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 3.537, 'length': 6.206, 'timeline': 'gcp-video-logo-detection', 'track': 'Patagonia', 'content': ['Patagonia']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'bM62oaRK82OzUUNpjCyA3Xo2mf0tvQpU', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.967, 'stop': 10.344, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 10.244, 'length': 0.1, 'timeline': 'gcp-video-logo-detection', 'track': 'Patagonia', 'content': ['Patagonia']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': '0jtcS42wmk6dwSbRiW3Oi9v46QY4W9Yz', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.967, 'stop': 13.847, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 13.447, 'length': 0.4, 'timeline': 'gcp-video-logo-detection', 'track': 'Patagonia', 'content': ['Patagonia']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'b4lo4jKOhU0lyqPJ1NTQCnOZGLO9TOwt', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.967, 'stop': 15.048, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 14.748, 'length': 0.3, 'timeline': 'gcp-video-logo-detection', 'track': 'Patagonia', 'content': ['Patagonia']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'hFAwdiwtZs0WovYQ2RhOTREHUVy8KQud', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.967, 'stop': 19.453, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 15.949, 'length': 3.504, 'timeline': 'gcp-video-logo-detection', 'track': 'Patagonia', 'content': ['Patagonia']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': '43fHeEBKapcIOEQQyhiNNMrvd2zBY1hl', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.885, 'stop': 1.935, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 0.033, 'length': 1.902, 'timeline': 'gcp-video-object-detection', 'track': 'person', 'content': ['person']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'R83rxMfDt3_cOLjR7IqH-wjx0366YnzG', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.872, 'stop': 0.334, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 0.033, 'length': 0.301, 'timeline': 'gcp-video-object-detection', 'track': 'car', 'content': ['car']}}}, {'_index': 'psqvrhj2nweamqb5', '_type': '_doc', '_id': 'l38Em6MQkL4KjQsNTmNVW-2ePau2ZSUF', '_score': 0.0, '_routing': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', '_source': {'clip': {'score': 0.847, 'stop': 1.935, 'assetId': '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5', 'start': 1.635, 'length': 0.3, 'timeline': 'gcp-video-object-detection', 'track': 'car', 'content': ['car']}}}]  # noqa

        # def field_type_response(*args, **kwargs):
        #     return 'prediction'

        monkeypatch.setattr(AssetViewSet, '_zmlp_get_all_content_from_es_search', mock_response)
        monkeypatch.setattr(LabelConfidenceFilter, 'field_type', 'prediction')
        asset_id = '161cSlllD5EP-mma5nw1Rk_xDDLVDrs5'
        querystring = 'W3sidHlwZSI6ImxhYmVsQ29uZmlkZW5jZSIsImF0dHJpYnV0ZSI6ImFuYWx5c2lzLmdjcC12aWRlby1sb2dvLWRldGVjdGlvbiIsInZhbHVlcyI6eyJsYWJlbHMiOlsiQUFNQ08gVHJhbnNtaXNzaW9ucyJdLCJtaW4iOjAsIm1heCI6MX19XQ=='
        response = api_client.get(reverse('asset-timelines',
                                          kwargs={'project_pk': project.id,
                                                  'pk': asset_id}), {'query': querystring})
        content = check_response(response)
        assert content == [
            {
                "timeline": "gcp-video-logo-detection",
                "tracks": [
                    {
                        "track": "AAMCO Transmissions",
                        "hits": [
                            {
                                "start": 0.033,
                                "stop": 19.453,
                                "highlight": True
                            }
                        ]
                    },
                    {
                        "track": "Patagonia",
                        "hits": [
                            {
                                "start": 1.735,
                                "stop": 2.936,
                                "highlight": False
                            },
                            {
                                "start": 3.537,
                                "stop": 9.743,
                                "highlight": False
                            },
                            {
                                "start": 10.244,
                                "stop": 10.344,
                                "highlight": False
                            },
                            {
                                "start": 13.447,
                                "stop": 13.847,
                                "highlight": False
                            },
                            {
                                "start": 14.748,
                                "stop": 15.048,
                                "highlight": False
                            },
                            {
                                "start": 15.949,
                                "stop": 19.453,
                                "highlight": False
                            }
                        ]
                    }
                ]
            },
            {
                "timeline": "gcp-video-object-detection",
                "tracks": [
                    {
                        "track": "person",
                        "hits": [
                            {
                                "start": 0.033,
                                "stop": 1.935,
                                "highlight": False
                            }
                        ]
                    },
                    {
                        "track": "car",
                        "hits": [
                            {
                                "start": 0.033,
                                "stop": 0.334,
                                "highlight": False
                            },
                            {
                                "start": 1.635,
                                "stop": 1.935,
                                "highlight": False
                            }
                        ]
                    }
                ]
            }
        ]


class TestUrls:

    def test_signed_url(self, login, project, api_client, monkeypatch):

        def mock_detail_response(*args, **kwargs):
            return Response(status=status.HTTP_200_OK, data={'id': 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C', 'metadata': {'system': {'jobId': '8d2603f7-00d4-132f-8681-0242ac120009', 'dataSourceId': '8d2603f6-00d4-132f-8681-0242ac120009', 'timeCreated': '2020-03-03T21:54:02.002039Z', 'state': 'Analyzed', 'projectId': '00000000-0000-0000-0000-000000000000', 'timeModified': '2020-03-03T21:54:23.978500Z', 'taskId': '8d2603f8-00d4-132f-8681-0242ac120009'}, 'files': [{'id': 'this/is/the/id.jpg', 'size': 89643, 'name': 'image_650x434.jpg', 'mimetype': 'image/jpeg', 'category': 'proxy', 'attrs': {'width': 650, 'height': 434}}, {'size': 60713, 'name': 'image_512x341.jpg', 'mimetype': 'image/jpeg', 'category': 'proxy', 'attrs': {'width': 512, 'height': 341}}, {'size': 30882, 'name': 'image_320x213.jpg', 'mimetype': 'image/jpeg', 'category': 'proxy', 'attrs': {'width': 320, 'height': 213}}], 'source': {'path': 'gs://zorroa-dev-data/image/TIFF_1MB.tiff', 'extension': 'tiff', 'filename': 'TIFF_1MB.tiff', 'checksum': 1867533868, 'mimetype': 'image/tiff', 'filesize': 1131930}, 'metrics': {'pipeline': [{'executionTime': 0.52, 'module': 'standard', 'checksum': 1621235190, 'executionDate': '2020-03-03T21:54:04.185632', 'processor': 'boonai_core.core.processors.PreCacheSourceFileProcessor'}, {'executionTime': 0.5, 'module': 'standard', 'checksum': 1426657387, 'executionDate': '2020-03-03T21:54:06.820102', 'processor': 'boonai_core.image.importers.ImageImporter'}, {'module': 'standard', 'checksum': 2001473853, 'processor': 'boonai_core.office.importers.OfficeImporter'}, {'module': 'standard', 'checksum': 3310423168, 'processor': 'boonai_core.video.VideoImporter'}, {'executionTime': 0.0, 'module': 'standard', 'checksum': 1841569083, 'executionDate': '2020-03-03T21:54:08.449234', 'processor': 'boonai_core.core.processors.AssertAttributesProcessor'}, {'executionTime': 0.89, 'module': 'standard', 'checksum': 457707303, 'executionDate': '2020-03-03T21:54:09.394490', 'processor': 'boonai_core.proxy.ImageProxyProcessor'}, {'module': 'standard', 'checksum': 482873147, 'processor': 'boonai_core.proxy.VideoProxyProcessor'}, {'executionTime': 2.07, 'module': 'standard', 'checksum': 2479952423, 'executionDate': '2020-03-03T21:54:20.533214', 'processor': 'boonai_analysis.mxnet.ZviSimilarityProcessor'}]}, 'media': {'orientation': 'landscape', 'aspect': 1.5, 'width': 650, 'length': 1, 'type': 'image', 'height': 434}, 'analysis': {'zvi': {'similarity': {'simhash': 'PBPBFHAOBGAHCDGNEBDDCGPDCP'}, 'tinyProxy': ['#f3dfc3', '#f4efd8', '#c18f46', '#ebdfbd', '#ccd3c0', '#e7d4bb', '#beae8e', '#cabf9e', '#d2c09c']}}, 'clip': {'sourceAssetId': 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C', 'stop': 1.0, 'pile': 'pUn6wBxUN7x9JxOxLkvruOyNdYA', 'start': 1.0, 'length': 1.0, 'type': 'page'}}})  # noqa

        def mock_response(*args, **kwargs):
            return {
                'uri': 'http://minio:9000/project-storage/projects/00000000-0000-0000-0000-000000000000/assets/AjXYVpaVeLsOgpenKKSW8oDB5YuOTWDs/web-proxy/web-proxy.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20200602T013235Z&X-Amz-SignedHeaders=host&X-Amz-Expires=3599&X-Amz-Credential=qwerty123%2F20200602%2FUS_WEST_2%2Fs3%2Faws4_request&X-Amz-Signature=acbf9a9b0668b29315f262713742648c1943299e63cb1ea5e6145cf27ad4f95f',  # noqa
                'mediaType': 'image/jpeg'}

        def mock_get_timelines(*args, **kwargs):
            return []

        monkeypatch.setattr(AssetViewSet, 'retrieve', mock_detail_response)
        monkeypatch.setattr(AssetViewSet, '_get_list_of_timelines', mock_get_timelines)
        monkeypatch.setattr(BoonClient, 'get', mock_response)
        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        response = api_client.get(reverse('asset-urls',
                                          kwargs={'project_pk': project.id,
                                                  'pk': asset_id}))
        content = check_response(response)
        assert 'uri' in content['signedUrl']
        assert 'mediaType' in content['signedUrl']
        assert content['tracks'] == []

    def test_signed_url_with_fallback(self, login, project, api_client, monkeypatch):

        def mock_detail_response(*args, **kwargs):
            return Response(status=status.HTTP_200_OK, data={'id': 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C', 'metadata': {'system': {'jobId': '8d2603f7-00d4-132f-8681-0242ac120009', 'dataSourceId': '8d2603f6-00d4-132f-8681-0242ac120009', 'timeCreated': '2020-03-03T21:54:02.002039Z', 'state': 'Analyzed', 'projectId': '00000000-0000-0000-0000-000000000000', 'timeModified': '2020-03-03T21:54:23.978500Z', 'taskId': '8d2603f8-00d4-132f-8681-0242ac120009'}, 'files': [], 'source': {'path': 'gs://zorroa-dev-data/image/TIFF_1MB.tiff', 'extension': 'tiff', 'filename': 'TIFF_1MB.tiff', 'checksum': 1867533868, 'mimetype': 'image/tiff', 'filesize': 1131930}, 'metrics': {'pipeline': [{'executionTime': 0.52, 'module': 'standard', 'checksum': 1621235190, 'executionDate': '2020-03-03T21:54:04.185632', 'processor': 'boonai_core.core.processors.PreCacheSourceFileProcessor'}, {'executionTime': 0.5, 'module': 'standard', 'checksum': 1426657387, 'executionDate': '2020-03-03T21:54:06.820102', 'processor': 'boonai_core.image.importers.ImageImporter'}, {'module': 'standard', 'checksum': 2001473853, 'processor': 'boonai_core.office.importers.OfficeImporter'}, {'module': 'standard', 'checksum': 3310423168, 'processor': 'boonai_core.video.VideoImporter'}, {'executionTime': 0.0, 'module': 'standard', 'checksum': 1841569083, 'executionDate': '2020-03-03T21:54:08.449234', 'processor': 'boonai_core.core.processors.AssertAttributesProcessor'}, {'executionTime': 0.89, 'module': 'standard', 'checksum': 457707303, 'executionDate': '2020-03-03T21:54:09.394490', 'processor': 'boonai_core.proxy.ImageProxyProcessor'}, {'module': 'standard', 'checksum': 482873147, 'processor': 'boonai_core.proxy.VideoProxyProcessor'}, {'executionTime': 2.07, 'module': 'standard', 'checksum': 2479952423, 'executionDate': '2020-03-03T21:54:20.533214', 'processor': 'boonai_analysis.mxnet.ZviSimilarityProcessor'}]}, 'media': {'orientation': 'landscape', 'aspect': 1.5, 'width': 650, 'length': 1, 'type': 'image', 'height': 434}, 'analysis': {'zvi': {'similarity': {'simhash': 'PBPBFHAOBGAHCDGNEBDDCGPDCP'}, 'tinyProxy': ['#f3dfc3', '#f4efd8', '#c18f46', '#ebdfbd', '#ccd3c0', '#e7d4bb', '#beae8e', '#cabf9e', '#d2c09c']}}, 'clip': {'sourceAssetId': 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C', 'stop': 1.0, 'pile': 'pUn6wBxUN7x9JxOxLkvruOyNdYA', 'start': 1.0, 'length': 1.0, 'type': 'page'}}})  # noqa

        def mock_get_timelines(*args, **kwargs):
            return ['gcp-video-object-detection', 'gcp-video-label-detection']

        monkeypatch.setattr(AssetViewSet, 'retrieve', mock_detail_response)
        monkeypatch.setattr(AssetViewSet, '_get_list_of_timelines', mock_get_timelines)

        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        response = api_client.get(reverse('asset-urls',
                                          kwargs={'project_pk': project.id,
                                                  'pk': asset_id}))
        content = check_response(response)
        assert content['signedUrl']['uri'] == '/icons/fallback_3x.png'
        assert content['signedUrl']['mediaType'] == 'image/png'

    def test_tracks(self, login, project, api_client, monkeypatch):

        def mock_detail_response(*args, **kwargs):
            return Response(status=status.HTTP_200_OK, data={'id': 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C', 'metadata': {'system': {'jobId': '8d2603f7-00d4-132f-8681-0242ac120009', 'dataSourceId': '8d2603f6-00d4-132f-8681-0242ac120009', 'timeCreated': '2020-03-03T21:54:02.002039Z', 'state': 'Analyzed', 'projectId': '00000000-0000-0000-0000-000000000000', 'timeModified': '2020-03-03T21:54:23.978500Z', 'taskId': '8d2603f8-00d4-132f-8681-0242ac120009'}, 'files': [{'size': 8, 'name': 'gcp-video-speech-transcription.vtt', 'mimetype': 'text/vtt', 'id': 'assets/3XD9NiGjRjB8W9WCgTplZSMTV-zpnRg_/captions/gcp-video-speech-transcription.vtt', 'category': 'captions', 'attrs': {}}, {'id': 'this/is/the/id.jpg', 'size': 89643, 'name': 'image_650x434.jpg', 'mimetype': 'image/jpeg', 'category': 'proxy', 'attrs': {'width': 650, 'height': 434}}, {'size': 60713, 'name': 'image_512x341.jpg', 'mimetype': 'image/jpeg', 'category': 'proxy', 'attrs': {'width': 512, 'height': 341}}, {'size': 30882, 'name': 'image_320x213.jpg', 'mimetype': 'image/jpeg', 'category': 'proxy', 'attrs': {'width': 320, 'height': 213}}], 'source': {'path': 'gs://zorroa-dev-data/image/TIFF_1MB.tiff', 'extension': 'tiff', 'filename': 'TIFF_1MB.tiff', 'checksum': 1867533868, 'mimetype': 'image/tiff', 'filesize': 1131930}, 'metrics': {'pipeline': [{'executionTime': 0.52, 'module': 'standard', 'checksum': 1621235190, 'executionDate': '2020-03-03T21:54:04.185632', 'processor': 'boonai_core.core.processors.PreCacheSourceFileProcessor'}, {'executionTime': 0.5, 'module': 'standard', 'checksum': 1426657387, 'executionDate': '2020-03-03T21:54:06.820102', 'processor': 'boonai_core.image.importers.ImageImporter'}, {'module': 'standard', 'checksum': 2001473853, 'processor': 'boonai_core.office.importers.OfficeImporter'}, {'module': 'standard', 'checksum': 3310423168, 'processor': 'boonai_core.video.VideoImporter'}, {'executionTime': 0.0, 'module': 'standard', 'checksum': 1841569083, 'executionDate': '2020-03-03T21:54:08.449234', 'processor': 'boonai_core.core.processors.AssertAttributesProcessor'}, {'executionTime': 0.89, 'module': 'standard', 'checksum': 457707303, 'executionDate': '2020-03-03T21:54:09.394490', 'processor': 'boonai_core.proxy.ImageProxyProcessor'}, {'module': 'standard', 'checksum': 482873147, 'processor': 'boonai_core.proxy.VideoProxyProcessor'}, {'executionTime': 2.07, 'module': 'standard', 'checksum': 2479952423, 'executionDate': '2020-03-03T21:54:20.533214', 'processor': 'boonai_analysis.mxnet.ZviSimilarityProcessor'}]}, 'media': {'orientation': 'landscape', 'aspect': 1.5, 'width': 650, 'length': 1, 'type': 'image', 'height': 434}, 'analysis': {'zvi': {'similarity': {'simhash': 'PBPBFHAOBGAHCDGNEBDDCGPDCP'}, 'tinyProxy': ['#f3dfc3', '#f4efd8', '#c18f46', '#ebdfbd', '#ccd3c0', '#e7d4bb', '#beae8e', '#cabf9e', '#d2c09c']}}, 'clip': {'sourceAssetId': 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C', 'stop': 1.0, 'pile': 'pUn6wBxUN7x9JxOxLkvruOyNdYA', 'start': 1.0, 'length': 1.0, 'type': 'page'}}})  # noqa

        def mock_response(*args, **kwargs):
            return {
                'uri': 'http://minio:9000/project-storage/projects/00000000-0000-0000-0000-000000000000/assets/AjXYVpaVeLsOgpenKKSW8oDB5YuOTWDs/web-proxy/web-proxy.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20200602T013235Z&X-Amz-SignedHeaders=host&X-Amz-Expires=3599&X-Amz-Credential=qwerty123%2F20200602%2FUS_WEST_2%2Fs3%2Faws4_request&X-Amz-Signature=acbf9a9b0668b29315f262713742648c1943299e63cb1ea5e6145cf27ad4f95f',  # noqa
                'mediaType': 'image/jpeg'}

        def mock_get_timelines(*args, **kwargs):
            return ['gcp-video-logo-detection', 'gcp-video-object-detection']

        monkeypatch.setattr(AssetViewSet, 'retrieve', mock_detail_response)
        monkeypatch.setattr(AssetViewSet, '_get_list_of_timelines', mock_get_timelines)
        monkeypatch.setattr(BoonClient, 'get', mock_response)
        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        response = api_client.get(reverse('asset-urls',
                                          kwargs={'project_pk': project.id,
                                                  'pk': asset_id}))
        content = check_response(response)
        assert 'uri' in content['signedUrl']
        assert 'mediaType' in content['signedUrl']
        assert len(content['tracks']) == 3
        assert {'label': 'gcp-video-logo-detection',
                'kind': 'metadata',
                'src': '/api/v1/projects/6abc33f0-4acf-4196-95ff-4cbb7f640a06/assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/webvtt/gcp-video-logo-detection.vtt/'} in content['tracks']
        assert {'label': 'gcp-video-object-detection',
                'kind': 'metadata',
                'src': '/api/v1/projects/6abc33f0-4acf-4196-95ff-4cbb7f640a06/assets/vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C/webvtt/gcp-video-object-detection.vtt/'} in content['tracks']
        found = False
        for track in content['tracks']:
            if track['kind'] == 'captions':
                found = True
                track['label'] == 'Gcp Video Speech'
                # don't bother checking the url since we're using a bad mock response to create it
        assert found is True


class TestWebVttViewSet:

    def test_get(self, project, login, api_client, monkeypatch):

        def mock_streamer(*args, **kwargs):
            for x in range(0, 1024):
                yield x

        monkeypatch.setattr(requests, 'get', mock_streamer)
        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        webvtt = 'all.vtt'
        response = api_client.get(reverse('webvtt-detail', kwargs={'project_pk': project.id,
                                                                   'asset_pk': asset_id,
                                                                   'pk': webvtt}))
        assert isinstance(response, StreamingHttpResponse)
        assert response.get('cache-control') == 'max-age=86400, private'
        assert 'expires' in response.headers


class TestFileNameViewSet:

    def test_get_proxy(self, login, project, api_client, monkeypatch):

        def mock_streamer(*args, **kwargs):
            for x in range(0, 1024):
                yield x

        monkeypatch.setattr(requests, 'get', mock_streamer)
        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        filename = 'TIFF_1MB.tiff'
        response = api_client.get(reverse('file_name-detail', kwargs={'project_pk': project.id,
                                                                      'asset_pk': asset_id,
                                                                      'category_pk': 'proxy',
                                                                      'pk': filename}))
        assert isinstance(response, StreamingHttpResponse)
        assert response.get('cache-control') == 'max-age=86400, private'
        assert 'expires' in response.headers

    def test_get_signed_url(self, project, api_client, monkeypatch, login):

        def mock_response(*args, **kwargs):
            return {'uri': 'http://minio:9000/project-storage/projects/00000000-0000-0000-0000-000000000000/assets/AjXYVpaVeLsOgpenKKSW8oDB5YuOTWDs/web-proxy/web-proxy.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20200602T013235Z&X-Amz-SignedHeaders=host&X-Amz-Expires=3599&X-Amz-Credential=qwerty123%2F20200602%2FUS_WEST_2%2Fs3%2Faws4_request&X-Amz-Signature=acbf9a9b0668b29315f262713742648c1943299e63cb1ea5e6145cf27ad4f95f', 'mediaType': 'image/jpeg'}  # noqa

        monkeypatch.setattr(BoonClient, 'get', mock_response)
        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        filename = 'TIFF_1MB.tiff'
        response = api_client.get(reverse('file_name-signed-url',
                                          kwargs={'project_pk': project.id,
                                                  'asset_pk': asset_id,
                                                  'category_pk': 'proxy',
                                                  'pk': filename}))
        content = check_response(response)
        assert 'uri' in content
        assert 'mediaType' in content


class TestBoxImagesAction:
    def test_box_images(self, login, project, monkeypatch, api_client, zmlp_project_user,
                        detail_api_return):
        def mock_get_attr_with_box_images(*args, **kwargs):
            return {'count': 1, 'type': 'labels', 'predictions': [{'score': 0.882, 'bbox': [0.068, 0.079, 0.904, 0.739], 'label': 'laptop', 'b64_image': 'data:image/png;base64, iVBORw0KGgoAAAANSUhEUgAAAAoAAAAICAIAAABPmPnhAAAAI0lEQVQIHXXBAQEAAAABIP6PzgJV5CvyFfmKfEW+Il+Rr8g33SQX8fv7NasAAAAASUVORK5CYII='}, {'score': 0.882, 'bbox': [0.068, 0.079, 0.904, 0.739], 'label': 'laptop2', 'b64_image': 'data:image/png;base64, iVBORw0KGgoAAAANSUhEUgAAAAoAAAAICAIAAABPmPnhAAAAI0lEQVQIHXXBAQEAAAABIP6PzgJV5CvyFfmKfEW+Il+Rr8g33SQX8fv7NasAAAAASUVORK5CYII='}]}  # noqa

        monkeypatch.setattr(AssetBoxImager, 'get_attr_with_box_images',
                            mock_get_attr_with_box_images)
        monkeypatch.setattr(BoonClient, 'get', lambda *args: detail_api_return)
        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        base_url = reverse('asset-box-images', kwargs={'project_pk': project.id, 'pk': asset_id})
        response = api_client.get(f'{base_url}?attr=analysis.zvi-object-detection')
        assert response.status_code == 200
        assert 'zvi-object-detection' in response.json()


class TestAnalyzeAction:
    def test_analyze_action(self, login, project, monkeypatch, api_client):
        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

        def mock_response(*args, **kwargs):
            return {'document': {'analysis': {'boonai-image-similarity': {'simhash': 'NOHLEPPPFCCLPECDPAMKPCLFPPPFLPFPCBEEPHPPPPHKPPPPMPPPLHNPPFPNNPGEEPEPAPMPBHPKPPHGBPIBPOJMIKDCPEPPALPDPJJFHPJPDHMMCAIKDEMIFEOAAIIDLPLPPPLELPPPPGGPIPPKKPPPPGFPLPPPPPPJPEBEDPPFPIOPPJEPPMPPGHKPPPPPGPFPHPPPKFPJPPPPPHCPPNPPAAJJEPPPPPPBPPBPMPIPPPBJPPPBHAPPPPGDPPOCFPOPCPOPNPPCOPPPPKJHPPIPHJPPPPPPPFPBFONLEPPOPLIBPKPKPGDILPNDPHPPBPPPPPFPNDGIHPPJCPFPPPPLAPPPJPJPPPPJEEGPBPBPKOPFKPMPCLPPNPHPNAPNDDEOPCLGBPPLFPPPPPEALDPPPEPBHPGPCGPBCCPPBFIPPCOFFPPJPFPPBPPDAIPPNNAAPPPHKDPLKBPCPGDPBHLFPFPPPPBPGPNBBPPPFLPLCPGPPPPHPFPHIPBPNAIAANPKPHPPKFPPGPKPOPPPNKPICDPCAEPPPPPCPPNPPPKPPANPPJPPBPPPKBPPPKAPPEPPPLKCIPLBPCLCLPLPCLPPKPDLPPPPFGOCDPGEBBPKCPOJBPENFPPPAPPGDHOPHLPBNPKCMBPAPKFPPMFPPPPAPNPPPPAEDPPPPBPJPPPHPPFNHHBFCDFPMPFIAMOPBPKODLBBGPLFPPPGCPPPPEMPFPPPPLFPPJNPIMCEPPPNPDPPEAHGPPFPPAPEPPHKPPIPOODKHPPILPHPPJPCPPAKPPIPLEPAPPPPPJGPDCPKCPBOPEPBAPPPAPPGBJHJIPEEEPPCNOBLPAEPOPHNNPFPOHOPHPPEPPKPGGHHHGMGICOPPDKGEPLPPPPAPDHPPKDPBPPBPEPPMPBCBCHPPPPGPPPPPPPCPBPBJPOPPPPHHPAALPMPPJPGCJGPDPJEPGCGPPFPENMHPMFBFCPPLPPACPHHJLPFAPHLPDBPIPPHPPDMAGHBLKKPPPHNCAAPBIJPPBPPEOPPPPPPKPLCPMPIPHODHAPDPFDPPPLKEPPKBAOPPPPHNEBPEPPCPPAFKGPPPGEPPPNPJFPIPPPOPFPPCIPPJHPPPKAPGAOPPFPDAIBMJPIKMPPCCIPAEPPLMPPPLOMAPKHPPHAPPDPPJFCPGIPBPIPPFPPDPPFPPDPPGPPPPPNPDFAMPPPKPPAFPPCAPDPJPPPNPCPFPIDJFCPPJKPPOPPPPLPPKBFIDPLPPPDBLPPEDJPCDGPPDDPJNPPEECPPGPDPBDKPPLCPBPPPPPPBGCIKPPAEPBFIBAPPAEPLPPNPCLEFDPHPCIPPMAPPPGPPMOJMJAKPPPCPDPPPJMPBLPPPIDDPPPPOEECPLPHCPPPPAHBPPJPKPPPPLOGPFCPCPGOPPPJPPEPPPBDIPPNEPPPFIJPALFPPGKPMPDPBPPPLJABAPPHPPGCHAEPKPBPPPNBEPFNDPBPIPGCKPHPPOMFPEJPPPOPFJEPJHPFOBMPPECPFIPPGLDAPCOAPPPPGMIPBPPPPPPPPFPHBBPPBGPHBIKOPDMBPPPPCPPKOPPPPPIHPPHDPPPPPPOCDPOPJDPHPPMBDPCPAAMPHPBEPEBBPHPDGBPPPPPEPMPIPPKPEPPPLFCCPPPJCCPOJPAPMBBCLPEEPACKIPPCIBDPPCGIJPPPPPPPLPKKPPHPPPPPPEEDKPPNPPIPJPLCPEPCJPPNAPBPIKPPDPPMPHPPELFPNBPFPPAPAPPPPBBPCPOPOPGPPPPPFPEDPPGPHNBPPPPAPPGKPPDPNMJMPJDCPPPDPAHNPPPHNPPAPPPPCCPBPPNIPPPKPPPPPICDPPLMBPBLDPPDIPMLPHFNBEPPMAPPPGPPPPKPPPPPPPPNPDPCPPKEPPFKDAPBPCHGBFPPPDPIPPFPPCPMPEFPPINFDKPPPJDPPPEPEPKAEOEPMPPKGPPPDPPLPEEPBPBPEPMIPHPPKGGHPDFBEJIMPDNBPBPIEAPCFLDDPBPHPBPAMPIPPBPHPNPDPFPPL', 'type': 'similarity'}, 'clarifai-age-detection': {'count': 9, 'predictions': [{'label': '20-29', 'occurrences': 1, 'score': 0.422}, {'label': '30-39', 'occurrences': 1, 'score': 0.332}, {'label': '10-19', 'occurrences': 1, 'score': 0.092}, {'label': '40-49', 'occurrences': 1, 'score': 0.081}, {'label': '3-9', 'occurrences': 1, 'score': 0.043}, {'label': '50-59', 'occurrences': 1, 'score': 0.02}, {'label': '60-69', 'occurrences': 1, 'score': 0.005}, {'label': '0-2', 'occurrences': 1, 'score': 0.004}, {'label': 'more than 70', 'occurrences': 1, 'score': 0.002}], 'type': 'labels'}, 'clarifai-apparel-detection': {'count': 20, 'predictions': [{'label': 'Tube Top', 'occurrences': 1, 'score': 0.45}, {'label': 'T-Shirt', 'occurrences': 1, 'score': 0.409}, {'label': 'Knee Length Skirt', 'occurrences': 1, 'score': 0.307}, {'label': 'Panties', 'occurrences': 1, 'score': 0.277}, {'label': "Men's Watch", 'occurrences': 1, 'score': 0.269}, {'label': 'Necklace', 'occurrences': 1, 'score': 0.252}, {'label': 'Cocktail Dress', 'occurrences': 1, 'score': 0.245}, {'label': 'Umbrella', 'occurrences': 1, 'score': 0.222}, {'label': 'Prom Dress', 'occurrences': 1, 'score': 0.211}, {'label': 'Formal Dress', 'occurrences': 1, 'score': 0.177}, {'label': 'Bracelet', 'occurrences': 1, 'score': 0.157}, {'label': 'Sweatshirt', 'occurrences': 1, 'score': 0.157}, {'label': 'Strapless Dress', 'occurrences': 1, 'score': 0.147}, {'label': 'Ring', 'occurrences': 1, 'score': 0.142}, {'label': "Women's Scarf", 'occurrences': 1, 'score': 0.129}, {'label': 'Maxi Skirt', 'occurrences': 1, 'score': 0.126}, {'label': 'Midi Skirt', 'occurrences': 1, 'score': 0.122}, {'label': "Women's Hat", 'occurrences': 1, 'score': 0.12}, {'label': 'Sweater', 'occurrences': 1, 'score': 0.116}, {'label': 'Wedding Dress', 'occurrences': 1, 'score': 0.115}], 'type': 'labels'}, 'clarifai-celebrity-detection': {'count': 0, 'predictions': [], 'type': 'labels'}, 'clarifai-ethnicity-detection': {'count': 7, 'predictions': [{'label': 'Black', 'occurrences': 1, 'score': 0.547}, {'label': 'White', 'occurrences': 1, 'score': 0.125}, {'label': 'Latino_Hispanic', 'occurrences': 1, 'score': 0.099}, {'label': 'Middle Eastern', 'occurrences': 1, 'score': 0.082}, {'label': 'East Asian', 'occurrences': 1, 'score': 0.07}, {'label': 'Southeast Asian', 'occurrences': 1, 'score': 0.054}, {'label': 'Indian', 'occurrences': 1, 'score': 0.024}], 'type': 'labels'}, 'clarifai-face-detection': {'count': 0, 'predictions': [], 'type': 'labels'}, 'clarifai-food-detection': {'count': 20, 'predictions': [{'label': 'coffee', 'occurrences': 1, 'score': 0.906}, {'label': 'tea', 'occurrences': 1, 'score': 0.9}, {'label': 'water', 'occurrences': 1, 'score': 0.877}, {'label': 'wine', 'occurrences': 1, 'score': 0.793}, {'label': 'apple', 'occurrences': 1, 'score': 0.765}, {'label': 'chocolate', 'occurrences': 1, 'score': 0.747}, {'label': 'ice', 'occurrences': 1, 'score': 0.746}, {'label': 'rum', 'occurrences': 1, 'score': 0.662}, {'label': 'pie', 'occurrences': 1, 'score': 0.634}, {'label': 'orange', 'occurrences': 1, 'score': 0.63}, {'label': 'lime', 'occurrences': 1, 'score': 0.611}, {'label': 'cake', 'occurrences': 1, 'score': 0.61}, {'label': 'coconut', 'occurrences': 1, 'score': 0.563}, {'label': 'green tea', 'occurrences': 1, 'score': 0.481}, {'label': 'cheesecake', 'occurrences': 1, 'score': 0.451}, {'label': 'fudge', 'occurrences': 1, 'score': 0.449}, {'label': 'beer', 'occurrences': 1, 'score': 0.424}, {'label': 'peach', 'occurrences': 1, 'score': 0.424}, {'label': 'turkey', 'occurrences': 1, 'score': 0.417}, {'label': 'cherry', 'occurrences': 1, 'score': 0.364}], 'type': 'labels'}, 'clarifai-gender-detection': {'count': 2, 'predictions': [{'label': 'Masculine', 'occurrences': 1, 'score': 0.581}, {'label': 'Feminine', 'occurrences': 1, 'score': 0.419}], 'type': 'labels'}, 'clarifai-label-detection': {'count': 0, 'predictions': [], 'type': 'labels'}, 'clarifai-logo-detection': {'count': 1, 'predictions': [{'label': 'Coachella', 'occurrences': 1, 'score': 0.283}], 'type': 'labels'}, 'clarifai-nsfw-detection': {'count': 2, 'predictions': [{'label': 'sfw', 'occurrences': 1, 'score': 0.879}, {'label': 'nsfw', 'occurrences': 1, 'score': 0.121}], 'type': 'labels'}, 'clarifai-room-types-detection': {'count': 20, 'predictions': [{'label': 'living-room', 'occurrences': 1, 'score': 0.053}, {'label': 'playground', 'occurrences': 1, 'score': 0.013}, {'label': 'indoor-swimming-pool', 'occurrences': 1, 'score': 0.004}, {'label': 'bedroom', 'occurrences': 1, 'score': 0.002}, {'label': 'bathroom', 'occurrences': 1, 'score': 0.001}, {'label': 'gym', 'occurrences': 1, 'score': 0.0}, {'label': 'library', 'occurrences': 1, 'score': 0.0}, {'label': 'beach', 'occurrences': 1, 'score': 0.0}, {'label': 'porch', 'occurrences': 1, 'score': 0.0}, {'label': 'dock', 'occurrences': 1, 'score': 0.0}, {'label': 'playroom', 'occurrences': 1, 'score': 0.0}, {'label': 'door', 'occurrences': 1, 'score': 0.0}, {'label': 'hot-tub', 'occurrences': 1, 'score': 0.0}, {'label': 'backyard', 'occurrences': 1, 'score': 0.0}, {'label': 'billiard-table', 'occurrences': 1, 'score': 0.0}, {'label': 'balcony', 'occurrences': 1, 'score': 0.0}, {'label': 'stairs', 'occurrences': 1, 'score': 0.0}, {'label': 'childrens-bedroom', 'occurrences': 1, 'score': 0.0}, {'label': 'nursery', 'occurrences': 1, 'score': 0.0}, {'label': 'window', 'occurrences': 1, 'score': 0.0}], 'type': 'labels'}, 'clarifai-texture-detection': {'count': 20, 'predictions': [{'label': 'clouds', 'occurrences': 1, 'score': 0.027}, {'label': 'glacial ice', 'occurrences': 1, 'score': 0.013}, {'label': 'lava', 'occurrences': 1, 'score': 0.012}, {'label': 'ice crystal', 'occurrences': 1, 'score': 0.01}, {'label': 'bark', 'occurrences': 1, 'score': 0.009}, {'label': 'petrified wood', 'occurrences': 1, 'score': 0.009}, {'label': 'water', 'occurrences': 1, 'score': 0.008}, {'label': 'frost', 'occurrences': 1, 'score': 0.006}, {'label': 'autumn foliage', 'occurrences': 1, 'score': 0.006}, {'label': 'grain fields', 'occurrences': 1, 'score': 0.006}, {'label': 'mudcracks', 'occurrences': 1, 'score': 0.005}, {'label': 'sand', 'occurrences': 1, 'score': 0.005}, {'label': 'conifer', 'occurrences': 1, 'score': 0.004}, {'label': 'dry foliage', 'occurrences': 1, 'score': 0.004}, {'label': 'fossil', 'occurrences': 1, 'score': 0.004}, {'label': 'smoke', 'occurrences': 1, 'score': 0.004}, {'label': 'rust', 'occurrences': 1, 'score': 0.004}, {'label': 'moss', 'occurrences': 1, 'score': 0.003}, {'label': 'spiral', 'occurrences': 1, 'score': 0.003}, {'label': 'cobblestone', 'occurrences': 1, 'score': 0.003}], 'type': 'labels'}, 'clarifai-travel-detection': {'count': 20, 'predictions': [{'label': 'Autumn', 'occurrences': 1, 'score': 0.839}, {'label': 'View', 'occurrences': 1, 'score': 0.605}, {'label': 'Surroundings', 'occurrences': 1, 'score': 0.583}, {'label': 'Winter', 'occurrences': 1, 'score': 0.555}, {'label': 'Snow & Ski Sports', 'occurrences': 1, 'score': 0.504}, {'label': 'Spring', 'occurrences': 1, 'score': 0.369}, {'label': 'Summer', 'occurrences': 1, 'score': 0.284}, {'label': 'Terrace', 'occurrences': 1, 'score': 0.221}, {'label': 'People', 'occurrences': 1, 'score': 0.134}, {'label': 'Entrance', 'occurrences': 1, 'score': 0.086}, {'label': 'Daytime', 'occurrences': 1, 'score': 0.077}, {'label': 'Nighttime', 'occurrences': 1, 'score': 0.054}, {'label': 'Building', 'occurrences': 1, 'score': 0.051}, {'label': 'Boat', 'occurrences': 1, 'score': 0.043}, {'label': 'Bicycling', 'occurrences': 1, 'score': 0.041}, {'label': 'Couch', 'occurrences': 1, 'score': 0.033}, {'label': 'Reception', 'occurrences': 1, 'score': 0.027}, {'label': 'Yoga', 'occurrences': 1, 'score': 0.025}, {'label': 'Balcony', 'occurrences': 1, 'score': 0.023}, {'label': 'Television', 'occurrences': 1, 'score': 0.02}], 'type': 'labels'}, 'clarifai-unsafe-detection': {'count': 5, 'predictions': [{'label': 'safe', 'occurrences': 1, 'score': 1.0}, {'label': 'suggestive', 'occurrences': 1, 'score': 0.0}, {'label': 'explicit', 'occurrences': 1, 'score': 0.0}, {'label': 'gore', 'occurrences': 1, 'score': 0.0}, {'label': 'drug', 'occurrences': 1, 'score': 0.0}], 'type': 'labels'}, 'clarifai-weapon-detection': {'count': 2, 'predictions': [{'label': 'long-gun', 'occurrences': 226, 'score': 0.705}, {'label': 'heavy-artillery', 'occurrences': 6, 'score': 0.13}], 'type': 'labels'}, 'clarifai-wedding-detection': {'count': 20, 'predictions': [{'label': 'travel', 'occurrences': 1, 'score': 0.86}, {'label': 'outdoors', 'occurrences': 1, 'score': 0.856}, {'label': 'scenery', 'occurrences': 1, 'score': 0.708}, {'label': 'blue', 'occurrences': 1, 'score': 0.664}, {'label': 'winter', 'occurrences': 1, 'score': 0.662}, {'label': 'tents', 'occurrences': 1, 'score': 0.505}, {'label': 'italy', 'occurrences': 1, 'score': 0.478}, {'label': 'summer', 'occurrences': 1, 'score': 0.477}, {'label': 'beach', 'occurrences': 1, 'score': 0.461}, {'label': 'beauty', 'occurrences': 1, 'score': 0.415}, {'label': 'love', 'occurrences': 1, 'score': 0.396}, {'label': 'wildflowers', 'occurrences': 1, 'score': 0.361}, {'label': 'ocean', 'occurrences': 1, 'score': 0.336}, {'label': 'fall', 'occurrences': 1, 'score': 0.327}, {'label': 'yellow', 'occurrences': 1, 'score': 0.316}, {'label': 'orange', 'occurrences': 1, 'score': 0.299}, {'label': 'white', 'occurrences': 1, 'score': 0.274}, {'label': 'green', 'occurrences': 1, 'score': 0.26}, {'label': 'walls', 'occurrences': 1, 'score': 0.235}, {'label': 'vintage', 'occurrences': 1, 'score': 0.235}], 'type': 'labels'}, 'gcp-video-label-detection': {'count': 11, 'predictions': [{'label': 'vehicle', 'occurrences': 4, 'score': 0.941}, {'label': 'geographical feature', 'occurrences': 8, 'score': 0.909}, {'label': 'mountain', 'occurrences': 2, 'score': 0.909}, {'label': 'mountain range', 'occurrences': 2, 'score': 0.839}, {'label': 'off roading', 'occurrences': 2, 'score': 0.764}, {'label': 'wilderness', 'occurrences': 2, 'score': 0.653}, {'label': 'off road vehicle', 'occurrences': 2, 'score': 0.637}, {'label': 'car', 'occurrences': 4, 'score': 0.637}, {'label': 'hill', 'occurrences': 2, 'score': 0.384}, {'label': 'winter', 'occurrences': 1, 'score': 0.368}, {'label': 'season', 'occurrences': 1, 'score': 0.368}], 'type': 'labels'}, 'gcp-video-logo-detection': {'count': 2, 'predictions': [{'label': 'General Motors', 'occurrences': 1, 'score': 0.915}, {'label': 'Jaguar Cars', 'occurrences': 1, 'score': 0.908}], 'type': 'labels'}, 'zvi-image-similarity': {'simhash': 'NOHLEPPPFCCLPECDPAMKPCLFPPPFLPFPCBEEPHPPPPHKPPPPMPPPLHNPPFPNNPGEEPEPAPMPBHPKPPHGBPIBPOJMIKDCPEPPALPDPJJFHPJPDHMMCAIKDEMIFEOAAIIDLPLPPPLELPPPPGGPIPPKKPPPPGFPLPPPPPPJPEBEDPPFPIOPPJEPPMPPGHKPPPPPGPFPHPPPKFPJPPPPPHCPPNPPAAJJEPPPPPPBPPBPMPIPPPBJPPPBHAPPPPGDPPOCFPOPCPOPNPPCOPPPPKJHPPIPHJPPPPPPPFPBFONLEPPOPLIBPKPKPGDILPNDPHPPBPPPPPFPNDGIHPPJCPFPPPPLAPPPJPJPPPPJEEGPBPBPKOPFKPMPCLPPNPHPNAPNDDEOPCLGBPPLFPPPPPEALDPPPEPBHPGPCGPBCCPPBFIPPCOFFPPJPFPPBPPDAIPPNNAAPPPHKDPLKBPCPGDPBHLFPFPPPPBPGPNBBPPPFLPLCPGPPPPHPFPHIPBPNAIAANPKPHPPKFPPGPKPOPPPNKPICDPCAEPPPPPCPPNPPPKPPANPPJPPBPPPKBPPPKAPPEPPPLKCIPLBPCLCLPLPCLPPKPDLPPPPFGOCDPGEBBPKCPOJBPENFPPPAPPGDHOPHLPBNPKCMBPAPKFPPMFPPPPAPNPPPPAEDPPPPBPJPPPHPPFNHHBFCDFPMPFIAMOPBPKODLBBGPLFPPPGCPPPPEMPFPPPPLFPPJNPIMCEPPPNPDPPEAHGPPFPPAPEPPHKPPIPOODKHPPILPHPPJPCPPAKPPIPLEPAPPPPPJGPDCPKCPBOPEPBAPPPAPPGBJHJIPEEEPPCNOBLPAEPOPHNNPFPOHOPHPPEPPKPGGHHHGMGICOPPDKGEPLPPPPAPDHPPKDPBPPBPEPPMPBCBCHPPPPGPPPPPPPCPBPBJPOPPPPHHPAALPMPPJPGCJGPDPJEPGCGPPFPENMHPMFBFCPPLPPACPHHJLPFAPHLPDBPIPPHPPDMAGHBLKKPPPHNCAAPBIJPPBPPEOPPPPPPKPLCPMPIPHODHAPDPFDPPPLKEPPKBAOPPPPHNEBPEPPCPPAFKGPPPGEPPPNPJFPIPPPOPFPPCIPPJHPPPKAPGAOPPFPDAIBMJPIKMPPCCIPAEPPLMPPPLOMAPKHPPHAPPDPPJFCPGIPBPIPPFPPDPPFPPDPPGPPPPPNPDFAMPPPKPPAFPPCAPDPJPPPNPCPFPIDJFCPPJKPPOPPPPLPPKBFIDPLPPPDBLPPEDJPCDGPPDDPJNPPEECPPGPDPBDKPPLCPBPPPPPPBGCIKPPAEPBFIBAPPAEPLPPNPCLEFDPHPCIPPMAPPPGPPMOJMJAKPPPCPDPPPJMPBLPPPIDDPPPPOEECPLPHCPPPPAHBPPJPKPPPPLOGPFCPCPGOPPPJPPEPPPBDIPPNEPPPFIJPALFPPGKPMPDPBPPPLJABAPPHPPGCHAEPKPBPPPNBEPFNDPBPIPGCKPHPPOMFPEJPPPOPFJEPJHPFOBMPPECPFIPPGLDAPCOAPPPPGMIPBPPPPPPPPFPHBBPPBGPHBIKOPDMBPPPPCPPKOPPPPPIHPPHDPPPPPPOCDPOPJDPHPPMBDPCPAAMPHPBEPEBBPHPDGBPPPPPEPMPIPPKPEPPPLFCCPPPJCCPOJPAPMBBCLPEEPACKIPPCIBDPPCGIJPPPPPPPLPKKPPHPPPPPPEEDKPPNPPIPJPLCPEPCJPPNAPBPIKPPDPPMPHPPELFPNBPFPPAPAPPPPBBPCPOPOPGPPPPPFPEDPPGPHNBPPPPAPPGKPPDPNMJMPJDCPPPDPAHNPPPHNPPAPPPPCCPBPPNIPPPKPPPPPICDPPLMBPBLDPPDIPMLPHFNBEPPMAPPPGPPPPKPPPPPPPPNPDPCPPKEPPFKDAPBPCHGBFPPPDPIPPFPPCPMPEFPPINFDKPPPJDPPPEPEPKAEOEPMPPKGPPPDPPLPEEPBPBPEPMIPHPPKGGHPDFBEJIMPDNBPBPIEAPCFLDDPBPHPBPAMPIPPBPHPNPDPFPPL', 'type': 'similarity'}}, 'deepSearch': 'video', 'files': [{'attrs': {'height': 720, 'time_offset': 3.29, 'width': 1280}, 'category': 'proxy', 'id': 'assets/pBbuB7fgST2faIKXAF1ZU4gZEDWzT03c/proxy/image_1280x720.jpg', 'mimetype': 'image/jpeg', 'name': 'image_1280x720.jpg', 'size': 230211}, {'attrs': {'height': 288, 'time_offset': 3.29, 'width': 512}, 'category': 'proxy', 'id': 'assets/pBbuB7fgST2faIKXAF1ZU4gZEDWzT03c/proxy/image_512x288.jpg', 'mimetype': 'image/jpeg', 'name': 'image_512x288.jpg', 'size': 53131}, {'attrs': {'height': 576, 'width': 1024}, 'category': 'web-proxy', 'id': 'assets/pBbuB7fgST2faIKXAF1ZU4gZEDWzT03c/web-proxy/web-proxy.jpg', 'mimetype': 'image/jpeg', 'name': 'web-proxy.jpg', 'size': 103807}, {'attrs': {'frameRate': 29.97, 'frames': 318, 'height': 720, 'transcode': 'optimize', 'width': 1280}, 'category': 'proxy', 'id': 'assets/pBbuB7fgST2faIKXAF1ZU4gZEDWzT03c/proxy/video_1280x720.mp4', 'mimetype': 'video/mp4', 'name': 'video_1280x720.mp4', 'size': 1566011}, {'attrs': {}, 'category': 'gcp', 'id': 'assets/pBbuB7fgST2faIKXAF1ZU4gZEDWzT03c/gcp/video-intelligence.dat', 'mimetype': 'application/octet-stream', 'name': 'video-intelligence.dat', 'size': 1530}], 'media': {'aspect': 1.78, 'height': 720, 'length': 6.571, 'orientation': 'landscape', 'timeCreated': '2021-01-14T19:50:18.000000Z', 'type': 'video', 'videoCodec': 'h264', 'width': 1280}, 'metrics': {'pipeline': [{'checksum': 2178814325, 'executionDate': '2021-01-14T21:33:15.252475', 'executionTime': 0.16, 'module': 'standard', 'processor': 'zmlp_core.core.PreCacheSourceFileProcessor'}, {'checksum': 117837444, 'executionDate': '2021-01-14T21:33:15.888515', 'executionTime': 0.56, 'module': 'standard', 'processor': 'zmlp_core.core.FileImportProcessor'}, {'checksum': 457707303, 'executionDate': '2021-01-14T21:33:17.721251', 'executionTime': 1.8, 'module': 'standard', 'processor': 'zmlp_core.proxy.ImageProxyProcessor'}, {'checksum': 482873147, 'executionDate': '2021-01-14T21:33:18.490945', 'executionTime': 0.74, 'module': 'standard', 'processor': 'zmlp_core.proxy.VideoProxyProcessor'}, {'checksum': 1879445844, 'executionDate': '2021-01-14T21:33:25.123911', 'executionTime': 0.55, 'module': 'standard', 'processor': 'zmlp_analysis.zvi.ZviSimilarityProcessor'}, {'checksum': 3931710671, 'executionDate': '2021-01-23T01:56:34.933085', 'executionTime': 43.17, 'module': 'gcp-video-label-detection', 'processor': 'zmlp_analysis.google.AsyncVideoIntelligenceProcessor'}, {'checksum': 3336180480, 'executionTime': 0, 'module': 'azure-label-detection', 'processor': 'zmlp_analysis.azure.AzureVisionLabelDetection'}, {'checksum': 2987135615, 'executionDate': '2021-01-14T21:33:34.570466', 'executionTime': 2.49, 'module': 'azure-label-detection', 'processor': 'zmlp_analysis.azure.AzureVideoLabelDetection'}, {'checksum': 2723746346, 'executionDate': '2021-03-11T20:30:58.877254', 'executionTime': 0.17, 'module': 'standard', 'processor': 'boonai_core.core.PreCacheSourceFileProcessor'}, {'checksum': 567873337, 'executionDate': '2021-03-11T20:30:59.820505', 'executionTime': 0.77, 'module': 'standard', 'processor': 'boonai_core.core.FileImportProcessor'}, {'checksum': 919605212, 'executionDate': '2021-03-11T20:31:02.716368', 'executionTime': 2.84, 'module': 'standard', 'processor': 'boonai_core.proxy.ImageProxyProcessor'}, {'checksum': 944771056, 'executionDate': '2021-03-11T20:31:04.371709', 'executionTime': 1.62, 'module': 'standard', 'processor': 'boonai_core.proxy.VideoProxyProcessor'}, {'checksum': 3292599080, 'executionDate': '2021-03-11T20:31:49.143818', 'executionTime': 0.91, 'module': 'standard', 'processor': 'boonai_analysis.boonai.ZviSimilarityProcessor'}, {'checksum': 2686719765, 'executionDate': '2021-03-11T20:31:59.602963', 'executionTime': 0.41, 'module': 'aws-end-credits-detection', 'processor': 'boonai_analysis.aws.video.EndCreditsVideoDetectProcessor'}, {'checksum': 114366711, 'executionTime': 0, 'module': 'clarifai-room-types-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiRoomTypesDetectionProcessor'}, {'checksum': 2250840814, 'executionDate': '2021-06-04T22:22:00.068324', 'executionTime': 3.0, 'module': 'clarifai-room-types-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoRoomTypesDetectionProcessor'}, {'checksum': 3161790383, 'executionTime': 0, 'module': 'clarifai-weapon-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiWeaponDetectionProcessor'}, {'checksum': 905386406, 'executionDate': '2021-06-04T22:22:05.926115', 'executionTime': 5.76, 'module': 'clarifai-weapon-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoWeaponDetectionProcessor'}, {'checksum': 3127973786, 'executionTime': 0, 'module': 'clarifai-demographics-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiGenderDetectionProcessor'}, {'checksum': 107288822, 'executionTime': 0, 'module': 'clarifai-demographics-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiEthnicityDetectionProcessor'}, {'checksum': 1949963858, 'executionTime': 0, 'module': 'clarifai-demographics-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiAgeDetectionProcessor'}, {'checksum': 871569809, 'executionDate': '2021-06-04T22:22:07.100536', 'executionTime': 0.92, 'module': 'clarifai-demographics-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoGenderDetectionProcessor'}, {'checksum': 2243762925, 'executionDate': '2021-06-04T22:22:07.646064', 'executionTime': 0.5, 'module': 'clarifai-demographics-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoEthnicityDetectionProcessor'}, {'checksum': 3888650313, 'executionDate': '2021-06-04T22:22:08.122492', 'executionTime': 0.42, 'module': 'clarifai-demographics-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoAgeDetectionProcessor'}, {'checksum': 518854999, 'executionTime': 0, 'module': 'clarifai-unsafe-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiModerationDetectionProcessor'}, {'checksum': 2688293710, 'executionDate': '2021-06-04T22:22:08.910029', 'executionTime': 0.7, 'module': 'clarifai-unsafe-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoModerationDetectionProcessor'}, {'checksum': 2369525462, 'executionTime': 0, 'module': 'clarifai-logo-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiLogoDetectionProcessor'}, {'checksum': 47192269, 'executionDate': '2021-06-04T22:22:11.843559', 'executionTime': 2.84, 'module': 'clarifai-logo-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoLogoDetectionProcessor'}, {'checksum': 3540195338, 'executionTime': 0, 'module': 'clarifai-apparel-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiApparelDetectionProcessor'}, {'checksum': 1316755969, 'executionDate': '2021-06-04T22:22:12.507937', 'executionTime': 0.57, 'module': 'clarifai-apparel-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoApparelDetectionProcessor'}, {'checksum': 2319455924, 'executionTime': 0, 'module': 'clarifai-face-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiFaceDetectionProcessor'}, {'checksum': 4291106987, 'executionDate': '2021-06-04T22:22:13.164034', 'executionTime': 0.56, 'module': 'clarifai-face-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoFaceDetectionProcessor'}, {'checksum': 3169851315, 'executionTime': 0, 'module': 'clarifai-travel-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiTravelDetectionProcessor'}, {'checksum': 913447338, 'executionDate': '2021-06-04T22:22:39.481597', 'executionTime': 5.51, 'module': 'clarifai-travel-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoTravelDetectionProcessor'}, {'checksum': 3537967111, 'executionTime': 0, 'module': 'clarifai-wedding-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiWeddingDetectionProcessor'}, {'checksum': 1314527742, 'executionDate': '2021-06-04T22:22:40.820015', 'executionTime': 1.2, 'module': 'clarifai-wedding-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoWeddingDetectionProcessor'}, {'checksum': 2717390629, 'executionTime': 0, 'module': 'clarifai-label-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiLabelDetectionProcessor'}, {'checksum': 428022044, 'executionDate': '2021-06-04T22:22:41.267516', 'executionTime': 0.34, 'module': 'clarifai-label-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoLabelDetectionProcessor'}, {'checksum': 2356483789, 'executionTime': 0, 'module': 'clarifai-food-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiFoodDetectionProcessor'}, {'checksum': 34150596, 'executionDate': '2021-06-04T22:22:41.872571', 'executionTime': 0.51, 'module': 'clarifai-food-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoFoodDetectionProcessor'}, {'checksum': 3981252743, 'executionTime': 0, 'module': 'clarifai-nsfw-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiExplicitDetectionProcessor'}, {'checksum': 1790777982, 'executionDate': '2021-06-04T22:22:42.380311', 'executionTime': 0.41, 'module': 'clarifai-nsfw-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoExplicitDetectionProcessor'}, {'checksum': 79829224, 'executionTime': 0, 'module': 'clarifai-celebrity-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiCelebrityDetectionProcessor'}, {'checksum': 2216303327, 'executionDate': '2021-06-04T22:22:43.462900', 'executionTime': 0.99, 'module': 'clarifai-celebrity-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoCelebrityDetectionProcessor'}, {'checksum': 4036237481, 'executionTime': 0, 'module': 'clarifai-texture-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiTexturesDetectionProcessor'}, {'checksum': 1845762720, 'executionDate': '2021-06-04T22:22:44.097769', 'executionTime': 0.54, 'module': 'clarifai-texture-detection', 'processor': 'boonai_analysis.clarifai.ClarifaiVideoTexturesDetectionProcessor'}]}, 'source': {'checksum': 729522015, 'extension': 'mp4', 'filename': 'death-valley-trail.mp4', 'filesize': 1563565, 'mimetype': 'video/mp4', 'path': 'gs://zorroa-public/demo-files/death-valley-trail.mp4'}, 'system': {'dataSourceId': '50e7090b-4d4a-11ed-8567-0a55d0605eaa', 'jobId': '50e7090c-4d4a-11ed-8567-0a55d0605eaa', 'projectId': '1c4e7b80-1ca0-4296-b051-3b8d05947eef', 'state': 'Analyzed', 'taskId': '50e7090d-4d4a-11ed-8567-0a55d0605eaa', 'timeCreated': '2021-01-14T21:33:11.317226Z', 'timeModified': '2021-06-04T22:22:44.358203Z'}}, 'id': asset_id, 'page': None, 'uri': 'gs://zorroa-public/demo-files/death-valley-trail.mp4'}  # noqa

        monkeypatch.setattr(BoonClient, 'post', mock_response)
        url = reverse('asset-analyze', kwargs={'project_pk': project.id, 'pk': asset_id})
        response = check_response(api_client.patch(url, {'modules': ['clarifai-age-detection']}))
        assert set(response.keys()) == {'id', 'metadata'}
        assert response['id'] == asset_id
        assert 'clarifai-age-detection' in response['metadata']['analysis']


class TestDetectFacesAction:
    def test_detect_faces_action(self, login, project, monkeypatch, api_client):
        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

        def mock_response(*args, **kwargs):
            return {'document': {'analysis': {'aws-label-detection': {'count': 8, 'predictions': [{'label': 'Plant', 'score': 99.712}, {'label': 'Flower', 'score': 98.409}, {'label': 'Blossom', 'score': 98.409}, {'label': 'Tulip', 'score': 88.556}, {'label': 'Rose', 'score': 69.329}, {'label': 'Photo', 'score': 59.959}, {'label': 'Photography', 'score': 59.959}, {'label': 'Petal', 'score': 59.292}], 'type': 'labels'}, 'boonai-face-detection': {'count': 0, 'predictions': [], 'type': 'labels'}, 'burger-types': {'label': 'Wendys', 'score': 0.625, 'type': 'single-label'}, 'clarifai-general-model': {'count': 20, 'predictions': [{'label': 'tulip', 'score': 0.997}, {'label': 'nature', 'score': 0.994}, {'label': 'flower', 'score': 0.992}, {'label': 'garden', 'score': 0.992}, {'label': 'leaf', 'score': 0.985}, {'label': 'flora', 'score': 0.985}, {'label': 'floral', 'score': 0.973}, {'label': 'no person', 'score': 0.969}, {'label': 'summer', 'score': 0.969}, {'label': 'love', 'score': 0.96}, {'label': 'blooming', 'score': 0.959}, {'label': 'romance', 'score': 0.959}, {'label': 'field', 'score': 0.955}, {'label': 'bright', 'score': 0.95}, {'label': 'petal', 'score': 0.948}, {'label': 'bouquet', 'score': 0.947}, {'label': 'Easter', 'score': 0.939}, {'label': 'bulb', 'score': 0.935}, {'label': 'park', 'score': 0.933}, {'label': 'color', 'score': 0.93}], 'type': 'labels'}, 'gcp-vision-label-detection': {'count': 10, 'predictions': [{'label': 'Flower', 'score': 0.996}, {'label': 'Flowering plant', 'score': 0.985}, {'label': 'Petal', 'score': 0.977}, {'label': 'Tulip', 'score': 0.972}, {'label': 'Plant', 'score': 0.964}, {'label': 'Pink', 'score': 0.948}, {'label': 'Natural environment', 'score': 0.893}, {'label': 'Spring', 'score': 0.88}, {'label': 'Plantation', 'score': 0.876}, {'label': 'Plant stem', 'score': 0.866}], 'type': 'labels'}, 'zvi-image-similarity': {'simhash': 'PDPMLHECJPCAPFCPPAJCHHPLFIMABFPPIKFCBEFPDBPPPPPNPPCIPDPPPDHPGGOPJBKPGPDDENHACPBCFPMBAHMBELKHECHODPKLGALKCBHAGCCJJKPBHGAKPIIPBPAHCPOBEPALOJPPPPCDGBNJLBNAPNCCDBOAAJDBOPAPGPOBPAIIIBNPAPAKNPPOAPJGNEAFJPBAAFCMHIFGCPPHPPKPPDBEKKPAFDPDPBHLNLNIPJPHPAEFNFPPFPKAKPHHOLDPAAPBIPLPPHCPMJHEPABJFBJMADPPPPDMBKDFPPAAPAPBPADIPBAPDPPHANLAAHCDPEEPPPPIEKALFAPBPBKFOPNBACPPBBAPPIPJKCPPBAEEPPPKDPDIDPDPBDFPPNPPLLMJMLPPMIINPIAAEMNAOPIPKGPPPOAPPPAIBGFOCEPIGDPFMAFAEPPFEAPPCCAPPMEPPPPPBHAPPCKAPOBPPLBCGAHEDPCBEDPAAILJPPPPPIIPABPCPEPMPPHAPKFHAAPAPPLDJPLAGPNPCGIAFPIDDJJPBPLPCPGCPPAFEPPANPJBPGFHAPHFPHPPPAPJBPPAKPBAPPAPBPMLLFCAPPEPAPFECHIPANABPPNOOPPACKPPPCPANKBPPAPABMNPDDPPPKABALABPHHHJDPPPPDOPPAMPPBEPPACPPPABDDGMJPMMHGPPBFPAJAIAPLHKHLPGHNPAKIHDPBACPAPPIIADADPLEPABECMPPBCBGPAGPDNEPCCEPFMAPPFCPPHAPBPJPCPGALEDPAMPBPDAAFEHPPAGABBLMPAPBIJAAPPPPPLAPPPPPPAPCACPACDPDBPBOHHOHPPAPPCPJPCDAPMPPPAGDBAELICOKNPFCPBPLPPPPBKPPPAPGAADMPPPLPPAPPJPEPPADJADABPAFPGPPKEPKDAPPPPALBBAPGPBPPPDPPLBBFAECPBBCPAIKAPOPDAAFDHIDHPDPBKDHBPGPKPIFNLCLAPOAPBGPPPLIMBNPMEJDPIPNGGAIDEPPPEPKHGFPFPPOKDAPHMAPIPPMPPOPPPCCBDFLAJAPPPFPBMMPPPHPPEAPPBAKJAJNPPAPCNBJNDPLOCHPPPPPPEAPPBMIALFAACCFBPFPFPPPPFACBPPPBDPPGBAAPKBPMEDJCPKKLBIHPPKAGAEKCKPICEDKHAPCPCPPPFAPAPPACAMIPPBBEHCPEKJELEAOPPAIBPPDANPPDMECPDAPPFDPJHPFJBCBABFFAPAPPCHAAPHGJABGDIAEOCFIJPAPHPBAPLPPIPDABPAMPPABNAIPEIPPPEGEJPKABIAPOKDAMAMPAHPKPPAPAEPCFPHJAPPPKFANPMOPPIPAAGCFLIFBPPHFHCPAIBPACGPBGPLFCPPKPPBLPPGPPCPLDPGDBPAPCJBAPDPPMBPEBPPPMPEPHLPGCAHADPAGKPLFJPPLPLPNAPFPNAPPPPCJBPPAPKPPLLPPCBAGENEAGPBAJPBFHPAPLPADFBPPDPPGPPPCIHANGPFDPPLEKAPHGDNCPENFOIHLPDAPHGPPEPEAJPCHPCAPPDPAOMMCPHNPACACFAEPPIOBPPHPCHLPAPPPPPPPAPPEPJKJPJEGPAPMAPGCPAPDEPIPLCMPAHILAPPGIJPPAPPPCLLKMAJJAPADIPBMBFAHGPDEJIFPMKAPPGPPKBPABOPOAPBAPPPPLCPNLEJAPPPJBPAEPPPDPFPCFILAAAPJPKCFBPAPAELPGHFMACDFPLALCLPDPJNPDOABEPFBPPAFBAAAPLAPPIKAPNPLPAPMPPFPLABLEGAMAPPIALDKPPPMNDAFIPPDPPPPPCBAPNAGPNPJAANFKBCNPAFPAEBFIHGGBFFPPAAKFAFLJPPPDAAAKADAIALAEPAHPCNKHDPPABAPPPJPPAPMPMGPMJAPHPDPGCACGLOPHACNBPAPMPPPANPAEGAPPOBJHAPBMAPELMFEPPBOPCFIIACAGLPMNPNKNHPFLPDJAEAEJPDBPPPNPEBPELIBAEAADAPICEKAPLPMBP', 'type': 'similarity'}, 'zvi-label-detection': {'count': 1, 'predictions': [{'label': 'pot', 'score': 0.193}], 'type': 'labels'}}, 'files': [{'attrs': {'height': 331, 'width': 500}, 'category': 'proxy', 'id': 'assets/up4YQOb7SMZ4ng7NclXSGxVZCzHEjjSE/proxy/image_500x331.jpg', 'mimetype': 'image/jpeg', 'name': 'image_500x331.jpg', 'size': 152644}, {'attrs': {'height': 331, 'width': 500}, 'category': 'web-proxy', 'id': 'assets/up4YQOb7SMZ4ng7NclXSGxVZCzHEjjSE/web-proxy/web-proxy.jpg', 'mimetype': 'image/jpeg', 'name': 'web-proxy.jpg', 'size': 52202}], 'media': {'aspect': 1.51, 'height': 331, 'length': 1, 'orientation': 'landscape', 'type': 'image', 'width': 500}, 'metrics': {'pipeline': [{'checksum': 2178814325, 'executionDate': '2020-09-17T01:40:34.540516', 'executionTime': 0.11, 'module': 'standard', 'processor': 'zmlp_core.core.PreCacheSourceFileProcessor'}, {'checksum': 117837444, 'executionDate': '2020-09-17T01:40:36.199635', 'executionTime': 0.24, 'module': 'standard', 'processor': 'zmlp_core.core.FileImportProcessor'}, {'checksum': 457707303, 'executionDate': '2020-09-17T01:40:40.421922', 'executionTime': 1.03, 'module': 'standard', 'processor': 'zmlp_core.proxy.ImageProxyProcessor'}, {'checksum': 482873147, 'executionTime': 0, 'module': 'standard', 'processor': 'zmlp_core.proxy.VideoProxyProcessor'}, {'checksum': 1879445844, 'executionDate': '2020-09-17T01:40:59.963169', 'executionTime': 0.61, 'module': 'standard', 'processor': 'zmlp_analysis.zvi.ZviSimilarityProcessor'}, {'checksum': 3329037091, 'executionDate': '2020-09-17T01:41:14.942655', 'executionTime': 0.85, 'module': 'zvi-object-detection', 'processor': 'zmlp_analysis.zvi.ZviObjectDetectionProcessor'}, {'checksum': 2989691564, 'executionDate': '2020-09-17T01:41:28.456145', 'executionTime': 0.39, 'module': 'zvi-label-detection', 'processor': 'zmlp_analysis.zvi.ZviLabelDetectionProcessor'}, {'checksum': 2199264714, 'executionDate': '2020-09-17T01:41:32.302266', 'executionTime': 0.23, 'module': 'clarifai-general-model', 'processor': 'zmlp_analysis.clarifai.ClarifaiLabelDetectionProcessor'}, {'checksum': 2975666803, 'executionDate': '2020-09-17T01:41:37.529211', 'executionTime': 0.37, 'module': 'gcp-label-detection', 'processor': 'zmlp_analysis.google.CloudVisionDetectLabels'}, {'checksum': 2678460964, 'executionDate': '2020-09-17T01:41:39.067327', 'executionTime': 0.23, 'module': 'gcp-logo-detection', 'processor': 'zmlp_analysis.google.CloudVisionDetectLogos'}, {'checksum': 2686980654, 'executionDate': '2020-09-17T01:41:42.313148', 'executionTime': 1.31, 'module': 'aws-label-detection', 'processor': 'zmlp_analysis.aws.RekognitionLabelDetection'}, {'checksum': 2689607761, 'executionDate': '2020-09-22T15:41:36.634083', 'executionTime': 0.02, 'module': 'burger-types', 'processor': 'zmlp_analysis.custom.KnnLabelDetectionClassifier'}, {'checksum': 4171961359, 'executionDate': '2021-06-14T22:56:52.654692', 'executionTime': 0.16, 'module': 'boonai-face-detection', 'processor': 'boonai_analysis.boonai.ZviFaceDetectionProcessor'}]}, 'source': {'checksum': 823151467, 'extension': 'jpg', 'filename': '9019694597_2d3bbedb17.jpg', 'filesize': 152644, 'mimetype': 'image/jpeg', 'path': 'gs://zorroa-public/datasets/flowers/tulips/9019694597_2d3bbedb17.jpg'}, 'system': {'dataSourceId': 'fdbfe55a-ab0e-113e-b198-2a5dc6fb1e19', 'jobId': 'fdbfe55b-ab0e-113e-b198-2a5dc6fb1e19', 'projectId': '1a0cbcd6-cf49-4992-a858-7966400082da', 'state': 'Analyzed', 'taskId': 'fdbfe55c-ab0e-113e-b198-2a5dc6fb1e19', 'timeCreated': '2020-09-16T22:59:04.992594Z', 'timeModified': '2020-09-22T15:41:36.999171Z'}}, 'id': asset_id, 'page': None, 'uri': 'gs://zorroa-public/datasets/flowers/tulips/9019694597_2d3bbedb17.jpg'}  # noqa

        monkeypatch.setattr(BoonClient, 'post', mock_response)
        url = reverse('asset-detect-faces', kwargs={'project_pk': project.id, 'pk': asset_id})
        response = check_response(api_client.patch(url))
        assert set(response.keys()) == {'id', 'metadata'}
        assert response['id'] == asset_id
        assert 'boonai-face-detection' in response['metadata']['analysis']
