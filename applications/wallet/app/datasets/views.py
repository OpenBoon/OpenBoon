import os

from boonsdk import LabelScope
from boonsdk.client import BoonSdkNotFoundException
from django.http import Http404
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from datasets.serializers import (DatasetSerializer, RemoveLabelsSerializer,
                                  AddLabelsSerializer,
                                  UpdateLabelsSerializer, DestroyLabelSerializer,
                                  RenameLabelSerializer, DatasetDetailSerializer,
                                  DatasetTypeSerializer)
from models.serializers import SimpleModelSerializer
from projects.viewsets import (BaseProjectViewSet, ZmlpListMixin, ZmlpCreateMixin,
                               ZmlpDestroyMixin, ZmlpRetrieveMixin,
                               ListViewType)
from wallet.exceptions import InvalidRequestError
from wallet.utils import validate_zmlp_data


class DatasetsViewSet(ZmlpCreateMixin,
                      ZmlpListMixin,
                      ZmlpRetrieveMixin,
                      ZmlpDestroyMixin,
                      # ZmlpUpdateMixin,  # TODO: Put back in once updating Datasets is supported
                      BaseProjectViewSet):

    zmlp_root_api_path = '/api/v3/datasets'
    list_type = ListViewType.SEARCH
    list_modifier = None
    retrieve_modifier = None

    def get_serializer_class(self):
        if self.action in ['retrieve', 'create']:
            return DatasetDetailSerializer
        return DatasetSerializer

    def create(self, request, project_pk):
        if request.data.get('projectId') is not None and request.data.get('projectId') != project_pk:
            msg = 'Invalid request. You can only create datasets for the current project context.'
            raise InvalidRequestError(detail={'detail': [msg]})
        return super(DatasetsViewSet, self).create(request, project_pk)

    # TODO: Put back in once updating Datasets is supported
    # def update(self, request, project_pk, pk):
    #     if request.data.get('projectId') is not None and request.data.get('projectId') != project_pk:
    #         msg = 'Invalid request. You can only update datasets for the current project context.'
    #         raise InvalidRequestError(detail={'detail': [msg]})
    #
    #     return super(DatasetsViewSet, self).update(request, project_pk, pk)

    @action(methods=['get'], detail=True)
    def get_labels(self, request, project_pk, pk):
        """Get the list of used labels and their counts for the given dataset."""
        path = f'{self.zmlp_root_api_path}/{pk}/_label_counts'
        response = request.client.get(path)
        labels = []
        for label in response:
            labels.append({'label': label, 'count': response[label]})
        data = {'count': len(labels),
                'results': labels}
        return Response(status=status.HTTP_200_OK, data=data)

    @action(methods=['get'], detail=True)
    def get_models(self, request, project_pk, pk):
        """Get the list of used labels and their counts for the given dataset."""
        return self._zmlp_list_from_search_all_pages(request, base_url='/api/v3/models',
                                                     search_filter={'dataSetIds': [pk]},
                                                     serializer_class=SimpleModelSerializer)

    @action(methods=['post'], detail=True)
    def add_labels(self, request, project_pk, pk):
        """Save labels on an asset for the given dataset.

        Takes each update and applies the given label to the specified asset for the
        current dataset.

        Expected Body:

            {
                "add_labels": [
                    {"assetId": $assetId,
                     "label": "Label Name",
                     "bbox": [0.313, 0.439, 0.394, 0.571],  # Optional
                     "simhash": "The sim hash",  # Optional
                     "scope": "TRAIN",  # or TEST, optional, default is TRAIN
                    },
                    ...
                ]
            }

        """
        serializer = AddLabelsSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        app = request.app
        data = serializer.validated_data
        labels = data['addLabels']
        dataset = self._get_dataset(app, pk)

        label_updates = self._get_assets_and_labels(app, dataset, labels)

        if label_updates:
            for asset, label in label_updates:
                app.assets.update_labels(asset, add_labels=label)
        else:
            msg = 'No valid labels sent for creation.'
            return Response(status=status.HTTP_400_BAD_REQUEST, data={'detail': [msg]})

        return Response(status=status.HTTP_201_CREATED, data={})

    @action(methods=['post'], detail=True)
    def update_labels(self, request, project_pk, pk):
        serializer = UpdateLabelsSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        app = request.app
        data = serializer.validated_data
        add_labels_raw = data['addLabels']
        remove_labels_raw = data['removeLabels']
        dataset = self._get_dataset(app, pk)

        add_labels = self._get_assets_and_labels(app, dataset, add_labels_raw)
        remove_labels = self._get_assets_and_labels(app, dataset, remove_labels_raw)

        by_asset = {}
        for (asset, label) in add_labels:
            by_asset.setdefault(asset, {}).setdefault('add', []).append(label)
        for (asset, label) in remove_labels:
            by_asset.setdefault(asset, {}).setdefault('remove', []).append(label)

        if add_labels and remove_labels:
            for asset in by_asset:
                app.assets.update_labels(asset, add_labels=by_asset[asset]['add'],
                                         remove_labels=by_asset[asset]['remove'])
        else:
            msg = 'No valid label updates sent.'
            return Response(status=status.HTTP_400_BAD_REQUEST, data={'detail': [msg]})

        return Response(status=status.HTTP_200_OK, data={})

    @action(methods=['delete'], detail=True)
    def delete_labels(self, request, project_pk, pk):
        serializer = RemoveLabelsSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        app = request.app
        data = serializer.validated_data
        labels = data['removeLabels']
        dataset = self._get_dataset(app, pk)

        label_updates = self._get_assets_and_labels(app, dataset, labels)

        if label_updates:
            for asset, label in label_updates:
                app.assets.update_labels(asset, remove_labels=label)
        else:
            msg = 'No valid labels sent for creation.'
            return Response(status=status.HTTP_400_BAD_REQUEST, data={'detail': [msg]})

        return Response(status=status.HTTP_200_OK, data={})

    @action(methods=['delete'], detail=True)
    def destroy_label(self, request, project_pk, pk):
        """Completely destroys all instances of a single label.

        Expected Body:

            {
                "label": "Dog",
            }

        """
        path = os.path.join(self.zmlp_root_api_path, pk, 'labels')
        serializer = DestroyLabelSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        return Response(request.client.delete(path, serializer.validated_data))

    @action(methods=['put'], detail=True)
    def rename_label(self, request, project_pk, pk):
        """Allows renaming an existing label. Requires the original label name and a new
        label name.

        Expected Body:

            {
                "label": "Dog",
                "newLabel": "Cat"
            }

        """
        path = os.path.join(self.zmlp_root_api_path, pk, 'labels')
        serializer = RenameLabelSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        return Response(request.client.put(path, serializer.validated_data))

    @action(methods=['get'], detail=False)
    def dataset_types(self, request, project_pk):
        """Get the available dataset types from boonsdk."""
        path = f'{self.zmlp_root_api_path}/_types'
        dataset_types = request.client.get(path)
        serializer = DatasetTypeSerializer(data=dataset_types, many=True,
                                           context=self.get_serializer_context())
        validate_zmlp_data(serializer)
        return Response({'results': serializer.data})

    def _get_dataset(self, app, dataset_id):
        """Gets the dataset for the given ID"""
        try:
            return app.datasets.get_dataset(dataset_id)
        except BoonSdkNotFoundException:
            raise Http404()

    def _get_assets_and_labels(self, app, dataset, data):
        """Get a list of Label objects from request data."""
        labels = []
        for raw in data:
            asset = app.assets.get_asset(raw['assetId'])
            if raw['scope'] == 'TRAIN':
                scope = LabelScope.TRAIN
            elif raw['scope'] == 'TEST':
                scope = LabelScope.TEST
            else:
                scope = None
            label = dataset.make_label(raw['label'], bbox=raw['bbox'],
                                       simhash=raw['simhash'], scope=scope)
            labels.append((asset, label))
        return labels
