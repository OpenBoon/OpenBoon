from rest_framework import status
from rest_framework.response import Response
from zmlp.client import ZmlpInvalidRequestException

from apikeys.serializers import ApikeySerializer
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


class ApikeyViewSet(BaseProjectViewSet):
    serializer_class = ApikeySerializer
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/auth/v1/apikey/'
    zmlp_only = True

    def list(self, request, project_pk):
        return self._zmlp_list_from_root(request)

    def retrieve(self, request, project_pk, pk):
        return self._zmlp_retrieve(request, pk)

    def create(self, request, project_pk):
        serializer = self.get_serializer(data=request.data)
        if not serializer.is_valid():
            return Response(status=status.HTTP_400_BAD_REQUEST, data=serializer.errors)
        body = {'name': serializer.validated_data['name'],
                'permissions': serializer.validated_data['permissions']}
        try:
            response = request.client.post(self.zmlp_root_api_path, body)
        except ZmlpInvalidRequestException:
            return Response(data={'detail': 'Bad Request'},
                            status=status.HTTP_400_BAD_REQUEST)
        return Response(status=status.HTTP_201_CREATED, data=response)

    def destroy(self, request, project, pk):
        return self._zmlp_destroy(request, pk)
