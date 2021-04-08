from permissions.serializers import PermissionSerializer
from projects.viewsets import BaseProjectViewSet, ZmlpListMixin, ListViewType
from wallet.paginators import ZMLPFromSizePagination


class PermissionViewSet(ZmlpListMixin,
                        BaseProjectViewSet):
    serializer_class = PermissionSerializer
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/auth/v1/permissions'
    list_type = ListViewType.ROOT
