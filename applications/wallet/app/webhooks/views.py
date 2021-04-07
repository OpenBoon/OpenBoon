from projects.viewsets import (ZmlpDestroyMixin, ZmlpRetrieveMixin, ZmlpListMixin,
                               BaseProjectViewSet, ZmlpCreateMixin, ListViewType)
from webhooks.serializers import WebhookSerializer


class WebhooksViewSet(ZmlpCreateMixin,
                      ZmlpListMixin,
                      ZmlpRetrieveMixin,
                      ZmlpDestroyMixin,
                      BaseProjectViewSet):
    serializer_class = WebhookSerializer
    zmlp_root_api_path = '/api/v1/webhooks'
    list_type = ListViewType.SEARCH
    list_query = {'sort': ['timeCreated:desc']}
