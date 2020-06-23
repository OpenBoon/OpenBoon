from django.http import Http404
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
from assets.utils import get_asset_style, get_video_length, get_thumbnail_and_video_urls
from projects.views import BaseProjectViewSet
from searches.models import Search
from searches.serializers import SearchSerializer, SearchAssetSerializer
from wallet.mixins import ConvertCamelToSnakeViewSetMixin
from wallet.paginators import FromSizePagination, ZMLPFromSizePagination
from searches.utils import FieldUtility, FilterBuddy
from .utils import VizBuddy


class VisualizationViewSet(BaseProjectViewSet):

    zmlp_only = True
    pagination_class = None
    serializer_class = None
    field_utility = FieldUtility()

    @action(detail=False, methods=['get'])
    def load(self, request, project_pk):
        """Takes a querystring with an ES query and a set of visuals to load.

        The querystring should be in the form:

            ?query=$B64EncodedQuery&visuals=$B64EncodedVisualizations

        `query` is the elasticsearch query the visualizations should be loaded from. The encoded
            JSON body should match the format:

            [$filter1, $filter2, $filter3, ...]

        `visuals` are the visualizations that the data should be loaded for. The encoded
            JSON body should match the format:

            [$visualization1, $visualization2, $visualization3, ...]

        Both of these query params should be B64 Encoded json object strings.

        Args:
            request: The DRF request
            project_pk: The Project ID to run this under
        """
        path = 'api/v3/assets'

        # Determine the query to use based on the given Filters
        filter_buddy = FilterBuddy()
        _filters = filter_buddy.get_filters_from_request(request)
        for _filter in _filters:
            _filter.is_valid(query=True, raise_exception=True)
        query = filter_buddy.reduce_filters_to_query(_filters)

        # Determine the Visualizations we need to load data for
        viz_buddy = VizBuddy(query=query)
        visualizations = viz_buddy.get_visualizations_from_request(request)
        for visualization in visualizations:
            visualization.is_valid(raise_exception=True)
        data_query = viz_buddy.reduce_visualizations_to_query(visualizations)

        # Query ZMLP for the data we need
        data = request.client.post(path, data_query)

        # Associate the returned data with the appropriate Visualization
        response_data = []
        for visualization in visualizations:
            response_data.append(visualization.serialize_response_data(data))

        return Response(status=status.HTTP_200_OK, data=response_data)
