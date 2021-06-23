import os
from copy import copy

from django.http import Http404
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from assets.utils import AssetBoxImager, resize_image, get_b64
from boonsdk import LabelScope
from boonsdk.client import BoonSdkNotFoundException
from datasets.serializers import (DatasetSerializer, RemoveLabelsSerializer,
                                  AddLabelsSerializer,
                                  UpdateLabelsSerializer, DestroyLabelSerializer,
                                  RenameLabelSerializer, DatasetDetailSerializer,
                                  DatasetTypeSerializer)
from models.serializers import SimpleModelSerializer
from projects.viewsets import (BaseProjectViewSet, ZmlpListMixin, ZmlpCreateMixin,
                               ZmlpDestroyMixin, ZmlpRetrieveMixin, ListViewType,
                               ZmlpUpdateMixin)
from wallet.exceptions import InvalidRequestError
from wallet.utils import validate_zmlp_data


class DatasetsViewSet(ZmlpCreateMixin,
                      ZmlpListMixin,
                      ZmlpRetrieveMixin,
                      ZmlpDestroyMixin,
                      ZmlpUpdateMixin,
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

    def update(self, request, project_pk, pk):
        if request.data.get('projectId') is not None and request.data.get('projectId') != project_pk:
            msg = 'Invalid request. You can only update datasets for the current project context.'
            raise InvalidRequestError(detail={'detail': [msg]})

        return super(DatasetsViewSet, self).update(request, project_pk, pk)

    @action(methods=['get'], detail=True)
    def get_labels(self, request, project_pk, pk):
        """Get the list of used labels and their counts for the given dataset."""
        path = f'/api/v4/datasets/{pk}/_label_counts/'
        response = request.client.get(path)
        labels = []
        for label, counts in response.items():
            labels.append({'label': label,
                           'trainCount': counts.get('TRAIN', 0),
                           'testCount': counts.get('TEST', 0)})
        data = {'count': len(labels),
                'results': labels}
        return Response(status=status.HTTP_200_OK, data=data)

    @action(methods=['get'], detail=True)
    def get_models(self, request, project_pk, pk):
        """Get the list of used labels and their counts for the given dataset."""
        return self._zmlp_list_from_search_all_pages(request, base_url='/api/v3/models',
                                                     search_filter={'datasetIds': [pk]},
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

    @action(methods=['get'], detail=True)
    def label_tool_info(self, request, project_pk, pk):
        """Returns all the information needed to drive the labeling tool UI in the visualizer.
        The labeling tool needs to display existing labels in some cases placeholder labels
        to the user. This action looks at the dataset type and the existing labels and returns
        all the labels and/or placeholders the label tool should display.

        """
        def label_filter(_label):
            return _label['datasetId'] == pk

        def classification_modifier(labels):
            """If the dataset type is classification we want to add an empty placeholder
            label if there is not already label for this asset. In addition all labels
            need to have a b64 thumbnail of the whole asset added.

            """
            # If there is not a classification label add a placeholder label.
            response_labels = copy(filtered_labels)
            if not response_labels:
                response_labels.append({"scope": "TRAIN",
                                        "datasetId": pk,
                                        "label": ""})

            # Add a base64 image of the entire asset for all labels.
            image = resize_image(box_imager.image, thumbnail_width)
            base64_image = get_b64(image)
            for _label in response_labels:
                _label['b64Image'] = base64_image
            return response_labels

        def face_recognition_modifier(labels):
            """If the dataset type is face recognition then we want to add a placeholder
            for any faces that have been detected where we don't already have a label
            with the same bbox.

            """
            response_labels = labels
            faces = asset.get_attr('analysis.boonai-face-detection.predictions')
            label_dict = {}

            # Adds placeholder labels for any detected faces.
            for face in faces:
                face['label'] = ''
                face['datasetId'] = pk
                _label = {'scope': 'TRAIN',
                          'datasetId': pk,
                          'label': '',
                          'bbox': face['bbox'],
                          'simhash': face['simhash']}
                label_dict[repr(_label['bbox'])] = _label

            # Add all existing labels and overwrite any of the placeholder labels that
            # have matching bboxes.
            for _label in response_labels:
                label_dict[repr(_label['bbox'])] = _label

            return label_dict.values()

        def do_nothing(labels):
            """Used anytime no modifications are needed on the labels."""
            return labels

        label_modifier_funcs = {'Classification': classification_modifier,
                                'FaceRecognition': face_recognition_modifier}

        dataset = request.client.get(os.path.join(self.zmlp_root_api_path, pk))
        dataset_type = dataset['type']
        asset = request.app.assets.get_asset(request.query_params['assetId'])
        labels = asset.get_attr('labels', [])
        box_imager = AssetBoxImager(asset, request.client)
        thumbnail_width = 56
        filtered_labels = list(filter(label_filter, labels))
        response_labels = label_modifier_funcs.get(dataset_type, do_nothing)(filtered_labels)

        # Add b64 images to any of the labels that have bboxes.
        for label in response_labels:
            box_imager._add_box_images(label, thumbnail_width)

        return Response({'count': len(response_labels), 'results': response_labels})

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
