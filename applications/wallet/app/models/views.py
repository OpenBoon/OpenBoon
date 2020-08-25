import os

from django.http import Http404
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response
from zmlp.client import ZmlpNotFoundException
from zmlp.entity.model import LabelScope

from models.serializers import (ModelSerializer, ModelTypeSerializer,
                                AddLabelsSerializer, UpdateLabelsSerializer,
                                RemoveLabelsSerializer, RenameLabelSerializer,
                                DestroyLabelSerializer)
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


def item_modifier(request, item):
    app = request.app
    running_jobs = app.jobs.find_jobs(state='InProgress', name=item['trainingJobName'],
                                      sort=['timeCreated:d'])
    running_jobs = list(running_jobs)
    running_job_id = running_jobs[0].id if running_jobs else ''
    item['runningJobId'] = running_job_id


class ModelViewSet(BaseProjectViewSet):
    serializer_class = ModelSerializer
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/api/v3/models'
    zmlp_only = True

    def list(self, request, project_pk):
        """List all of the Models for this project."""
        return self._zmlp_list_from_search(request, item_modifier=item_modifier)

    def retrieve(self, request, project_pk, pk):
        """Retrieve the details for this specific model."""
        return self._zmlp_retrieve(request, pk=pk, item_modifier=item_modifier)

    def destroy(self, request, project_pk, pk):
        """Deletes a model."""
        return self._zmlp_destroy(request, pk)

    def create(self, request, project_pk):
        """Create a model for this project.

        Body:
            {
                "name": "Model Name",
                "type": "Model Type"
            }

        Args:
            request: The DRF Request object.
            project_pk: The contextual project id for this endpoint.

        Returns:
            (Response): Returns a 201 if the model was created.
        """
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        response = request.client.post(self.zmlp_root_api_path, serializer.validated_data)
        return Response(status=status.HTTP_201_CREATED, data={'results': response})

    @action(methods=['get'], detail=False)
    def model_types(self, request, project_pk):
        """Get the available model types from ZMLP."""
        path = f'{self.zmlp_root_api_path}/_types'
        return self._zmlp_list_from_root(request, base_url=path,
                                         serializer_class=ModelTypeSerializer)

    @action(methods=['post'], detail=True)
    def train(self, request, project_pk, pk):
        """Trains a model and optionally deploys it.

        If the post body contains a True value for `deploy`, the model will be trained
        and deployed.

        Returns:
            (Response): Returns a 200 response if the training job is launched successfully.
        """
        app = request.app
        model = self._get_model(app, pk)
        deploy = request.data.get('deploy', False)

        job = app.models.train_model(model, deploy=deploy)
        return Response(status=status.HTTP_200_OK, data=job._data)

    @action(methods=['get'], detail=True)
    def get_labels(self, request, project_pk, pk):
        """Get the list of used labels and their counts for the given model."""
        path = f'{self.zmlp_root_api_path}/{pk}/_label_counts'
        response = request.client.get(path)
        labels = []
        for label in response:
            labels.append({'label': label, 'count': response[label]})
        data = {'count': len(labels),
                'results': labels}
        return Response(status=status.HTTP_200_OK, data=data)

    @action(methods=['post'], detail=True)
    def add_labels(self, request, project_pk, pk):
        """Save labels on an asset for the given model.

        Takes each update and applies the given label to the specified asset for the
        current model.

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
        model = self._get_model(app, pk)

        label_updates = self._get_assets_and_labels(app, model, labels)

        if label_updates:
            for asset, label in label_updates:
                app.assets.update_labels(asset, add_labels=label)
        else:
            msg = 'No valid labels sent for creation.'
            return Response(status=status.HTTP_400_BAD_REQUEST, data={'detail': msg})

        return Response(status=status.HTTP_201_CREATED, data={})

    @action(methods=['post'], detail=True)
    def update_labels(self, request, project_pk, pk):
        serializer = UpdateLabelsSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        app = request.app
        data = serializer.validated_data
        add_labels_raw = data['addLabels']
        remove_labels_raw = data['removeLabels']
        model = self._get_model(app, pk)

        add_labels = self._get_assets_and_labels(app, model, add_labels_raw)
        remove_labels = self._get_assets_and_labels(app, model, remove_labels_raw)

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
            return Response(status=status.HTTP_400_BAD_REQUEST, data={'detail': msg})

        return Response(status=status.HTTP_200_OK, data={})

    @action(methods=['delete'], detail=True)
    def delete_labels(self, request, project_pk, pk):
        serializer = RemoveLabelsSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        app = request.app
        data = serializer.validated_data
        labels = data['removeLabels']
        model = self._get_model(app, pk)

        label_updates = self._get_assets_and_labels(app, model, labels)

        if label_updates:
            for asset, label in label_updates:
                app.assets.update_labels(asset, remove_labels=label)
        else:
            msg = 'No valid labels sent for creation.'
            return Response(status=status.HTTP_400_BAD_REQUEST, data={'detail': msg})

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

    def _get_assets_and_labels(self, app, model, data):
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
            label = model.make_label(raw['label'], bbox=raw['bbox'],
                                     simhash=raw['simhash'], scope=scope)
            labels.append((asset, label))
        return labels

    def _get_model(self, app, model_id):
        """Gets the model for the given ID"""
        try:
            return app.models.get_model(model_id)
        except ZmlpNotFoundException:
            raise Http404()
