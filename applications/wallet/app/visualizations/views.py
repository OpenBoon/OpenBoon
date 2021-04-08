from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from projects.viewsets import BaseProjectViewSet
from searches.utils import FieldUtility, FilterBuddy
from .utils import VizBuddy
from wallet.mixins import CamelCaseRendererMixin


class VisualizationViewSet(CamelCaseRendererMixin,
                           BaseProjectViewSet):
    pagination_class = None
    serializer_class = None
    field_utility = FieldUtility()

    def list(self, request, project_pk):
        """Available endpoints for the visualization api:

        *load* - accepts two query parameters. `query` and `visuals`. Used to return the data
            needed to populate a visualization in the UI. 4
        """
        # TODO: Could be nice to list out available visualizations & their formats.
        return Response(status=status.HTTP_418_IM_A_TEAPOT, data={})

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

        Accepted Visualizations:

            Range:
                {
                    "type": "range",
                    "id": "$uniqueIdentifier",
                    "attribute": "$attribute.dot.path"
                }

            Facet:
                {
                    "type": "facet",
                    "id": "$uniqueIdentifier",
                    "attribute": "$attribute.dot.path",
                    "options": {
                        "order": "desc",       # Sort order, desc or asc
                        "size": 10,            # Limit # of facets/buckets returned
                        "minimum_count": 2     # Min. # of hits a facet needs to be returned
                    }
                }

                ** A Facet visualization can be created for any field with a type of "keyword" or
                "prediction".

            Histogram:
                {
                    "type": "histogram",
                    "id": "$uniqueIdentifier",
                    "attribute": "$attribute.dot.path",
                    "options": {
                        "size": 10              # Default size is 10 buckets
                    }
                }

                ** A Histogram visualization can be created for any field with a type of "integer",
                "double", "float", "long", or "prediction".

        Args:
            request: The DRF request
            project_pk: The Project ID to run this under
        """
        path = 'api/v3/assets/_search'

        # Determine the query to use based on the given Filters
        filter_buddy = FilterBuddy()
        _filters = filter_buddy.get_filters_from_request(request)
        for _filter in _filters:
            _filter.is_valid(query=True, raise_exception=True)
        query = filter_buddy.reduce_filters_to_query(_filters)

        # Determine the Visualizations we need to load data for
        viz_buddy = VizBuddy(filter_query=query)
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
