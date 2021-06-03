from datasets.serializers import DatasetSerializer
from projects.viewsets import (BaseProjectViewSet, ZmlpListMixin, ZmlpCreateMixin,
                               # ZmlpUpdateMixin, # TODO: Put back in once updating Datasets is supported
                               ZmlpDestroyMixin, ZmlpRetrieveMixin,
                               ListViewType)
from wallet.exceptions import InvalidRequestError


class DatasetsViewSet(ZmlpCreateMixin,
                      ZmlpListMixin,
                      ZmlpRetrieveMixin,
                      ZmlpDestroyMixin,
                      # ZmlpUpdateMixin,  # TODO: Put back in once updating Datasets is supported
                      BaseProjectViewSet):

    serializer_class = DatasetSerializer
    zmlp_root_api_path = '/api/v3/datasets'
    list_type = ListViewType.SEARCH
    list_modifier = None
    retrieve_modifier = None

    def create(self, request, project_pk):
        if request.data.get('projectId') is not None and request.data.get('projectId') != project_pk:
            msg = 'Invalid request. You can only create datasets for the current project context.'
            raise InvalidRequestError(detail={'detail': [msg]})
        else:
            request.data['projectId'] = project_pk

        return super(DatasetsViewSet, self).create(request, project_pk)

    # TODO: Put back in once updating Datasets is supported
    # def update(self, request, project_pk, pk):
    #     if request.data.get('projectId') is not None and request.data.get('projectId') != project_pk:
    #         msg = 'Invalid request. You can only update datasets for the current project context.'
    #         raise InvalidRequestError(detail={'detail': [msg]})
    #     else:
    #         request.data['projectId'] = project_pk
    #
    #     return super(DatasetsViewSet, self).update(request, project_pk, pk)
