from rest_framework.response import Response

from permissions.serializers import PermissionSerializer
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


class PermissionViewSet(BaseProjectViewSet):

    serializer_class = PermissionSerializer
    pagination_class = ZMLPFromSizePagination

    ZMLP_ONLY = True

    def list(self, request, project_pk, client):
        # Doesn't have a search endpoint to use for pagination
        response = client.get('/auth/v1/permissions')
        serializer = self.get_serializer(data=response, many=True)
        serializer.is_valid()
        return Response({'results': serializer.data})
