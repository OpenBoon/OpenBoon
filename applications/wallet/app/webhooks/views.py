from rest_framework.decorators import action
from rest_framework.mixins import ListModelMixin
from rest_framework.response import Response
from rest_framework.reverse import reverse
from rest_framework.viewsets import ViewSet, GenericViewSet

from projects.viewsets import (ZmlpDestroyMixin, ZmlpRetrieveMixin, ZmlpListMixin,
                               BaseProjectViewSet, ZmlpCreateMixin, ListViewType,
                               ZmlpUpdateMixin)
from wallet.exceptions import InvalidZmlpDataError
from wallet.utils import get_zmlp_superuser_client
from webhooks.models import Trigger
from webhooks.serializers import WebhookSerializer, TriggerSerializer


class ProjectWebhooksViewSet(ZmlpCreateMixin,
                             ZmlpListMixin,
                             ZmlpUpdateMixin,
                             ZmlpRetrieveMixin,
                             ZmlpDestroyMixin,
                             BaseProjectViewSet):
    serializer_class = WebhookSerializer
    zmlp_root_api_path = '/api/v3/webhooks/'
    list_type = ListViewType.SEARCH
    list_query = {'sort': ['timeCreated:desc']}


class WebhooksViewSet(ViewSet):
    def list(self, request):
        return Response({
            'triggers': reverse('webhook-util-trigger-list', request=request),
            'test': reverse('webhook-util-test', request=request)
        })

    @action(detail=False, methods=['post'])
    def test(self, request):
        serializer = WebhookSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        response = get_zmlp_superuser_client().post('/api/v3/webhooks/_test',
                                                    serializer.validated_data)
        if not response.get('success'):
            raise InvalidZmlpDataError({'detail': ['Failed to send test webhook payload.']})
        return Response({'detail': ['Successfully sent test webhook payload.']})


class TriggersViewSet(ListModelMixin,
                      GenericViewSet):
    queryset = Trigger.objects.all()
    serializer_class = TriggerSerializer
