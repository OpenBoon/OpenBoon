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

    def test_get_list(self, project, zvi_project_user, api_client, monkeypatch, list_api_return):

        def mock_api_response(*args, **kwargs):
            return list_api_return

        monkeypatch.setattr(BoonClient, 'post', mock_api_response)
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

        monkeypatch.setattr(BoonClient, 'get', mock_api_response)
        api_client.force_authenticate(zvi_project_user)
        api_client.force_login(zvi_project_user)
        id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        response = api_client.get(reverse('asset-detail', kwargs={'project_pk': project.id,
                                                                  'pk': id}))
        assert response.status_code == 200
        content = response.json()
        assert content['id'] == id
        assert 'metadata' in content

    def test_list_and_detail_resources_match(self, project, zvi_project_user, api_client,
                                             monkeypatch, detail_api_return, list_api_return):

        def mock_list_response(*args, **kwargs):
            return list_api_return

        def mock_detail_response(*args, **kwargs):
            return detail_api_return

        monkeypatch.setattr(BoonClient, 'post', mock_list_response)
        api_client.force_authenticate(zvi_project_user)
        api_client.force_login(zvi_project_user)
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
        check_response(response, status.HTTP_204_NO_CONTENT)

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
        assert content['detail'] == ['Unable to delete asset.']

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
        assert response._headers['cache-control'] == ('Cache-Control', 'max-age=86400, private')
        assert 'expires' in response._headers


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
        assert response._headers['cache-control'] == ('Cache-Control', 'max-age=86400, private')
        assert 'expires' in response._headers

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
    def test_box_images(self, project, monkeypatch, api_client, zvi_project_user,
                        detail_api_return):
        def mock_get_attr_with_box_images(*args, **kwargs):
            return {'count': 1, 'type': 'labels', 'predictions': [{'score': 0.882, 'bbox': [0.068, 0.079, 0.904, 0.739], 'label': 'laptop', 'b64_image': 'data:image/png;base64, iVBORw0KGgoAAAANSUhEUgAAAAoAAAAICAIAAABPmPnhAAAAI0lEQVQIHXXBAQEAAAABIP6PzgJV5CvyFfmKfEW+Il+Rr8g33SQX8fv7NasAAAAASUVORK5CYII='}, {'score': 0.882, 'bbox': [0.068, 0.079, 0.904, 0.739], 'label': 'laptop2', 'b64_image': 'data:image/png;base64, iVBORw0KGgoAAAANSUhEUgAAAAoAAAAICAIAAABPmPnhAAAAI0lEQVQIHXXBAQEAAAABIP6PzgJV5CvyFfmKfEW+Il+Rr8g33SQX8fv7NasAAAAASUVORK5CYII='}]}  # noqa

        monkeypatch.setattr(AssetBoxImager, 'get_attr_with_box_images',
                            mock_get_attr_with_box_images)
        monkeypatch.setattr(BoonClient, 'get', lambda *args: detail_api_return)
        api_client.force_authenticate(zvi_project_user)
        api_client.force_login(zvi_project_user)
        asset_id = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
        base_url = reverse('asset-box-images', kwargs={'project_pk': project.id, 'pk': asset_id})
        response = api_client.get(f'{base_url}?attr=analysis.zvi-object-detection')
        assert response.status_code == 200
        assert 'zvi-object-detection' in response.json()
