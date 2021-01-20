from rest_framework.response import Response

from modules.models import Provider
from modules.serializers import ModuleSerializer, ProviderSerializer
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination
from wallet.utils import validate_zmlp_data


class ModuleViewSet(BaseProjectViewSet):

    zmlp_only = True
    zmlp_root_api_path = '/api/v1/pipeline-mods/'
    pagination_class = ZMLPFromSizePagination
    serializer_class = ModuleSerializer

    def list(self, request, project_pk):
        return self._zmlp_list_from_search(request)


class ProviderViewSet(BaseProjectViewSet):

    zmlp_only = True
    zmlp_root_api_path = '/api/v1/pipeline-mods/'
    pagination_class = ZMLPFromSizePagination
    serializer_class = ModuleSerializer

    def list(self, request, project_pk):
        provider_map = {}
        results = []
        for module in self._zmlp_list_from_search_all_pages(request).data['results']:
            provider = provider_map.setdefault(module['provider'].lower(), {})
            category = provider.setdefault(module['category'], [])
            category.append(module)

        # For now we find all providers. In the future we will likely need to support
        # project-specific providers. When that happens we'll need to update this query.
        for provider in Provider.objects.all():
            name = provider.name.lower()
            if name in provider_map:
                categories = []
                for category_name, modules in provider_map[name].items():
                    categories.append({'name': category_name, 'modules': modules})
                results.append({'name': name,
                                'logo': provider.logo_data_uri,
                                'description': provider.description,
                                'categories': categories})
        serializer = ProviderSerializer(data=results, many=True)
        validate_zmlp_data(serializer)
        return Response({'results': serializer.validated_data})
