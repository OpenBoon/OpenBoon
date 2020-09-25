import mimetypes
import requests

from django.http import StreamingHttpResponse
from django.utils.cache import patch_response_headers, patch_cache_control
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from assets.serializers import AssetSerializer
from assets.utils import AssetBoxImager, get_best_fullscreen_file_data
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


def asset_modifier(request, item):
    if '_source' in item:
        item['id'] = item['_id']
        item['metadata'] = item['_source']
        # No need to be passing around the data we just duplicated
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

    def destroy(self, request, project_pk, pk):
        path = f'{self.zmlp_root_api_path}/{pk}'
        response = request.client.delete(path)
        if response.get('success'):
            return Response(status=status.HTTP_204_NO_CONTENT)
        else:
            # This may never be used as it doesn't seem like the ZMLP endpoint ever
            # returns a non-success response.
            return Response(data={'detail': ['Unable to delete asset.']},
                            status=status.HTTP_500_INTERNAL_SERVER_ERROR)

    @action(detail=True, methods=['get'])
    def box_images(self, request, project_pk, pk):
        """Special action that returns a portion of the Asset's metadata with base64 encoded
        images anywhere it finds a "bbox" key. When a bbox key is found an image that represents
        that box is generated and added to the metadata next to the "bbox" key as "b64_image".
        By default the entire "analysis" section is returned but the query param "attr" can be
        used to return a specific section of the metadata.

        Available Query Params:

        - *width* - Width of the images generated in pixels.
        - *attr* - Dot-notation path to the attr of the metadata to create box images for. Below
        is an example querystring for getting the "zvi-object-detection" section of the metadata
        shown.

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

    @action(detail=True, methods=['get'])
    def signed_url(self, request, project_pk, pk):
        """Helper action to try to determine the best file and return the signed url for it."""
        # Query for the asset details to get the assets list of files.
        detail_response = self.retrieve(request, project_pk, pk)

        # Determine the best file to display in fullscreen
        _file = get_best_fullscreen_file_data(detail_response.data)
        if not _file:
            return Response(status=status.HTTP_200_OK, data={'uri': '/icons/fallback_3x.png',
                                                             'mediaType': 'image/png'})

        # Query for it's signed url
        path = f'{FileNameViewSet.zmlp_root_api_path}/_sign/{_file["id"]}'
        response = request.client.get(path)
        return Response(status=status.HTTP_200_OK, data=response)

    @action(detail=True, methods=['get'])
    def timelines(self, request, project_pk, pk):
        """Returns the time based metadata in timeline format."""
        base_path = f'{self.zmlp_root_api_path}{pk}/clips'
        content = self._zmlp_get_content_from_es_search(request, base_url=base_path)
        formatted_content = self._get_formatted_timelines(content)
        return Response(formatted_content)

    def _get_formatted_timelines(self, content):
        """Helper to format the clip search response from ZMLP into the JSON response for the UI"""
        # Organize the detections into a more helpful state
        data = {}
        for entry in content['hits']['hits']:
            clip = entry['_source']['clip']
            timeline = clip['timeline']
            track = clip['track']
            start = clip['start']
            stop = clip['stop']
            data.setdefault(timeline, {}).setdefault(track, []).append({'start': start,
                                                                        'stop': stop})

        formatted_timelines = []
        for timeline in data:
            section = {'timeline': timeline,
                       'tracks': []}
            for track in data[timeline]:
                track_section = {'track': track,
                                 'hits': data[timeline][track]}
                section['tracks'].append(track_section)
            formatted_timelines.append(section)
        return formatted_timelines


class FileCategoryViewSet(BaseProjectViewSet):
    zmlp_only = True


class FileNameViewSet(BaseProjectViewSet):
    zmlp_only = True
    zmlp_root_api_path = 'api/v3/files'
    lookup_value_regex = '[^/]+'

    def retrieve(self, request, project_pk, asset_pk, category_pk, pk):
        path = f'{self.zmlp_root_api_path}/_stream/assets/{asset_pk}/{category_pk}/{pk}'
        content_type, encoding = mimetypes.guess_type(pk)
        response = StreamingHttpResponse(stream(request, path), content_type=content_type)
        patch_response_headers(response, cache_timeout=86400)
        patch_cache_control(response, private=True)
        return response

    @action(detail=True, methods=['get'])
    def signed_url(self, request, project_pk, asset_pk, category_pk, pk):
        """Retrieves the signed URL for the given asset id."""
        # make the call, bro
        path = f'{self.zmlp_root_api_path}/_sign/assets/{asset_pk}/{category_pk}/{pk}'
        response = request.client.get(path)
        return Response(status=status.HTTP_200_OK, data=response)
