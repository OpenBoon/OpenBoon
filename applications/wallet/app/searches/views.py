from django.http import Http404
from djangorestframework_camel_case.render import CamelCaseBrowsableAPIRenderer, \
    CamelCaseJSONRenderer
from flatten_dict import flatten
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.mixins import (ListModelMixin, RetrieveModelMixin,
                                   CreateModelMixin, UpdateModelMixin, DestroyModelMixin)
from rest_framework.response import Response
from rest_framework.viewsets import GenericViewSet
from rest_framework_csv.renderers import CSVRenderer
from boonsdk.search import AssetSearchScroller

from assets.utils import get_asset_style, get_video_length, get_thumbnail_and_video_urls
from assets.views import asset_modifier
from projects.viewsets import BaseProjectViewSet
from searches.models import Search
from searches.serializers import SearchSerializer, SearchAssetSerializer
from wallet.paginators import FromSizePagination, ZMLPFromSizePagination
from .utils import FieldUtility, FilterBuddy


def search_asset_modifier(request, item):
    asset_modifier(request, item)

    # Set the AssetStyle for the frontend.
    item['assetStyle'] = get_asset_style(item)

    # Set the videoLength
    item['videoLength'] = get_video_length(item)

    # Set thumbnail and video urls
    thumbnail_url, video_proxy_url = get_thumbnail_and_video_urls(request, item)
    item['thumbnailUrl'] = thumbnail_url
    item['videoProxyUrl'] = video_proxy_url

    # Cleanup
    if 'files' in item['metadata']:
        del(item['metadata']['files'])
    if 'media' in item['metadata']:
        del(item['metadata']['media'])


class SearchViewSet(CreateModelMixin,
                    UpdateModelMixin,
                    ListModelMixin,
                    RetrieveModelMixin,
                    DestroyModelMixin,
                    BaseProjectViewSet,
                    GenericViewSet):

    pagination_class = FromSizePagination
    serializer_class = SearchSerializer
    field_utility = FieldUtility()

    def get_object(self):
        try:
            return Search.objects.get(id=self.kwargs['pk'], project=self.kwargs['project_pk'])
        except Search.DoesNotExist:
            raise Http404

    def get_queryset(self):
        return Search.objects.filter(project=self.kwargs['project_pk'])

    def create(self, request, *args, **kwargs):
        if 'project' not in request.data:
            request.data['project'] = kwargs['project_pk']
        # Always correct the created_by value
        request.data['createdBy'] = str(request.user.id)
        return super(SearchViewSet, self).create(request, *args, **kwargs)

    @action(detail=False, methods=['get'])
    def fields(self, request, project_pk):
        """Returns all available fields in the ES index and their type."""

        # This is a temporary fix to remove fields that cause errors.
        restricted_fields = ['clip']

        try:
            fields = self.field_utility.get_filter_map(request.client)
        except ValueError:
            return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR,
                            data={'detail': ['ZMLP did not return field mappings as expected.']})
        for field in restricted_fields:
            if field in fields:
                del fields[field]
        return Response(status=status.HTTP_200_OK, data=fields)

    @action(detail=False, methods=['get'])
    def query(self, request, project_pk):
        """Takes a querystring and runs the query to filter the assets.

        The querystring should be in the form

            ?query=$base64_json_string

        `query` is the querystring key. The value `$base64_json_string` is the JSON
        representation of a list of Filter Objects. The encoded json body should match the
        format:

            [$filter1, $filter2, $filter3]

        An additional, optional, querystring parameter is `fields`, which takes a
        comma-seperated string of field names to include in the quary return

        The JSON Filter Objects you can run queries for are:

        Exists:

            {
                "type": "exists",
                "attribute": "$metadata_attribute_dot_path",
                "values": {
                    "exists": $boolean
                }
            }

        Range:

            {
                "type": "range",
                "attribute": "$metadata_attribute_dot_path",
                "values": {
                    "min": $value,
                    "max": $value
                }
            }

        Facet:

            {
                "type": "facet",
                "attribute": "$metadata_attribute_dot_path",
                "values": {
                    "facets": [$attribute_values_to_filter]
                }
            }

        LabelConfidence:

            {
                "type": "labelConfidence",
                "attribute": "analysis.zvi-label-detection",
                "values": {
                    "labels": ["label1", "label2"],
                    "min": 0.0,  # Allowed minimum is 0.0
                    "max": 1.0,  # Allowed maximum is 1.0
                }
            }

        TextContent:

            {
                "type": "textContent",
                "attribute": "analysis.zvi-text-detection",
                "values": {
                    "query": "Text or ES Simple Query String Pattern to match"
                }
            }

            *NOTE* The "attribute" field on TextContent can also be left off, and the query will
            be matched against all available fields.

        Similarity:

            {
                "type": "similarity",
                "attribute": "analysis.zvi-image-similarity",
                "values": {
                    "ids": ["Asset IDs to match"],
                    "minScore": 0.75,   # Optional
                    "boost": 1.0        # Optional
                }
            }

        Labels:

            {
                'type': 'label',
                'modelId': '$model_id_UUID',
                'values': {
                    'labels': ['Celeste', 'David'],
                    'scope': 'all'
                }
            }

            *Note:* The scope value can be set to 'all', 'train', or 'test'.

        Date:

            {
                "type": "date",
                "attribute": "system.timeCreated",
                "values": {
                    "min": "2020-05-30T00:00:00Z",
                    "max": "2020-07-31T00:00:00Z"
                }
            }

            *Note:* The `min` and `max` values need to be in "yyyy-mm-ddTHH:MM:SSZ"
            format (ISO 8601).

        PredictionCount:

            {
                "type": "predictionCount",
                "attribute": "$metadata_attribute_dot_path",
                "values": {
                    "min": $value,
                    "max": $value
                }
            }

            *Note:* This is meant to be used on LabelConfidence/Prediction attrs. Works
            like a Range filter, but adds the `.count` field to the analysis name to filter
            over the given prediction count.

        Limit:

            {
                "type": "limit",
                "values": {
                    "maxAssets": $value
                }
            }

        """
        path = 'api/v3/assets'
        fields = ['id',
                  'source*',
                  'files*',
                  'media*']

        # Include additionally requested fields in query return
        requested_fields = request.query_params.get('fields')
        if requested_fields is not None:
            fields.extend([f'{x}*' for x in requested_fields.split(',')])

        query = self._build_query_from_querystring(request)

        # Only returns the specified fields in the metadata
        query['_source'] = fields
        query['track_total_hits'] = True

        return self._zmlp_list_from_es(request, search_filter=query, base_url=path,
                                       serializer_class=SearchAssetSerializer,
                                       item_modifier=search_asset_modifier,
                                       pagination_class=ZMLPFromSizePagination)

    def _build_query_from_querystring(self, request):
        """Helper to build the query used for query and raw_query."""
        filter_buddy = FilterBuddy()
        _filters = filter_buddy.get_filters_from_request(request)
        return filter_buddy.finalize_query_from_filters_and_request(_filters, request)

    @action(detail=False, methods=['get'])
    def raw_query(self, request, project_pk):
        """Takes a query querystring and dumps out wha the raw ES query would be."""
        query = self._build_query_from_querystring(request)
        return Response({'results': query})

    @action(detail=False, methods=['get'],
            renderer_classes=[CamelCaseJSONRenderer, CamelCaseBrowsableAPIRenderer])
    def aggregate(self, request, project_pk):
        """Takes a filter querystring and runs the aggregation to populate that filter's UI.

        The querystring should be in the form

            ?filter=$base64_json_string

        `filter` is the querystring key. The value `$base64_json_string` is the JSON
        representation of the filter object, dumped to a string and then Base64 encoded.


        The JSON Filter Objects you can run aggregations for are:

        Range:

            {
                "type": "range",
                "attribute": "$metadata_attribute_dot_path"
            }

        Facet:

            {
                "type": "facet",
                "attribute": "$metadata_attribute_dot_path"
            }

        Facet aggregations can also be sorted and filtered with the additional
        key/values:

            "order": "asc" OR "desc"

            "minimumCount": $integer

        LabelConfidence:

            {
                "type": "labelConfidence",
                "attribute": "analysis.zvi-label-detection"
            }

        Date:

            {
                "type": "date",
                "attribute": "system.timeCreated"
            }

        Labels:

            {
                "type": "labels",
                "modelId": "$model_id_UUID"
            }

        Similar to Facet aggregations, Label aggs can also be sorted and filtered with
        the additional key/values:

            "order": "asc" OR "desc"

            "minimumCount": $integer

        """
        path = 'api/v3/assets/_search'

        filter_service = FilterBuddy()
        _filter = filter_service.get_filter_from_request(request)
        _filter.is_valid(raise_exception=True)

        try:
            response = request.client.post(path, _filter.get_es_agg())
        except NotImplementedError:
            return Response(status=status.HTTP_400_BAD_REQUEST,
                            data={'detail': ['This Filter does not support aggregations.']})

        return Response(status=status.HTTP_200_OK, data=_filter.serialize_agg_response(response))


