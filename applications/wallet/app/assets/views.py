import mimetypes
import requests

from django.http import StreamingHttpResponse
from django.urls import reverse
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
    def urls(self, request, project_pk, pk):
        """Returns the set of urls used for the media and webvtt files"""
        urls = {'signedUrl': {},
                'tracks': []}
        # Query for the asset details to get it's metadata and list of files
        detail_response = self.retrieve(request, project_pk, pk)

        # Figure out the signed url
        # Determine the best file to display in fullscreen
        _file = get_best_fullscreen_file_data(detail_response.data)
        if not _file:
            urls['signedUrl'] = {'uri': '/icons/fallback_3x.png',
                                 'mediaType': 'image/png'}
        else:
            path = f'{FileNameViewSet.zmlp_root_api_path}/_sign/{_file["id"]}'
            urls['signedUrl'] = request.client.get(path)

        # Figure out the tracks
        # Get all the clips so we know what vtt's to add
        timelines = self._get_list_of_timelines(request, pk)
        # clips = self._get_all_clips(request, pk)
        # timelines = self._get_formatted_timelines(clips)
        for timeline in timelines:
            track = {'label': timeline,
                     'kind': 'metadata',
                     'src': reverse('webvtt-detail', kwargs={'project_pk': project_pk,
                                                             'asset_pk': pk,
                                                             'pk': f'{timeline}.vtt'})}
            urls['tracks'].append(track)

        # Add the closed captioning files
        files = detail_response.data.get('metadata').get('files')
        caption_files = [_file for _file in files if _file['category'] == 'captions']
        for caption in caption_files:
            name = caption['name']
            label = name.split('.')[0].replace('-transcription', '').replace('-', ' ').title()
            get_signed_url = f'{FileNameViewSet.zmlp_root_api_path}/_sign/{caption["id"]}'
            signed_url = request.client.get(get_signed_url)
            track = {'label': label,
                     'kind': 'captions',
                     'src': signed_url['uri']}
            urls['tracks'].append(track)

        return Response(data=urls)

    @action(detail=True, methods=['get'])
    def timelines(self, request, project_pk, pk):
        """Returns the time based metadata in timeline format."""
        content = self._get_all_clips(request, pk)
        formatted_content = self._get_formatted_timelines(content)
        return Response(formatted_content)

    def _get_list_of_timelines(self, request, pk):
        """Helper to aggregate all the available timelines on this clip."""
        base_path = f'{self.zmlp_root_api_path}{pk}/clips/_search'
        agg_query = {
            'size': 0,
            'aggs': {
                'timelines': {
                    'terms': {
                        'field': 'clip.timeline',
                        'size': 1000
                    }
                }
            }
        }
        result = request.client.post(base_path, agg_query)
        timeline_buckets = result.get(
            'aggregations', {}).get(
            'sterms#timelines', {}).get(
            'buckets', [])
        return [bucket.get('key') for bucket in timeline_buckets]

    def _get_all_clips(self, request, pk):
        """Helper to return all the timelines/clips for an asset"""
        base_path = f'{self.zmlp_root_api_path}{pk}/clips'
        return self._zmlp_get_all_content_from_es_search(request, base_url=base_path, default_page_size=200)

    def _get_formatted_timelines(self, content):
        """Helper to format the clip search response from ZMLP into the JSON response for the UI"""
        # Organize the detections into a more helpful state
        data = {}
        for entry in content:
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


class WebVttViewSet(BaseProjectViewSet):
    zmlp_only = True
    zmlp_root_api_path = 'api/v3/assets'
    lookup_value_regex = '[^/]+'

    def retrieve(self, request, project_pk, asset_pk, pk):
        """Streams the webvtt for the specified vtt file."""
        if pk == 'all.vtt':
            path = f'{self.zmlp_root_api_path}/{asset_pk}/clips/{pk}'
        else:
            path = f'{self.zmlp_root_api_path}/{asset_pk}/clips/timelines/{pk}'
        response = StreamingHttpResponse(stream(request, path), content_type='text/vtt')
        patch_response_headers(response, cache_timeout=86400)
        patch_cache_control(response, private=True)
        return response


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
