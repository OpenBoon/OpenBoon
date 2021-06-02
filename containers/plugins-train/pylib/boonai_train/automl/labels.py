import logging
import tempfile

from google.cloud import automl

import boonsdk
from boonflow import file_storage
from boonflow.cloud import get_gcp_project_id

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

    def __init__(self, model, reactor=None):
        self.model = model
        self.reactor = reactor

        self.app = boonsdk.app_from_env()
        self.client = automl.AutoMlClient()
        self.project_location = f"projects/{get_gcp_project_id()}/locations/us-central1"
        # Can only be 32 chars
        self.display_name = self.model.id.replace("-", "")

    def train(self):
        """
        Train the AutoML model. This method builds a dataSet and kicks off
        a training job. The job is registered with the Archivist which
        handles monitoring the job and publishing the model.

        Returns:
            dict: A Boon AI AutoML session.
        """

        dataset = self._create_automl_dataset()

        self._import_images_into_dataset(dataset)

        return self._train_automl_model()

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
        self._export_model(automl_model)

        return automl_model

    def _export_model(self, automl_model):
        export_model_location = file_storage.projects\
            .get_directory_location('models', self.model.id)

        gcs_destination = automl.GcsDestination(output_uri_prefix=export_model_location)
        output_config = automl.ModelExportOutputConfig(
            gcs_destination=gcs_destination, model_format="tflite")
        request = automl.ExportModelRequest(name=automl_model.name, output_config=output_config)

        self.emit_status(f'Exporting Model {self.model.name} to {gcs_destination}')
        self.client.export_model(request=request).result()

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

                # get proxy uri
                proxy_uri = self._get_img_proxy_uri(asset)
                if proxy_uri:
                    fp.write(f"{test}{proxy_uri},{tag}\n")

        ref = file_storage.projects.store_file(
            csv_file, self.model, "automl", "labels.csv")

        return file_storage.projects.get_native_uri(ref)

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
