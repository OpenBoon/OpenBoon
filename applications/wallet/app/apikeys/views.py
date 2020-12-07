from django.utils.decorators import method_decorator
from django.views.decorators.cache import cache_control
from rest_framework import status
from rest_framework.response import Response
from zmlp.client import ZmlpDuplicateException

from apikeys.serializers import ApikeySerializer
from apikeys.utils import create_zmlp_api_key
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


@method_decorator(cache_control(max_age=0, no_store=True), name='dispatch')
class ApikeyViewSet(BaseProjectViewSet):
    serializer_class = ApikeySerializer
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/auth/v1/apikey/'
    zmlp_only = True

    def list(self, request, project_pk):
        query = {'sort': ['timeCreated:desc']}
        return self._zmlp_list_from_search(request, search_filter=query)

    def retrieve(self, request, project_pk, pk):
        return self._zmlp_retrieve(request, pk)

    def create(self, request, project_pk):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        try:
            apikey = create_zmlp_api_key(request.client, serializer.validated_data['name'],
                                         serializer.validated_data['permissions'], encode_b64=False,
                                         internal=serializer.validated_data.get('internal', False))
        except ZmlpDuplicateException:
            msg = 'An API Key with this name already exists. Please choose another.'
            return Response(status=status.HTTP_409_CONFLICT, data={'name': [msg]})
        slim_key = {'accessKey': apikey['accessKey'],
                    'secretKey': apikey['secretKey']}
        return Response(status=status.HTTP_201_CREATED, data=slim_key)

    def destroy(self, request, project_pk, pk):
        return self._zmlp_destroy(request, pk)
