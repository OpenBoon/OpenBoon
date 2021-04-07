from boonsdk.client import BoonSdkDuplicateException
from django.utils.decorators import method_decorator
from django.views.decorators.cache import cache_control
from rest_framework import status
from rest_framework.response import Response

from apikeys.serializers import ApikeySerializer
from apikeys.utils import create_zmlp_api_key
from projects.viewsets import (ZmlpListMixin, ZmlpDestroyMixin, ZmlpRetrieveMixin,
                               BaseProjectViewSet, ListViewType)
from wallet.exceptions import DuplicateError
from wallet.paginators import ZMLPFromSizePagination


@method_decorator(cache_control(max_age=0, no_store=True), name='dispatch')
class ApikeyViewSet(ZmlpListMixin,
                    ZmlpRetrieveMixin,
                    ZmlpDestroyMixin,
                    BaseProjectViewSet):
    serializer_class = ApikeySerializer
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/auth/v1/apikey/'
    list_type = ListViewType.SEARCH
    list_filter = {'sort': ['timeCreated:desc']}

    def create(self, request, project_pk):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        try:
            apikey = create_zmlp_api_key(request.client, serializer.validated_data['name'],
                                         serializer.validated_data['permissions'], encode_b64=False,
                                         internal=serializer.validated_data.get('internal', False))
        except BoonSdkDuplicateException:
            msg = 'An API Key with this name already exists.'
            raise DuplicateError({'name': [msg]})
        slim_key = {'accessKey': apikey['accessKey'],
                    'secretKey': apikey['secretKey']}
        return Response(status=status.HTTP_201_CREATED, data=slim_key)
