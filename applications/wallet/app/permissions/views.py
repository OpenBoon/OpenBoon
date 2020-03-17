from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from permissions.serializers import PermissionSerializer
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


class PermissionViewSet(BaseProjectViewSet):

    serializer_class = PermissionSerializer
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/auth/v1/permissions'
    zmlp_only = True

    def list(self, request, project_pk):
        return self._zmlp_list_from_root(request)

    @action(detail=False, methods=['get'])
    def user(self, request, project_pk):
        return Response(status=status.HTTP_405_METHOD_NOT_ALLOWED)