import os
import requests
import mimetypes
from rest_framework.response import Response
from django.http import StreamingHttpResponse

from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


def asset_modifier(request, item, many=True):
    current_url = request.build_absolute_uri(request.path)
    if many:
        item['id'] = item['_id']
        item['metadata'] = item['_source']
        del (item['_id'])
        del (item['_index'])
        del (item['_score'])
        del (item['_source'])
        del (item['_type'])
    else:
        # Normalize the current URL
        current_url = current_url.replace(f'{item["id"]}/', '')
        item['metadata'] = item['document']
        del (item['analyzed'])
        del (item['document'])

    # Now add the asset id
    current_url = f'{current_url}{item["id"]}/'
    item['url'] = current_url

    # Add urls to the proxy files
    for entry in item['metadata']['files']:
        entry['url'] = f'{current_url}files/category/{entry["category"]}/name/{entry["name"]}'

    # Add url for the source file
    item['metadata']['source']['url'] = f'{current_url}files/source/{item["metadata"]["source"]["filename"]}'  # noqa


def stream(request, path):
    response = requests.get(request.client.get_url(path), verify=False,
                            headers=request.client.headers(), stream=True)
    for block in response.iter_content(1024):
        yield block


class AssetViewSet(BaseProjectViewSet):

    zmlp_only = True
    zmlp_root_api_path = 'api/v3/assets/'
    pagination_class = ZMLPFromSizePagination

    def list(self, request, project_pk):
        return self._zmlp_list_from_es(request, item_modifier=asset_modifier)

    def retrieve(self, request, project_pk, pk):
        response = request.client.get(os.path.join(self.zmlp_root_api_path, pk))
        content = self._get_content(response)
        asset_modifier(request, content, many=False)
        return Response(content)


class SourceFileViewSet(BaseProjectViewSet):

    zmlp_only = True
    zmlp_root_api_path = 'api/v3/assets'
    lookup_value_regex = '[^/]+'

    def retrieve(self, request, project_pk, asset_pk, pk):
        path = f'{self.zmlp_root_api_path}/{asset_pk}/_stream'
        content_type, encoding = mimetypes.guess_type(pk)
        return StreamingHttpResponse(stream(request, path), content_type=content_type)


class FileCategoryViewSet(BaseProjectViewSet):

    zmlp_only = True


class FileNameViewSet(BaseProjectViewSet):

    zmlp_only = True
    zmlp_root_api_path = 'api/v3/assets'
    lookup_value_regex = '[^/]+'

    def retrieve(self, request, project_pk, asset_pk, category_pk, pk):
        path = f'{self.zmlp_root_api_path}/{asset_pk}/_files/{category_pk}/{pk}'
        content_type, encoding = mimetypes.guess_type(pk)
        return StreamingHttpResponse(stream(request, path), content_type=content_type)