class MetadataExportViewSet(BaseProjectViewSet):
    """Exports asset metadata as CSV file.

    Notes:
        Disabled due to a security vulnerability found by ISE and a bug where large exports
        result in a 504. If we find a need for this in the future those issue will need to be
        addressed before re-enabling.

    """
    renderer_classes = [CSVRenderer, CamelCaseBrowsableAPIRenderer]

    def _search_for_assets(self, request):
        """Testing seam that returns the results of an asset search."""
        path = 'api/v3/assets'
        filter_boy = FilterBuddy()

        _filters = filter_boy.get_filters_from_request(request)
        for _filter in _filters:
            _filter.is_valid(query=True, raise_exception=True)
        query = filter_boy.reduce_filters_to_query(_filters, None)

        return self._yield_all_items_from_es(request, base_url=path, search_filter=query)

    def _yield_all_items_from_es(self, request, base_url=None, search_filter={}):
        """Helper to get all results from scroll responses from boonsdk.

        Given the search in `search_filter`, will return the results from boonsdk, making
        repeated paginated requests until all results are returned. Returned items will
        be run through an item_modifier to correctly update them.

        Args:
            request: The original DRF request

        Keyword Args:
            base_url (str): The base URL to use for the ZMLP Request. Defaults to None (which uses
                the views default.
            search_filter (dict): An optional filter to use on the ZMLP request.

        Yields:
            (dict): All assets for the matching query, as a generator
        """
        scroller = AssetSearchScroller(request.app, search_filter)

        for item in scroller:
            # Coerce the Asset into the form our item_modifiers expect
            _data = {'_id': item.id, '_source': item.document}
            search_asset_modifier(request, _data)
            yield _data

    def list(self, request, project_pk):
        def dot_reducer(k1, k2):
            """Reducer function used by the flatten method to combine nested dict keys with dots."""
            if k1 is None:
                return k2
            else:
                return k1 + "." + k2

        # Create a list of flat dictionaries that represent the metadata for each asset.
        flat_assets = []
        for asset in self._search_for_assets(request):
            flat_asset = flatten(asset['metadata'], reducer=dot_reducer)
            flat_asset['id'] = asset['id']
            flat_assets.append(flat_asset)

        # Return the CSV file to the client.
        return Response(flat_assets)
