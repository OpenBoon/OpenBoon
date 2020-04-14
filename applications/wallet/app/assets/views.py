import mimetypes

import requests
from django.http import StreamingHttpResponse
from djangorestframework_camel_case.render import CamelCaseBrowsableAPIRenderer
from flatten_dict import flatten
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework_csv.renderers import CSVRenderer

from assets.serializers import AssetSerializer, MetadataExportSerializer
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
    zmlp_root_api_path = 'api/v3/assets'
    lookup_value_regex = '[^/]+'

    def retrieve(self, request, project_pk, asset_pk, category_pk, pk):
        path = f'{self.zmlp_root_api_path}/{asset_pk}/_files/{category_pk}/{pk}'
        content_type, encoding = mimetypes.guess_type(pk)
        return StreamingHttpResponse(stream(request, path), content_type=content_type)


class MetadataExportViewSet(BaseProjectViewSet):
    """Exports asset metadata as CSV file."""
    renderer_classes = [CSVRenderer, CamelCaseBrowsableAPIRenderer]
    serializer_class = MetadataExportSerializer

    def _search_for_assets(self, request):
        """Testing seam that returns the results of an asset search."""
        return request.app.assets.search().assets

    def create(self, request, project_pk):
        def dot_reducer(k1, k2):
            """Reducer function used by the flatten method to combine nested dict keys with dots."""
            if k1 is None:
                return k2
            else:
                return k1 + "." + k2

        # Create a list of flat dictionaries that represent the metadata for each asset.
        assets = self._search_for_assets(request)
        flat_assets = []
        for asset in assets:
            flat_asset = flatten(asset.document, reducer=dot_reducer)
            flat_asset['id'] = asset.id
            flat_assets.append(flat_asset)

        # Return the CSV file to the client.
        return Response(flat_assets)
