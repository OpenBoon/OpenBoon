from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination
from models.serializers import ModelSerializer


class ModelViewSet(BaseProjectViewSet):
    serializer_class = ModelSerializer
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = ''
    zmlp_only = True

    def list(self, request, project_pk):
        return Response(status=status.HTTP_200_OK, data={})

    def retrieve(self, request, project_pk, pk):
        return Response(status=status.HTTP_200_OK, data={})

    def create(self, request, project_pk):
        return Response(status=status.HTTP_201_CREATED, data={})

    def destroy(self, request, project_pk, pk):
        return Response(status=status.HTTP_200_OK, data={})

    @action(methods=['get'], detail=False)
    def model_types(self, request, project_pk):
        return Response(status=status.HTTP_200_OK, data={})

    @action(methods=['post'], detail=True)
    def train(self, request, project_pk, pk):
        return Response(status=status.HTTP_200_OK, data={})


class LabelViewSet(BaseProjectViewSet):
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = ''
    serializer_class = LabelSerializer

    def list(self, request, project_pk, model_pk):
        return Response(status=status.HTTP_200_OK, data={})

    def create(self, request, project_pk, model_pk):
        return Response(status=status.HTTP_200_OK, data={})

    def destroy(self, request, project_pk, model_pk, pk):
