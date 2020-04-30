from django.http import Http404
from rest_framework.mixins import ListModelMixin, RetrieveModelMixin
from rest_framework.viewsets import GenericViewSet
from rest_framework.response import Response

from projects.views import BaseProjectViewSet
from subscriptions.models import Subscription
from subscriptions.serializers import SubscriptionSerializer
from wallet.mixins import ConvertCamelToSnakeViewSetMixin
from wallet.paginators import FromSizePagination


class SubscriptionViewSet(ConvertCamelToSnakeViewSetMixin,
                          ListModelMixin,
                          RetrieveModelMixin,
                          BaseProjectViewSet,
                          GenericViewSet):

    zmlp_only = True
    pagination_class = FromSizePagination
    serializer_class = SubscriptionSerializer

    def get_object(self):
        try:
            return Subscription.objects.get(id=self.kwargs['pk'],
                                            project=self.kwargs['project_pk'])
        except Subscription.DoesNotExist:
            raise Http404

    def get_queryset(self):
        return Subscription.objects.filter(project=self.kwargs['project_pk'])

    def list(self, request, *args, **kwargs):
        queryset = self.filter_queryset(self.get_queryset())

        # self._update_usage(request, queryset)

        page = self.paginate_queryset(queryset)
        if page is not None:
            serializer = self.get_serializer(page, many=True)
            return self.get_paginated_response(serializer.data)

        serializer = self.get_serializer(queryset, many=True)
        return Response(serializer.data)

    # def _update_usage(self, request, queryset):
    #     for plan in queryset:
