import json

from boonsdk.client import BoonSdkNotFoundException
from boonsdk.entity import PostTrainAction
from django.http import Http404, HttpResponse
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from models.serializers import (ModelSerializer, ModelTypeSerializer,
                                ModelDetailSerializer,
                                ConfusionMatrixSerializer, ModelUpdateSerializer)
from models.utils import ConfusionMatrix
from projects.viewsets import (BaseProjectViewSet, ZmlpListMixin, ZmlpRetrieveMixin,
                               ListViewType, ZmlpDestroyMixin, ZmlpCreateMixin,
                               ZmlpUpdateMixin, ZmlpPartialUpdateMixin)
from wallet.utils import validate_zmlp_data


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
    dataset_id = item.get('datasetId')
    model_type_info = app.models.get_model_type_info(model_type)
    label_counts = app.datasets.get_label_counts(dataset_id) if dataset_id else []
    min_examples = model_type_info.min_examples
    min_concepts = model_type_info.min_concepts

    item['modelTypeRestrictions'] = get_model_type_restrictions(label_counts,
                                                                min_concepts,
                                                                min_examples)


class ModelViewSet(ZmlpCreateMixin,
                   ZmlpListMixin,
                   ZmlpUpdateMixin,
                   ZmlpPartialUpdateMixin,
                   ZmlpRetrieveMixin,
                   ZmlpDestroyMixin,
                   BaseProjectViewSet):
    serializer_class = ModelSerializer
    zmlp_root_api_path = '/api/v3/models'
    list_type = ListViewType.SEARCH
    list_modifier = staticmethod(item_modifier)
    retrieve_modifier = staticmethod(detail_item_modifier)

    def get_serializer_class(self):
        action_map = {'retrieve': ModelDetailSerializer,
                      'update': ModelUpdateSerializer}
        return action_map.get(self.action, self.serializer_class)

    @action(methods=['get'], detail=False)
    def all(self, request, project_pk):
        """Get all the models available by consuming all the paginated responses."""
        return self._zmlp_list_from_search_all_pages(request, item_modifier=item_modifier)

    @action(methods=['get'], detail=False)
    def model_types(self, request, project_pk):
        """Get the available model types from boonsdk."""
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
        apply = request.data.get('apply', False)
        test = request.data.get('test', False)
        if test and apply:
            return Response(status=status.HTTP_400_BAD_REQUEST,
                            data={'detail': ['Cannot specify both test and apply, '
                                             'please pick one.']})
        post_train_action = PostTrainAction.NONE
        if apply:
            post_train_action = PostTrainAction.APPLY
        if test:
            post_train_action = PostTrainAction.TEST

        job = app.models.train_model(model, post_action=post_train_action)
        return Response(status=status.HTTP_200_OK, data=job._data)

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
        test_set_only = json.loads(request.query_params.get('testSetOnly', 'true'))
        matrix = ConfusionMatrix(model, request.app,
                                 min_score=request.query_params.get('minScore', 0.0),
                                 max_score=request.query_params.get('maxScore', 1.0),
                                 test_set_only=test_set_only)
        try:
            response_data = matrix.to_dict()
        except ValueError:
            response_data = {
                "name": model.name,
                "moduleName": model.module_name,
                "overallAccuracy": 0.0,
                "labels": [],
                'minScore': 0.0,
                'maxScore': 1.0,
                'testSetOnly': True,
                "matrix": [],
                "isMatrixApplicable": True,
                "datasetId": None}

        # Set the ready/unapplied changes status
        response_data['unappliedChanges'] = not model.ready

        serializer = ConfusionMatrixSerializer(data=response_data)
        serializer.is_valid(raise_exception=True)
        return Response(serializer.validated_data)

    @action(methods=['get'], detail=True)
    def confusion_matrix_thumbnail(self, request, project_pk, pk):
        """Returns a thumbnail image of the confusion matrix for this model."""
        model = request.app.models.get_model(pk)
        test_set_only = json.loads(request.query_params.get('testSetOnly', 'true'))
        matrix = ConfusionMatrix(model, request.app,
                                 min_score=request.query_params.get('minScore', 0.0),
                                 max_score=request.query_params.get('maxScore', 1.0),
                                 test_set_only=test_set_only)
        thumbnail = matrix.create_thumbnail_image()
        return HttpResponse(thumbnail.read(), content_type='image/png')

    def _get_model(self, app, model_id):
        """Gets the model for the given ID"""
        try:
            return app.models.get_model(model_id)
        except BoonSdkNotFoundException:
            raise Http404()
