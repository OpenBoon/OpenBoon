from permissions.serializers import PermissionSerializer
from projects.viewsets import BaseProjectViewSet, ZmlpListMixin, ListViewType


class PermissionViewSet(ZmlpListMixin,
                        BaseProjectViewSet):
    serializer_class = PermissionSerializer
    zmlp_root_api_path = '/auth/v1/permissions'
    list_type = ListViewType.ROOT
