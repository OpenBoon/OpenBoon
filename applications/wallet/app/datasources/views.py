from functools import lru_cache

from rest_framework import status
from rest_framework.response import Response

from datasources.serializers import DataSourceSerializer
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


class DataSourceViewSet(BaseProjectViewSet):
    """CRUD operations for ZMLP Data Sources."""
    serializer_class = DataSourceSerializer
    pagination_class = ZMLPFromSizePagination
    zmlp_only = True
    zmlp_root_api_path = '/api/v1/data-sources/'

    def create(self, request, project_pk):
        serializer = self.get_serializer(data=request.data)
        if not serializer.is_valid():
            return Response(status=status.HTTP_400_BAD_REQUEST, data=serializer.errors)
        app = request.app
        data = serializer.validated_data
        creds = data.get('credentials') or None
        datasource = app.datasource.create_datasource(name=data['name'], uri=data['uri'],
                                                      modules=data['modules'],
                                                      credentials=creds,
                                                      file_types=data['fileTypes'])
        app.datasource.import_files(datasource)
        serializer = self.get_serializer(data=datasource._data)
        if not serializer.is_valid():
            return Response({'detail': serializer.errors}, status=500)
        return Response(serializer.validated_data)

    def list(self, request, project_pk):
        def item_modifier(request, datasource):
            modules = datasource.get('modules')
            if modules:
                datasource['modules'] = [self._get_module_name(m, request.client) for m in modules]

        return self._zmlp_list_from_search(request, item_modifier=item_modifier)

    def retrieve(self, request, project_pk, pk):
        return self._zmlp_retrieve(request, pk)

    def destroy(self, request, project_pk, pk):
        return self._zmlp_destroy(request, pk)

    @lru_cache(maxsize=128)
    def _get_module_name(self, module_id, client):
        """Gets a pipeline module name based on its ID."""
        response = client.get(f'/api/v1/pipeline-mods/{module_id}')
        return response['name']
