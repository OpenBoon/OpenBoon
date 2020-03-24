from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination
from modules.serializers import ModuleSerializer


class ModuleViewSet(BaseProjectViewSet):

    zmlp_only = True
    zmlp_root_api_path = '/api/v1/pipeline-mods/'
    pagination_class = ZMLPFromSizePagination
    serializer_class = ModuleSerializer

    def list(self, request, project_pk):
        return self._zmlp_list_from_search(request)
