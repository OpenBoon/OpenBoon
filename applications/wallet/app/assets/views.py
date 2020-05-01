import mimetypes

import requests
from django.http import StreamingHttpResponse
from rest_framework.decorators import action
from rest_framework.response import Response

from assets.serializers import AssetSerializer
from assets.utils import AssetBoxImager
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


def asset_modifier(request, item):
    if '_source' in item:
        item['id'] = item['_id']
        item['metadata'] = item['_source']
        # We don't need to be passing around the data we just duplicated
        del(item['_id'])
        del(item['_source'])
    else:
        item['metadata'] = item['document']

    if 'files' not in item['metadata']:
        item['metadata']['files'] = []


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

    @action(detail=True, methods=['get'])
    def box_images(self, request, project_pk, pk):
        """Special action that returns a portion of the Asset's metadata with base64 encoded
images anywhere it finds a "bbox" key. When a bbox key is found an image that represents that
box is generated and added to the metadata next to the "bbox" key as "b64_image". By default
the entire "analysis" section is returned but the query param "attr" can be used to return
only a specific section of the metadata.

Available Query Params:

- *width* - Width of the images generated in pixels.
- *attr* - Dot-notation path to the attr of the metadata to create box images for. Below
is an example querystring for getting the "zvi-object-detection" section of the metadata shown.

Metadata:

    {
      "analysis": {
        "zvi-object-detection": {
          ...
        }
      }
    }

Querystring:

    ?attr=analysis.zvi-object-detection&width=128

"""
        asset = request.app.assets.get_asset(pk)
        imager = AssetBoxImager(asset, request.client)
        attr = request.query_params.get('attr', 'analysis')
        width = int(request.query_params.get('width', 255))
        response_data = {attr.split('.')[-1]: imager.get_attr_with_box_images(attr, width=width)}
        return Response(response_data)


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
