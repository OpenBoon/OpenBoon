from django.http import Http404
from django.urls import reverse
from djangorestframework_camel_case.render import CamelCaseBrowsableAPIRenderer
from flatten_dict import flatten
from rest_framework.mixins import (ListModelMixin, RetrieveModelMixin,
                                   CreateModelMixin, UpdateModelMixin, DestroyModelMixin)
from rest_framework.viewsets import GenericViewSet
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework_csv.renderers import CSVRenderer
from zmlp.search import AssetSearchScroller

from assets.views import asset_modifier
from projects.views import BaseProjectViewSet
from searches.models import Search
from searches.serializers import SearchSerializer, SearchAssetSerializer
from wallet.mixins import ConvertCamelToSnakeViewSetMixin
from wallet.paginators import FromSizePagination, ZMLPFromSizePagination
from .utils import FieldUtility, FilterBoy


def search_asset_modifier(request, item):
    asset_modifier(request, item)
    # Default relative url in case a thumbnail is not found
    thumbnail_url = '/icons/fallback_3x.png'
    project_id = request.parser_context['view'].kwargs['project_pk']
    asset_id = item['id']
    for _file in item['metadata']['files']:
        category = 'web-proxy'
        if _file['category'] == category:
            name = _file['name']
            # If a web-proxy is found, build the file serving url for it
            thumbnail_url = reverse('file_name-detail', kwargs={'project_pk': project_id,
                                                                'asset_pk': asset_id,
                                                                'category_pk': category,
                                                                'pk': name})
    # Regardless of the url being used, make it absolute
    item['thumbnail_url'] = request.build_absolute_uri(thumbnail_url)
    del(item['metadata']['files'])


class SearchViewSet(ConvertCamelToSnakeViewSetMixin,
                    CreateModelMixin,
                    UpdateModelMixin,
                    ListModelMixin,
                    RetrieveModelMixin,
                    DestroyModelMixin,
                    BaseProjectViewSet,
                    GenericViewSet):

    zmlp_only = True
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
        request.data['created_by'] = str(request.user.id)
        return super(SearchViewSet, self).create(request, *args, **kwargs)

    @action(detail=False, methods=['get'])
    def fields(self, request, project_pk):
        """Returns all available fields in the ES index and their type."""
        path = 'api/v3/fields/_mapping'
        content = request.client.get(path)
        indexes = list(content.keys())
        if len(indexes) != 1:
            return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR,
                            data={'detail': 'ZMLP did not return field mappings as expected.'})

        index = indexes[0]
        mappings = content[index]['mappings']
        fields = self.field_utility.get_fields_from_mappings(mappings)

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
                    "hashes": ["Simhashes to match"],
                    "minScore": 0.75,   # Optional
                    "boost": 1.0        # Optional
                }
            }

        """
        path = 'api/v3/assets'
        fields = ['id',
                  'source*',
                  'files*']
        filter_boy = FilterBoy()

        _filters = filter_boy.get_filters_from_request(request)
        for _filter in _filters:
            _filter.is_valid(query=True, raise_exception=True)
        query = filter_boy.reduce_filters_to_query(_filters)

        # If there's no specific query at this point, let's sort by the created date
        # to make the visual display in Visualizer more useful
        if not query:
            query['sort'] = {'system.timeCreated': {'order': 'desc'}}

        # Only returns the specified fields in the metadata
        query['_source'] = fields

        return self._zmlp_list_from_es(request, search_filter=query, base_url=path,
                                       serializer_class=SearchAssetSerializer,
                                       item_modifier=search_asset_modifier,
                                       pagination_class=ZMLPFromSizePagination)

    @action(detail=False, methods=['get'])
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

            "minimum_count": $integer

        LabelConfidence:

            {
                "type": "labelConfidence",
                "attribute": "analysis.zvi-label-detection",
            }

        """
        path = 'api/v3/assets/_search'

        filter_service = FilterBoy()
        _filter = filter_service.get_filter_from_request(request)
        _filter.is_valid(raise_exception=True)

        response = request.client.post(path, _filter.get_es_agg())

        return Response(status=status.HTTP_200_OK, data=_filter.serialize_agg_response(response))


class MetadataExportViewSet(BaseProjectViewSet):
    """Exports asset metadata as CSV file."""
    renderer_classes = [CSVRenderer, CamelCaseBrowsableAPIRenderer]

    def _search_for_assets(self, request):
        """Testing seam that returns the results of an asset search."""
        path = 'api/v3/assets'
        filter_boy = FilterBoy()

        _filters = filter_boy.get_filters_from_request(request)
        for _filter in _filters:
            _filter.is_valid(query=True, raise_exception=True)
        query = filter_boy.reduce_filters_to_query(_filters)

        return self._yield_all_items_from_es(request, base_url=path, search_filter=query)

    def _yield_all_items_from_es(self, request, base_url=None, search_filter={}):
        """Helper to get all results from paginated responses from ZMLP.

        Given the search in `search_filter`, will return the results from ZMLP, making
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
