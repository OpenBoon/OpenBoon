from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from zmlp import DataSetType, ModelType
from zmlp.client import ZmlpNotFoundException

from assets.utils import AssetBoxImager
from assets.views import AssetViewSet
from faces.serializers import UpdateLabelsSerializer, FaceAssetSerializer
from projects.views import BaseProjectViewSet
from wallet.mixins import ConvertCamelToSnakeViewSetMixin
from wallet.paginators import ZMLPFromSizePagination


class FaceViewSet(ConvertCamelToSnakeViewSetMixin, BaseProjectViewSet):
    zmlp_only = True
    zmlp_root_api_path = 'api/v3/assets/'
    analysis_attr = 'analysis.zvi-face-detection'
    dataset_name = 'console_face_recognition'
    serializer_class = UpdateLabelsSerializer
    pagination_class = ZMLPFromSizePagination

    def list(self, request, project_pk):
        """This view is only intended for use in the browsable API to give examples of the
        detail endpoint.
        """
        def item_modifier(request, asset):
            asset['url'] = request.build_absolute_uri(f'{request.path}{asset["_id"]}')

        filter = {
            'query': {
                'bool': {
                    'filter': [{'exists': {'field': 'analysis.zvi-face-detection'}}]
                }
            }
        }
        return self._zmlp_list_from_es(request, item_modifier=item_modifier,
                                       search_filter=filter,
                                       base_url=AssetViewSet.zmlp_root_api_path,
                                       serializer_class=FaceAssetSerializer)

    def retrieve(self, request, project_pk, pk):
        """Given an asset, returns the face predictions and applicable labels for that asset.

        Looks at the zvi-face-detection analysis and returns the bbox image, simhash, label,
        and bbox coordinates for each prediction that is made. Also looks at applied labels
        for this asset, and matches up labels with their associated prediction. If a label
        mathes a prediction, the predictions `label` will be overridden with the labels value,
        and the `modified` field on the prediction will be set to True.

        Args:
            request: The DRF Request.
            project_pk: The contextual project id for this endpoint.
            pk: The Asset ID to return predictions for.

        Returns:
            (Response): The Predictions for the given Asset ID. The structure should match
                the following:

        Example Return:
            ```
            {
                "predictions": [
                    {
                        "score": 1,
                        "bbox": [
                            0.313,
                            0.439,
                            0.394,
                            0.571
                        ],
                        "label": "Danny",
                        "simhash": "the sim hash",
                        "b64Image": "b64 image string",
                        "modified": true
                    }
                ]
            }
            ```

        """
        app = request.app
        client = request.client
        asset = app.assets.get_asset(pk)

        # Get the filename, setup return structure
        data = {'filename': asset.get_attr('source.filename'),
                'predictions': ''}

        if not asset.get_attr(self.analysis_attr):
            return Response(status=status.HTTP_200_OK, data=data)
        dataset = self._get_dataset(app)

        # Get the bboxes for each prediction
        imager = AssetBoxImager(asset, client)
        width = int(request.query_params.get('width', 255))
        predictions = imager.get_attr_with_box_images(self.analysis_attr,
                                                      width=width)['predictions']

        # Filter existing labels to only those for this dataset
        labels = asset.document.get('labels', [])
        filtered_labels = []
        for label in labels:
            if label['dataSetId'] == dataset.id:
                filtered_labels.append(label)

        # Match existing filtered labels with the bbox predictions, and mark them as modified
        for prediction in predictions:
            prediction['modified'] = False
            for label in filtered_labels:
                if (prediction['bbox'] == label['bbox']
                        and prediction['simhash'] == label['simhash']):
                    prediction['label'] = label['label']
                    prediction['modified'] = True

        data['predictions'] = predictions
        return Response(status=status.HTTP_200_OK, data=data)

    @action(detail=True, methods=['post'])
    def save(self, request, project_pk, pk):
        """Save the modified labels on an asset.

        Takes the labels in the request and saves them on the given asset.

        Expected Body:
            ```
            {
                "labels": [
                {"bbox": [0.313, 0.439, 0.394, 0.571],
                 "simhash": "The sim hash",
                 "label": "Danny"}
                ]
            }
            ```

        Args:
            request: The DRF Request object.
            project_pk: The contextual project id for this endpoint.
            pk: The Asset ID to return predictions for.

        Returns:
            (Response): Returns a 201 if a label is created or a 200 if no labels are given.
        """
        serializer = UpdateLabelsSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        app = request.app
        data = serializer.validated_data
        asset = app.assets.get_asset(pk)
        dataset = self._get_dataset(app)

        # Generate labels for every given update
        labels_to_apply = []
        for label_update in data['labels']:
            label = dataset.make_label(label_update['label'], bbox=label_update['bbox'],
                                       simhash=label_update['simhash'])
            labels_to_apply.append(label)

        if labels_to_apply:
            app.assets.update_labels(asset, add_labels=labels_to_apply)
        else:
            return Response(status=status.HTTP_200_OK, data={})

        return Response(status=status.HTTP_201_CREATED, data={})

    @action(detail=False, methods=['post'])
    def train(self, request, project_pk):
        """Creates a model and reprocesses all assets with face detection using it.

        This creates two processing job. The first trains the model off of all the
        labels in the face recognition dataset. The second reprocesses all the assets
        with previously created model.

        Args:
            request: The DRF Request object.
            project_pk: The contextual project id for this endpoint.

        Returns:
            (Response): Returns a 200 response if the jobs are launched successfully.
        """
        app = request.app
        dataset = self._get_dataset(app)
        model = self._get_model(app, dataset)

        # Train the model
        # TODO: Add filtering query for what to reprocess once it's avilable.
        job = app.models.train_model(model, deploy=True)

        return Response(status=status.HTTP_200_OK, data=job._data)

    @action(detail=False, methods=['get'])
    def status(self, request, project_pk):
        """Returns any running reprocessing job and whether there are unapplied changes."""
        # Check for jobs
        name_prefix = ('Train zvi-console_face_recognition-face-recognition')
        running_jobs = request.app.jobs.find_jobs(state='InProgress')
        job_id = ''
        for job in running_jobs:
            if job.name.startswith(name_prefix):
                job_id = job.id

        # Check for unapplied changes - always True until we can use real logic
        # to check for this. True allows us to use the Train & Apply button in the UI.
        changes = True
        return Response(status=status.HTTP_200_OK, data={'unapplied_changes': changes,
                                                         'job_id': job_id})

    # @action(detail=False, methods=['get'])
    # def training_job(self, request, project_pk):
    #     """Returns the ID of any running face reprocessing job."""
    #     name_prefix = ('Train zvi-console_face_recognition-face-recognition')
    #     running_jobs = request.app.jobs.find_jobs(state='InProgress')
    #     for job in running_jobs:
    #         if job.name.startswith(name_prefix):
    #             return Response(status=status.HTTP_200_OK, data={'job_id': job.id})
    #     return Response(status=status.HTTP_200_OK, data={'job_id': ''})
    #
    # @action(detail=False, methods=['get'])
    # def unapplied_changes(self, request, project_pk):
    #     """Returns whether the dataset has been updated but assets not reprocessed."""
    #     # TODO: Figure out how to return whether there are unapplied changes or not.
    #     data = {'unapplied_changes': True}
    #     return Response(status=status.HTTP_200_OK, data=data)

    @action(detail=False, methods=['get'])
    def labels(self, request, project_pk):
        """Gives the list of labels for the face recognition dataset and their usage count."""
        app = request.app
        dataset = self._get_dataset(app)
        label_counts = app.datasets.get_label_counts(dataset)
        possible_labels = []
        for label in label_counts:
            possible_labels.append({'label': label,
                                    'count': label_counts[label]})

        data = {'possible_labels': possible_labels}
        return Response(status=status.HTTP_200_OK, data=data)

    def _get_dataset(self, app):
        """Helper to get or create the Face Training Dataset."""
        try:
            dataset = app.datasets.find_one_dataset(name=self.dataset_name)
        except ZmlpNotFoundException:
            dataset = app.datasets.create_dataset(self.dataset_name, DataSetType.FACE_RECOGNITION)
        return dataset

    def _get_model(self, app, dataset):
        """Helper to get or create the model for Face Training on the given Dataset."""
        try:
            model = app.models.find_one_model(dataset=dataset)
        except ZmlpNotFoundException:
            model = app.models.create_model(dataset, ModelType.ZVI_FACE_RECOGNITION)
        return model
