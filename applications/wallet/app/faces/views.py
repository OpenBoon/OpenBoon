from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from boonsdk import ModelType, DatasetType, Model
from boonsdk.client import BoonSdkNotFoundException

from assets.utils import AssetBoxImager
from assets.views import AssetViewSet
from faces.serializers import UpdateFaceLabelsSerializer, FaceAssetSerializer
from projects.viewsets import BaseProjectViewSet
from wallet.mixins import CamelCaseRendererMixin
from wallet.paginators import ZMLPFromSizePagination


def predictions_match(left, right):
    """Helper to compare two prediction blobs and tell if their bbox and simhash are equal."""
    try:
        if (left['bbox'] == right['bbox']):
            return True
    except KeyError:
        pass
    return False


class FaceViewSet(CamelCaseRendererMixin, BaseProjectViewSet):
    zmlp_root_api_path = 'api/v3/assets/'
    detection_attr = 'analysis.boonai-face-detection'
    model_name = 'console'
    serializer_class = UpdateFaceLabelsSerializer
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
                    'filter': [{'exists': {'field': self.detection_attr}}]
                }
            }
        }
        return self._zmlp_list_from_es(request, item_modifier=item_modifier,
                                       search_filter=filter,
                                       base_url=AssetViewSet.zmlp_root_api_path,
                                       serializer_class=FaceAssetSerializer)

    def retrieve(self, request, project_pk, pk):
        """Given an asset, returns the face predictions and applicable labels for that asset.

        Looks at the boonai-face-detection analysis and returns the bbox image, simhash, label,
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

        if not asset.get_attr(self.detection_attr):
            return Response(status=status.HTTP_200_OK, data=data)
        model = self._get_model(app)
        recognition_attr = f'analysis.{model.module_name}'

        # Get the bboxes for each prediction
        imager = AssetBoxImager(asset, client)
        width = int(request.query_params.get('width', 255))
        detection_predictions = imager.get_attr_with_box_images(self.detection_attr,
                                                                width=width)['predictions']

        # Get existing predictions from face-recognition
        recognition_predictions = asset.get_attr(recognition_attr, {}).get('predictions', [])

        # Filter existing labels to only those for this model
        labels = asset.document.get('labels', [])
        filtered_labels = []
        for label in labels:
            if label['modelId'] == model.id:
                filtered_labels.append(label)

        # Match existing filtered labels with the bbox predictions, and mark them as modified
        for detected_prediction in detection_predictions:
            detected_prediction['modified'] = False

            # Prefer labels if they exist
            for label in filtered_labels:
                if predictions_match(detected_prediction, label):
                    detected_prediction['label'] = label['label']
                    detected_prediction['modified'] = True
                    break
            if detected_prediction['modified'] is True:
                continue

            # Look for face-recognition predictions if there were no matching labels
            for recognition_prediction in recognition_predictions:
                if predictions_match(detected_prediction, recognition_prediction):
                    detected_prediction['label'] = recognition_prediction['label']
                    detected_prediction['modified'] = True
                    break

        data['predictions'] = detection_predictions
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
        serializer = UpdateFaceLabelsSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        app = request.app
        data = serializer.validated_data
        asset = app.assets.get_asset(pk)
        model = self._get_model(app)

        # Generate labels for every given update
        labels_to_apply = []
        for label_update in data['labels']:
            label = model.make_label(label_update['label'], bbox=label_update['bbox'],
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
        labels in the face recognition model. The second reprocesses all the assets
        with previously created model.

        Args:
            request: The DRF Request object.
            project_pk: The contextual project id for this endpoint.

        Returns:
            (Response): Returns a 200 response if the jobs are launched successfully.
        """
        app = request.app
        model = self._get_model(app)

        # Train the model
        # TODO: Add filtering query for what to reprocess once it's avilable.
        job = app.models.train_model(model, deploy=True)

        return Response(status=status.HTTP_200_OK, data=job._data)

    @action(detail=False, methods=['get'])
    def status(self, request, project_pk):
        """Returns any running reprocessing job and whether there are unapplied changes."""
        app = request.app

        # Check for jobs
        patterns = [f'Train {self.model_name} ',
                    f'Training model: {self.model_name} -']
        running_jobs = app.jobs.find_jobs(state='InProgress', sort=['timeCreated:d'])
        job_id = ''
        for job in running_jobs:
            if any([job.name.startswith(pattern) for pattern in patterns]):
                job_id = job.id

        # Check for unapplied changes - always True until we can use real logic
        # to check for this. True allows us to use the Train & Apply button in the UI.
        # Unapplied changes is the opposite of the model.ready property
        model = self._get_model(app)
        changes = not model.ready
        return Response(status=status.HTTP_200_OK, data={'unapplied_changes': changes,
                                                         'job_id': job_id})

    @action(detail=False, methods=['get'])
    def labels(self, request, project_pk):
        """Gives the list of labels for the face recognition  model and their usage count."""
        app = request.app
        model = self._get_model(app)
        dataset = app.datasets.get_dataset(model.dataset_id)
        label_counts = app.datasets.get_label_counts(dataset)
        possible_labels = []
        for label in label_counts:
            possible_labels.append({'label': label,
                                    'count': label_counts[label]})

        data = {'possible_labels': possible_labels}
        return Response(status=status.HTTP_200_OK, data=data)

    def _get_model(self, app):
        """Helper to get or create the model for Face Training."""
        try:
            model = app.models.find_one_model(name=self.model_name)
        except BoonSdkNotFoundException:
            dataset = app.datasets.create_dataset(self.model_name, DatasetType.FaceRecognition)
            body = {'name': self.model_name,
                    'type': ModelType.FACE_RECOGNITION,
                    'datasetId': dataset.id}
            model = Model(app.client.post("/api/v3/models", body))
        return model
