from django.http import Http404
from rest_framework.response import Response

from projects.views import BaseProjectViewSet
from wallet.paginators import FromSizePagination
from subscriptions.models import Subscription
from subscriptions.serializers import SubscriptionSerializer


class SubscriptionViewSet(BaseProjectViewSet):

    zmlp_only = True
    pagination_class = FromSizePagination
    serializer_class = SubscriptionSerializer

    def get_object(self, project_pk, pk):
        try:
            return Subscription.objects.get(id=pk, project=project_pk)
        except Subscription.DoesNotExist:
            raise Http404

    def get_queryset(self, project_pk):
        return Subscription.objects.filter(project=project_pk)

    def list(self, request, project_pk):
        queryset = self.get_queryset(project_pk)

        page = self.paginate_queryset(queryset)
        if page is not None:
            serializer = self.get_serializer(page, many=True)
            return self.get_paginated_response(serializer.data)

        serializer = self.get_serializer(queryset, many=True)
        return Response(serializer.data)

    def retrieve(self, request, project_pk, pk):
        instance = self.get_object(project_pk, pk)
        serializer = self.get_serializer(instance)
        return Response(serializer.data)
