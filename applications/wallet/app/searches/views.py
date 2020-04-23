from django.http import Http404
from rest_framework.mixins import (ListModelMixin, RetrieveModelMixin,
                                   CreateModelMixin, UpdateModelMixin, DestroyModelMixin)
from rest_framework.viewsets import GenericViewSet
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from assets.serializers import AssetSerializer
from assets.views import asset_modifier
from projects.views import BaseProjectViewSet
from searches.models import Search
from searches.serializers import SearchSerializer
from wallet.mixins import ConvertCamelToSnakeViewSetMixin
from wallet.paginators import FromSizePagination, ZMLPFromSizePagination
from .utils import FieldUtility, FilterBoy


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

        # Only returns the specified fields in the metadata
        query['_source'] = fields

        return self._zmlp_list_from_es(request, search_filter=query, base_url=path,
                                       serializer_class=AssetSerializer,
                                       item_modifier=asset_modifier,
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
        """
        path = 'api/v3/assets/_search'

        filter_service = FilterBoy()
        _filter = filter_service.get_filter_from_request(request)
        _filter.is_valid(raise_exception=True)

        response = request.client.post(path, _filter.get_es_agg())

        return Response(status=status.HTTP_200_OK, data=_filter.serialize_agg_response(response))
