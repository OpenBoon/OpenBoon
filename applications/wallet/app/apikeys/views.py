from rest_framework import status
from rest_framework.response import Response

from apikeys.serializers import ApikeySerializer
from projects.views import BaseProjectViewSet
from wallet.paginators import FromSizePagination


class ApikeyViewSet(BaseProjectViewSet):

    serializer_class = ApikeySerializer
    pagination_class = FromSizePagination

    ZMLP_ONLY = True

    def list(self, request, project_pk, client):
        # Need to be able to paginate and filter by project key
        # currently the api is automatically filtering by the project the users
        # apikey is created against
        response = client.get('/auth/v1/apikey')
        serializer = self.get_serializer(data=response, many=True)
        serializer.is_valid()
        return Response({'results': serializer.data})

    def retrieve(self, request, project_pk, client, pk):
        response = client.get(f'/auth/v1/apikey/{pk}')
        return Response(response)

    def create(self, request, project_pk, client):
        serializer = self.get_serializer(data=request.data)
        if not serializer.is_valid():
            return Response(status=status.HTTP_400_BAD_REQUEST, data=serializer.errors)
        body = {'projectId': project_pk,
                'name': serializer.validated_data['name'],
                'permissions': serializer.validated_data['permissions']}
        response = client.post('/auth/v1/apikey', body)
        return Response(status=status.HTTP_201_CREATED, data=response)
