import os

from django.http import Http404, HttpResponse
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from models.serializers import (ModelSerializer, ModelTypeSerializer,
                                AddLabelsSerializer, UpdateLabelsSerializer,
                                RemoveLabelsSerializer, RenameLabelSerializer,
                                DestroyLabelSerializer, ModelDetailSerializer,
                                ConfusionMatrixSerializer)
from models.utils import ConfusionMatrix
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination
from wallet.utils import validate_zmlp_data
from zmlp.client import ZmlpNotFoundException
from zmlp.entity.model import LabelScope


def get_model_type_restrictions(label_counts, min_concepts, min_examples):
    if label_counts:
        # Calculate number of missing labels
        difference = min_concepts - len(label_counts)
        if difference <= 0:
            missing_label_count = 0
        else:
            missing_label_count = difference

        # Calculate number of missing labels on assets
        # Account for completely missing labels
        missing_label_count_asset_total = missing_label_count * min_examples
        # Account for labels that don't have the minimum amount of examples
        label_sum = 0
        for label in label_counts:
            additional_labels_required = min_examples - label_counts[label]
            if additional_labels_required > 0:
                label_sum += additional_labels_required

        missing_labels_on_assets = missing_label_count_asset_total + label_sum

    else:
        missing_label_count = min_concepts
        missing_labels_on_assets = min_concepts * min_examples

    return {'requiredLabels': min_concepts,
            'missingLabels': missing_label_count,
            'requiredAssetsPerLabel': min_examples,
            'missingLabelsOnAssets': missing_labels_on_assets}


def item_modifier(request, item):
    # Convert ready to unapplied changes
    ready = item['ready']
    del(item['ready'])
    item['unappliedChanges'] = not ready


def detail_item_modifier(request, item):
    item_modifier(request, item)
    app = request.app

    # Get the running job info
    running_jobs = app.jobs.find_jobs(state='InProgress', name=item['trainingJobName'],
                                      sort=['timeCreated:d'])
    running_jobs = list(running_jobs)
    running_job_id = running_jobs[0].id if running_jobs else ''
    item['runningJobId'] = running_job_id

    # Get the model type restrictions
    model_type = item['type']
    model_id = item['id']
    model_type_info = app.models.get_model_type_info(model_type)
    label_counts = app.models.get_label_counts(model_id)
    min_examples = model_type_info.min_examples
    min_concepts = model_type_info.min_concepts

    item['modelTypeRestrictions'] = get_model_type_restrictions(label_counts,
                                                                min_concepts,
                                                                min_examples)


class ModelViewSet(BaseProjectViewSet):
    serializer_class = ModelSerializer
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/api/v3/models'
    zmlp_only = True

    def get_serializer_class(self):
        if self.action == 'retrieve':
            return ModelDetailSerializer
        return self.serializer_class

    def list(self, request, project_pk):
        """List all of the Models for this project."""
        return self._zmlp_list_from_search(request, item_modifier=item_modifier)

    def retrieve(self, request, project_pk, pk):
        """Retrieve the details for this specific model."""
        return self._zmlp_retrieve(request, pk=pk, item_modifier=detail_item_modifier)

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
    def all(self, request, project_pk):
        """Get all the models available by consuming all the paginated responses."""
        return self._zmlp_list_from_search_all_pages(request, item_modifier=item_modifier)

    @action(methods=['get'], detail=False)
    def model_types(self, request, project_pk):
        """Get the available model types from ZMLP."""
        path = f'{self.zmlp_root_api_path}/_types'
        excluded_names = ['ZVI_FACE_RECOGNITION']
        response = request.client.get(path)
        filtered = [x for x in response if x['name'] not in excluded_names]
        serializer = ModelTypeSerializer(data=filtered, many=True,
                                         context=self.get_serializer_context())
        validate_zmlp_data(serializer)
        return Response({'results': serializer.data})

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
            return Response(status=status.HTTP_400_BAD_REQUEST, data={'detail': [msg]})

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

    @action(methods=['get'], detail=True)
    def confusion_matrix(self, request, project_pk, pk):
        """Returns data required to construct a confusion matrix for the model.

        Available Query Params:

        - *minScore* - Minimum confidence score to filter by (float).
        - *maxScore* - Maximum confidence score to filter by (float).
        - *testSetOnly* - Boolean, if true then only assets in the test set are evaluated.
        - *normalize* - Boolean, if true then the values are normalized between 0-1.

        """
        model = request.app.models.get_model(pk)
        matrix = ConfusionMatrix(model, request.app,
                                 min_score=request.query_params.get('minScore', 0.0),
                                 max_score=request.query_params.get('maxScore', 1.0),
                                 test_set_only=request.query_params.get('testSetOnly', True))
        response_data = matrix.to_dict(normalize_matrix=request.query_params.get('normalize', False))
        serializer = ConfusionMatrixSerializer(data=response_data)
        serializer.is_valid(raise_exception=True)
        return Response(serializer.validated_data)

    @action(methods=['get'], detail=True)
    def confusion_matrix_thumbnail(self, request, project_pk, pk):
        """Returns a thumbnail image of the confusion matrix for this model."""
        model = request.app.models.get_model(pk)
        matrix = ConfusionMatrix(model, request.app,
                                 min_score=request.data.get('minScore', 0.0),
                                 max_score=request.data.get('maxScore', 1.0),
                                 test_set_only=request.data.get('testSetOnly', True))
        thumbnail = matrix.create_thumbnail_image()
        return HttpResponse(thumbnail.read(), content_type='image/png')

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
