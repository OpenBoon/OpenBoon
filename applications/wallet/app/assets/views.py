import mimetypes

from django.http import StreamingHttpResponse
from django.urls import reverse
from django.utils.cache import patch_response_headers, patch_cache_control
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from assets.serializers import AssetSerializer
from assets.utils import AssetBoxImager, get_best_fullscreen_file_data
from projects.viewsets import BaseProjectViewSet, ZmlpRetrieveMixin, ZmlpDestroyMixin, \
    ZmlpListMixin, ListViewType
from searches.utils import FilterBuddy


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


def analyze_asset(request, asset_id, modules, index=True):
    """Does on-the-fly ML analysis on an asset. This function is intended to be used within a
    DRF view. It expects a request as an arg and returns a Response.

    Args:
        request (Request): Request passed to a view using this function.
        asset_id (str): UUID of the asset to analyze.
        modules (list<str>): List of module names to run on the asset.
        index (bool): If True the results will be added to the metadata for the asset.

    Returns:
        Response: Response with asset data ready to be returned by a view..
    """
    from assets.views import asset_modifier
    payload = {'assetId': asset_id, 'modules': modules, 'index': index}
    asset = request.client.post('/ml/v1/pipelines/apply-modules-to-asset', payload)
    asset_modifier(request, asset)
    return Response(AssetSerializer(asset).data)


class AssetViewSet(ZmlpListMixin,
                   ZmlpRetrieveMixin,
                   ZmlpDestroyMixin,
                   BaseProjectViewSet):
    zmlp_root_api_path = 'api/v3/assets/'
    serializer_class = AssetSerializer
    list_type = ListViewType.ES
    list_modifier = staticmethod(asset_modifier)
    retrieve_modifier = staticmethod(asset_modifier)

    @action(detail=True, methods=['get'])
    def box_images(self, request, project_pk, pk):
        """Special action that returns a portion of the Asset's metadata with base64 encoded
        images anywhere it finds a "bbox" key. When a bbox key is found an image that represents
        that box is generated and added to the metadata next to the "bbox" key as "b64Image".
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

    @action(detail=True, methods=['put', 'patch'])
    def detect_faces(self, request, project_pk, pk):
        """Runs face detection analysis on the asset and adds the results to the database."""
        return analyze_asset(request, pk, ['boonai-face-detection'], index=True)

    @action(detail=True, methods=['put', 'patch'])
    def analyze(self, request, project_pk, pk):
        """Runs analysis modules on an asset immediately. Requires a list of analysis module names
        to be sent in the payload. It also excepts an option bool arg, "index". If "index" is true
        then the results of the analysis will be added to the metadata of the asset.

        A list of available modules can be gathered from the
        /api/v1/projects/<ID>>/providers/ endpoint. The new complete metadata for the asset is
        returned.

        Expected Body:

            {
                "modules": ["azure-image-description"],
                "index": true
            }

        """
        return analyze_asset(request, pk, request.data.get('modules'),
                             index=request.data.get('index'))

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
        highlight_clips = self._get_highlight_clips(request, pk)
        highlighted_content = self._highlight_content(formatted_content, highlight_clips)
        return Response(highlighted_content)

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

    def _get_highlight_clips(self, request, pk):
        """Helper for returning only the clips that matched the user's original query."""
        base_path = f'{self.zmlp_root_api_path}{pk}/clips'
        query = self._build_clip_query_from_querystring(request)
        if query:
            return self._zmlp_get_all_content_from_es_search(request, base_url=base_path,
                                                             search_filter=query)
        else:
            return {}

    def _build_clip_query_from_querystring(self, request):
        filter_boy = FilterBuddy()

        _filters = filter_boy.get_filters_from_request(request)
        for _filter in _filters:
            _filter.is_valid(query=True, raise_exception=True)
        return filter_boy.reduce_filters_to_clip_query(_filters)

    def _get_formatted_timelines(self, content):
        """Helper to format the clip search response from boonsdk into the JSON response for the UI"""
        # Organize the detections into a more helpful state
        data = {}
        for entry in content:
            clip = entry['_source']['clip']
            timeline = clip['timeline']
            track = clip['track']
            start = clip['start']
            stop = clip['stop']
            data.setdefault(timeline, {}).setdefault(track, []).append({'start': start,
                                                                        'stop': stop,
                                                                        'highlight': False})

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

    def _highlight_content(self, formatted_content, highlight_clips):
        """Helper for marking all the highlighted clips in the formatted json response."""
        # Too much brute force going on here.
        for clip in highlight_clips:
            _clip = clip['_source']['clip']
            timeline = _clip['timeline']
            track = _clip['track']
            start = _clip['start']
            stop = _clip['stop']
            # Check all timelines
            for content_timeline in formatted_content:
                if timeline == content_timeline['timeline']:
                    # Check the tracks in this timeline
                    for content_track in content_timeline['tracks']:
                        if track == content_track['track']:
                            # Check the hits in this track for a match
                            for content_hit in content_track['hits']:
                                if start == content_hit['start'] and stop == content_hit['stop']:
                                    # Phew, found it. Mark it as highlighted.
                                    content_hit['highlight'] = True
                                    break
                            break
                    break

        return formatted_content


class WebVttViewSet(BaseProjectViewSet):
    zmlp_root_api_path = 'api/v3/assets'
    lookup_value_regex = '[^/]+'

    def retrieve(self, request, project_pk, asset_pk, pk):
        """Streams the webvtt for the specified vtt file."""
        if pk == 'all.vtt':
            path = f'{self.zmlp_root_api_path}/{asset_pk}/clips/{pk}'
        else:
            path = f'{self.zmlp_root_api_path}/{asset_pk}/clips/timelines/{pk}'
        response = StreamingHttpResponse(self.stream_zmlp_endpoint(path), content_type='text/vtt')
        patch_response_headers(response, cache_timeout=86400)
        patch_cache_control(response, private=True)
        return response


class FileCategoryViewSet(BaseProjectViewSet):
    pass


class FileNameViewSet(BaseProjectViewSet):
    zmlp_root_api_path = 'api/v3/files'
    lookup_value_regex = '[^/]+'

    def retrieve(self, request, project_pk, asset_pk, category_pk, pk):
        path = f'{self.zmlp_root_api_path}/_stream/assets/{asset_pk}/{category_pk}/{pk}'
        content_type, encoding = mimetypes.guess_type(pk)
        response = StreamingHttpResponse(self.stream_zmlp_endpoint(path), content_type=content_type)
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
