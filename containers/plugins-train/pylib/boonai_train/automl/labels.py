import logging
import boonsdk
import uuid
import shutil
import os
import tempfile

from google.cloud import automl
from urllib.parse import urlparse
from boonflow import file_storage
from boonflow.cloud import get_gcp_project_id, get_google_storage_client

logger = logging.getLogger(__name__)

__all__ = [
    'AutomlLabelDetectionSession'
]


class AutomlLabelDetectionSession:
    """
    The AutoMLDataSetImporter handles importing a set of labeled Assets into AutoML.
    This class currently only handle multi-class (single labels) model training, not
    multi-label or objects.
    """

    def __init__(self, model, reactor=None, training_bucket=None):
        self.model = model
        self.reactor = reactor
        self.training_bucket = training_bucket

        self.app = boonsdk.app_from_env()
        self.client = automl.AutoMlClient()
        self.project_location = f"projects/{get_gcp_project_id()}/locations/us-central1"
        # Can only be 32 chars
        self.display_name = self.model.id.replace("-", "")

        self.automl_dataset = None
        self.automl_model = None

    def train(self):
        """
        Train the AutoML model. This method builds a dataSet and kicks off
        a training job. The job is registered with the Archivist which
        handles monitoring the job and publishing the model.

        Returns:
            dict: A Boon AI AutoML session.
        """

        self.automl_dataset = self._create_automl_dataset()

        self._import_images_into_dataset(self.automl_dataset)

        self.automl_model = self._train_automl_model(self.automl_dataset)

        temp_model_url = self._export_model(self.automl_model)

        self._delete_train_resources()

        self._upload_resources(temp_model_url)

    def _upload_resources(self, model_url):

        self.emit_status('Download and zipping exported trained files')

        parsed_uri = urlparse(model_url)

        blobs = self.storage_client \
            .list_blobs(bucket_or_name=parsed_uri.netloc,
                        prefix=f"{self.model.id}/model/model-export/icn/tflite-{self.display_name}")

        model_blob = None
        label_blob = None
        for blob in blobs:
            if blob.name.endswith('model.tflite'):
                model_blob = blob
            if blob.name.endswith('dict.txt'):
                label_blob = blob

        self.model_file = file_storage.localize_file(model_blob.name)
        self.label_file = file_storage.localize_file(label_blob.name)

        # copy model and label files to tmp directory and zip it
        tmp = tempfile.mkdtemp()
        shutil.copy(self.model_file, os.path.join(tmp, "model.tflite"))
        shutil.copy(self.label_file, os.path.join(tmp, "labels.txt"))

        self.app.models.upload_trained_model(self.model, tmp, None)

    def _move_asset_to_temp_bucket(self, asset):

        # get proxy uri
        asset_uri = self._get_img_proxy_uri(asset)
        storage_client = get_google_storage_client()

        parse_source = urlparse(asset_uri)
        bucket_source = storage_client.get_bucket(parse_source.netloc)
        blob_source = bucket_source.blob(parse_source.path[1:])

        parse_destination = urlparse(self.training_bucket)
        bucket_destination = storage_client.get_bucket(parse_destination.netloc)

        blob_to = bucket_source.copy_blob(blob_source,
                                          bucket_destination,
                                          f'{self.model.id}/assets/{asset.id}/{uuid.uuid4().hex}')

        return f'gs://{bucket_destination.name}/{blob_to.name}'

    def _train_automl_model(self, dataset):
        """
        Start an AutoML training job.

        Args:
            dataset (DataSet): The AutoML dataset.

        Returns:
            str: The training job name.
        """

        self.model_name = '{}_model'.format(self.display_name)

        # Leave model unset to use the default base model provided by Google
        # train_budget_milli_node_hours: The actual train_cost will be equal or
        # less than this value.
        # https://cloud.google.com/automl/docs/reference/rpc/google.cloud.automl.v1#imageclassificationmodelmetadata
        metadata = automl.types.ImageClassificationModelMetadata(
            train_budget_milli_node_hours=24000,
            model_type="mobile-low-latency-1"
        )
        automl_model_request = automl.types.Model(
            display_name=self.model_name,
            dataset_id=dataset.name,
            image_classification_model_metadata=metadata,
        )

        self.emit_status(f'Training AutoML exportable Model {self.model_name}')

        # Create a model with the model metadata in the region.
        automl_model = self.client.create_model(
            parent=self.project_location,
            model=automl_model_request).result()

        return automl_model

    def _export_model(self, automl_model):

        export_model_location = f'{self.training_bucket}/{self.model.id}/model/'

        gcs_destination = automl.GcsDestination(output_uri_prefix=export_model_location)
        output_config = automl.ModelExportOutputConfig(
            gcs_destination=gcs_destination, model_format="tflite")
        request = automl.ExportModelRequest(name=automl_model.name, output_config=output_config)

        self.emit_status(f'Exporting Model {self.model.name} to {gcs_destination}')
        self.client.export_model(request=request).result()

        return export_model_location

    def _create_automl_dataset(self):
        """
        Create a new Google AutoML dataset. The DataSet is empty at this point.

        Returns:
            DataSet: A google AutoML dataset instance.
        """
        self.emit_status(f'Creating AutoML DataSet {self.display_name}')

        metadata = automl.ImageClassificationDatasetMetadata(
            classification_type=automl.ClassificationType.MULTICLASS
        )
        spec = automl.Dataset(
            display_name=self.display_name,
            image_classification_dataset_metadata=metadata,
        )
        rsp = self.client.create_dataset(parent=self.project_location, dataset=spec)
        return rsp.result()

    def _import_images_into_dataset(self, dataset):
        """
        Import images to the AutoML dataset.

        Args:
            dataset (DataSet): The automl dataset.

        """
        self.emit_status(f'Importing labeled images into {self.display_name}')

        labels_url = self._store_labels_file()

        gcs_source = automl.GcsSource(input_uris=[labels_url])
        input_config = automl.InputConfig(gcs_source=gcs_source)
        result = self.client.import_data(dataset.name, input_config).result()

        logger.info("Processing import...")
        logger.info("Data imported. {}".format(result))

    def _store_labels_file(self):
        """
        Write a DataSet in a AutoML training structure.

        Returns:
            str: A path to data file.
        """
        self.emit_status(f'Building labels file for {self.display_name}')

        _, csv_file = tempfile.mkstemp(".csv")
        query = self.model.get_label_search()

        with open(csv_file, "w") as fp:
            for asset in self.app.assets.scroll_search(query, timeout='5m'):
                label = self._get_label(asset)
                if not label:
                    continue

                tag = label.get('label')
                scope = label.get('scope')

                test = ""
                if scope == "TEST":
                    test = "TEST"

                # move asset to temp bucket
                tmp_uri = self._move_asset_to_temp_bucket(asset)

                if tmp_uri:
                    fp.write(f"{test}{tmp_uri},{tag}\n")

        ref = file_storage.projects.store_file(
            csv_file, self.model, "automl", "labels.csv")

        return file_storage.projects.get_native_uri(ref)

    def _delete_train_resources(self):
        self.emit_status('Deleting model and dataset used for training')
        self.client.delete_model(automl.DeleteModelRequest(name=self.model.name)).result()

        ds_del_request = automl.DeleteDatasetRequest(name=self.automl_dataset.name)
        self.client.delete_dataset(ds_del_request).result()

    def _get_img_proxy_uri(self, asset):
        """
        Get a URI to the img proxy

        Args:
            asset: (Asset): The asset to find an audio proxy for.

        Returns:
            str: A URI to the smallest image proxy if not empty else empty string
        """
        img_proxies = asset.get_files(
            mimetype="image/",
            category='proxy',
            sort_func=lambda f: f.attrs.get('width', 0)
        )

        if img_proxies:
            img_proxy = img_proxies[0]  # get the smallest proxy
            return file_storage.assets.get_native_uri(img_proxy)
        return None

    def _get_label(self, asset):
        """
        Get the current model label for the given asset.

        Args:
            asset (Asset): The asset to check.

        Returns:
            list[dict]: The labels for training a model.

        """
        ds_labels = asset.get_attr('labels')
        if not ds_labels:
            return None

        for ds_label in ds_labels:
            if ds_label.get('modelId') == self.model.id:
                return ds_label
        return None

    def emit_status(self, msg):
        """
        Emit a status back to the Archivist.

        Args:
            msg (str): The message to emit.

        """
        if not self.reactor:
            return
        self.reactor.emit_status(msg)
