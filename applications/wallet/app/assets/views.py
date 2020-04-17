import mimetypes

import requests
from django.http import StreamingHttpResponse
from rest_framework.decorators import action

from assets.serializers import AssetSerializer
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


def asset_modifier(request, item):
    current_url = request.build_absolute_uri(request.path)
    if '_source' in item:
        item['id'] = item['_id']
        item['metadata'] = item['_source']
    else:
        # Normalize the current URL
        current_url = current_url.replace(f'{item["id"]}/', '')
        item['metadata'] = item['document']

    # Now add the asset id
    current_url = f'{current_url}{item["id"]}/'
    item['url'] = current_url

    # Add urls to the proxy files
    if 'files' not in item['metadata']:
        item['metadata']['files'] = []
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
    serializer_class = AssetSerializer

    def list(self, request, project_pk):
        return self._zmlp_list_from_es(request, item_modifier=asset_modifier)

    def retrieve(self, request, project_pk, pk):
        return self._zmlp_retrieve(request, pk, item_modifier=asset_modifier)

    @action(detail=False, methods=['post'])
    def search(self, request, project_pk):
        """Searches the assets for this project with whichever query is given.

        Pagination arguments are expected in the POST body, rather than the querystring.

            Args:
                request (Request): Request the view method was given.
                project_pk (int): The Project ID to search under.

            Returns:
                Response: DRF Response with the results of the search

            """

        return self._zmlp_list_from_es(request, item_modifier=asset_modifier)


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
    zmlp_root_api_path = 'api/v3/files/_stream'
    lookup_value_regex = '[^/]+'

    def retrieve(self, request, project_pk, asset_pk, category_pk, pk):
        path = f'{self.zmlp_root_api_path}/assets/{asset_pk}/{category_pk}/{pk}'
        content_type, encoding = mimetypes.guess_type(pk)
        return StreamingHttpResponse(stream(request, path), content_type=content_type)
