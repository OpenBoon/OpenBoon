from django.http import Http404
from rest_framework.mixins import (ListModelMixin, RetrieveModelMixin,
                                   CreateModelMixin, UpdateModelMixin, DestroyModelMixin)
from rest_framework.viewsets import GenericViewSet
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from projects.views import BaseProjectViewSet
from searches.models import Search
from searches.serializers import SearchSerializer
from wallet.mixins import ConvertCamelToSnakeViewSetMixin
from wallet.paginators import FromSizePagination
from .services import FieldService


class SearchViewSet(ConvertCamelToSnakeViewSetMixin,
                    CreateModelMixin,
                    UpdateModelMixin,
                    ListModelMixin,
                    RetrieveModelMixin,
                    DestroyModelMixin,
                    BaseProjectViewSet,
                    GenericViewSet):
    """Allows a User to save a Search/Filter query for later use.

    Searches are associated with a Project, and viewable by any user with a membership
    to that project.

    Available HTTP methods, endpoints, and what they do:

    * **GET** _api/v1/projects/$Project_Id/searches/_ - List the saved searches for $Project_Id
    * **GET** _api/v1/projects/$Project_Id/searches/$Search_Id/_ - Detail info on $Search_Id
    * **POST** _api/v1/projects/*Project_Id/searches/ - Create a new Saved Search
        - To create one, you only need to send a name for the search and the query fields to save:
            `{
                "name": "My Search",
                "search": {
                    "query": {
                        "prefix": {
                            "files.name": {
                                "value": "image"
                            }
                        }
                    }
                }
            }`
    * **PUT** _api/v1/projects/$Project_Id/searches/$Search_Id/_ - Send the full object
    with updated values.
    * **PATCH** _api/v1/projects/$Project_Id/searches/$Search_Id/_ - Send only the new
    values to update the object.
    * **DELETE** _api/v1/projects/$Project_Id/searches/$Search_Id/_ - Remove the Saved Search
    """

    zmlp_only = True
    pagination_class = FromSizePagination
    serializer_class = SearchSerializer
    field_service = FieldService()

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
        fields = self.field_service.get_fields_from_mappings(mappings)

        return Response(status=status.HTTP_200_OK, data=fields)
