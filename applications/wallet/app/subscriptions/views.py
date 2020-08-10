from django.http import Http404
from rest_framework.mixins import ListModelMixin, RetrieveModelMixin
from rest_framework.viewsets import GenericViewSet

from projects.views import BaseProjectViewSet
from subscriptions.models import Subscription
from subscriptions.serializers import SubscriptionSerializer
from wallet.paginators import FromSizePagination


class SubscriptionViewSet(ListModelMixin,
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
