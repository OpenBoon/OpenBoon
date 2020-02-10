from rest_framework import status
from rest_framework.response import Response

from datasources.serializers import DataSourceSerializer
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


class DataSourceViewSet(BaseProjectViewSet):
    """CRUD operations for ZMLP Data Sources."""
    serializer_class = DataSourceSerializer
    pagination_class = ZMLPFromSizePagination
    ZMLP_ONLY = True
    BASE_URL = '/api/v1/data-sources/'

    def create(self, request, project_pk):
        serializer = self.get_serializer(data=request.data)
        if not serializer.is_valid():
            return Response(status=status.HTTP_400_BAD_REQUEST, data=serializer.errors)
        app = request.app
        data = serializer.validated_data
        datasource = app.datasource.create_datasource(name=data['name'], uri=data['uri'],
                                                      modules=data['modules'],
                                                      file_types=data['file_types'])
        app.datasource.import_files(datasource)
        return Response(self.get_serializer(datasource).data)

    def list(self, request, project_pk):
        return self._list_from_zmlp_search_endpoint(request)

    def retrieve(self, request, project_pk, pk):
        return self._retrieve(request, pk)
