from rest_framework import status
from rest_framework.response import Response

from datasources.serializers import DataSourceSerializer
from projects.views import BaseProjectViewSet


class DataSourceViewSet(BaseProjectViewSet):
    serializer_class = DataSourceSerializer

    def create(self, request, project_pk, client):
        serializer = self.get_serializer(data=request.data)
        if not serializer.is_valid():
            return Response(status=status.HTTP_400_BAD_REQUEST, data=serializer.errors)
        app = request.app
        data = serializer.validated_data
        datasource = app.datasource.create_datasource(data['name'], data['uri'],
                                                      modules=data['modules'],
                                                      file_types=data['file_types'])
        app.datasource.import_files(datasource)
        return Response(self.get_serializer(data=datasource._data).data)


