from rest_framework import status
from rest_framework.response import Response
from zmlp.client import ZmlpInvalidRequestException

from apikeys.serializers import ApikeySerializer
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


class ApikeyViewSet(BaseProjectViewSet):
    base_url = '/auth/v1/apikey/'
    serializer_class = ApikeySerializer
    pagination_class = ZMLPFromSizePagination

    ZMLP_ONLY = True

    def list(self, request, project_pk):
        # Need to be able to paginate and filter by project key
        # currently the api is automatically filtering by the project the users
        # apikey is created against
        response = request.client.get('/auth/v1/apikey')
        serializer = self.get_serializer(data=response, many=True)
        serializer.is_valid()
        return Response({'results': serializer.data})

    def retrieve(self, request, project_pk, pk):
        return self._retrieve(request, pk)

    def create(self, request, project_pk):
        serializer = self.get_serializer(data=request.data)
        if not serializer.is_valid():
            return Response(status=status.HTTP_400_BAD_REQUEST, data=serializer.errors)
        body = {'name': serializer.validated_data['name'],
                'permissions': serializer.validated_data['permissions']}
        try:
            response = request.client.post('/auth/v1/apikey', body)
        except ZmlpInvalidRequestException:
            return Response("Bad Request", status=status.HTTP_400_BAD_REQUEST)
        return Response(status=status.HTTP_201_CREATED, data=response)

    def destroy(self, request, project, pk):
        return self._destroy(request, pk)
