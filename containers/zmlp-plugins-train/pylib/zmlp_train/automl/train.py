import os
import time
import tempfile
import json
import logging
import pandas as pd

import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import automl_v1beta1 as automl
from google.cloud import storage as gcs

from zmlpsdk import Argument, AssetProcessor, file_storage
from zmlp.training import TrainingSetDownloader as tsd

logging.basicConfig()


class AutoMLModelTrainer(AssetProcessor):
    """Create Google AutoML Model"""

    tool_tips = {
        'display_name': 'Name of the dataset',
        'project_path': 'Path to data CSV'
    }

    def __init__(self):
        super(AutoMLModelTrainer, self).__init__()
        self.add_arg(Argument("model_id", "string", required=True,
                              toolTip="The model Id"))
        self.add_arg(Argument("display_name", "string", required=True,
                              toolTip=AutoMLModelTrainer.tool_tips['display_name']))
        self.add_arg(Argument("project_path", "string",
                              toolTip=AutoMLModelTrainer.tool_tips['project_path']))
        self.add_arg(Argument("model_path", "string",
                              toolTip=AutoMLModelTrainer.tool_tips['project_path']))
        self.add_arg(Argument("deploy", "bool", default=False,
                              toolTip="Automatically deploy the model onto assets."))

        self.app_model = None
        self.model_path = None
        self.client = None

        self.project_id = None
        self.project_path = None
        self.display_name = None

        self.df = None

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))
        self.project_id = self._get_project_id()
        self.display_name = self.arg_value('display_name')
        self.project_path = self.arg_value('project_path')
        self.model_path = self.arg_value('model_path')

        self.client = automl.AutoMlClient()
        self.df = pd.read_csv(self.project_path, header=None)

    def process(self, frame):
        # create empty dataset from project ID
        dataset = self.create_dataset(self.project_id, self.display_name)
        dataset_id = self._get_id(dataset)

        # build and return CSV file
        self.project_path = self._build_dataset()

        # import dataset from project_path CSV file
        self.import_dataset(self.project_id, dataset_id, self.project_path)

        # create/train model
        model = self.create_model(self.project_id, dataset_id, self.display_name)
        model_id = self.model_path or self._get_id(model.operation)

        # publish model
        self.publish_model(model_id)

    @staticmethod
    def _get_project_id():
        """Get Project ID for a GC Project

        Returns:
            (str) Project ID (e.g. 'zorroa-poc-dev')
        """
        # If this is running in a cloud function, then GCP_PROJECT should be defined
        if 'GCP_PROJECT' in os.environ:
            project_id = os.environ['GCP_PROJECT']
        # else if this is running locally then GOOGLE_APPLICATION_CREDENTIALS should be defined
        elif 'GOOGLE_APPLICATION_CREDENTIALS' in os.environ:
            with open(os.environ['GOOGLE_APPLICATION_CREDENTIALS'], 'r') as fp:
                credentials = json.load(fp)
            project_id = credentials['project_id']
        else:
            raise Exception('Failed to determine project_id')

        return project_id

    @staticmethod
    def _get_id(name):
        """Parse a response name for its ID

        Args:
            name (Object): AutoML class (e.g. Dataset or Model class)

        Returns:
            (str) the parsed name's ID (or its location basename)
        """
        return name.name.split("/")[-1]

    def _build_dataset(self):
        """
        Write a DataSet in a AutoML training structure.

        Returns:
            str: A path to an annotation file.
        """
        storage_client = gcs.Client()

        # build CSV file
        d = tempfile.mkdtemp()
        dsl = tsd(self.app, '12345', 'objects_automl', d)
        local_csv_path = dsl.build()

        # writing the data into the file
        gcs_csv_path = 'csv/data.csv'
        self._upload_to_gcs_bucket(storage_client, self.project_id, gcs_csv_path, local_csv_path)

    @staticmethod
    def _upload_to_gcs_bucket(storage_client, bucket_name, blob_path, local_path):
        """
        Write local file to GCS bucket

        Args:
            storage_client: GCS Client
            bucket_name: bucket name (e.g. project_id)
            blob_path: path where file will be uploaded (excluding 'gs://bucket_name/')
            local_path: path to local file

        Returns:
            None
        """
        bucket = storage_client.bucket(bucket_name)
        blob = bucket.blob(blob_path)

        blob.upload_from_filename(local_path)
        logging.debug("File {} uploaded to {}.".format(local_path, blob_path))

    def create_dataset(self, project_id, display_name, region="us-central1"):
        """Create an empty dataset that will eventually hold the training data for the model.
        Specify the type of classification you want your custom model to perform:

        MULTICLASS assigns a single label to each classified image
        MULTILABEL allows an image to be assigned multiple labels
        https://cloud.google.com/automl/docs/reference/rpc/google.cloud.automl.v1#classificationtype

        Args:
            project_id (str): GC Project ID
            display_name (str): Project name
            region (str): GC Region ID (default: us-central1)

        Returns:
            (google.cloud.automl_v1beta1.types.Dataset) Dataset metadata
        """
        # A resource that represents Google Cloud Platform location.
        project_location = self.client.location_path(project_id, region)

        # Specify the classification type
        metadata = automl.types.ImageClassificationDatasetMetadata(
            classification_type=automl.enums.ClassificationType.MULTICLASS
        )
        dataset = automl.types.Dataset(
            display_name=display_name,
            image_classification_dataset_metadata=metadata,
        )

        # Create a dataset with the dataset metadata in the region.
        created_dataset = self.client.create_dataset(project_location, dataset)

        # Display the dataset information
        logging.debug("Dataset name: {}".format(created_dataset.name))
        logging.debug("Dataset id: {}".format(created_dataset.name.split("/")[-1]))

        return created_dataset

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def import_dataset(self, project_id, dataset_id, path, region="us-central1"):
        """Takes as input a .csv file that lists the locations of all training images and the
        proper label for each one

        Args:
            project_id (str): GC Project ID
            dataset_id (str): Dataset ID
            path (str): Path to CSV
            region (str): GC Region ID (default: us-central1)

        Returns:
            None
        """
        start_time = time.time()
        # Get the full path of the dataset.
        dataset_full_id = self.client.dataset_path(
            project_id, region, dataset_id
        )
        # Get the multiple Google Cloud Storage URIs
        input_uris = path.split(",")
        gcs_source = automl.types.GcsSource(input_uris=input_uris)
        input_config = automl.types.InputConfig(gcs_source=gcs_source)
        # Import data from the input URI
        response = self.client.import_data(dataset_full_id, input_config)

        self.reactor.emit_status("Processing import...")
        self.reactor.emit_status("Data imported. {}".format(response.result()))
        logging.debug("Total import time: {}".format(time.time()-start_time))

    def create_model(self, project_id, dataset_id, display_name, region="us-central1"):
        """Create and train a model

        Leave model unset to use the default base model provided by Google
        train_budget_milli_node_hours: The actual train_cost will be equal or
        less than this value.
        https://cloud.google.com/automl/docs/reference/rpc/google.cloud.automl.v1#imageclassificationmodelmetadata

        Args:
            project_id (str): GC Project ID
            dataset_id (str): Dataset ID
            display_name (str): Project name
            region (str): GC Region ID (default: us-central1)

        Returns:
            (google.cloud.automl_v1.types.Model¶) Model metadata
        """
        start_time = time.time()

        # A resource that represents Google Cloud Platform location.
        project_location = self.client.location_path(project_id, region)
        metadata = automl.types.ImageClassificationModelMetadata(train_budget=1)
        model = automl.types.Model(
            display_name=display_name,
            dataset_id=dataset_id,
            image_classification_model_metadata=metadata,
        )

        # Create a model with the model metadata in the region.
        response = self.client.create_model(project_location, model)

        self.reactor.emit_status("Training operation name: {}".format(response.operation.name))
        self.reactor.emit_status("Training started...")

        self.reactor.emit_status("Training complete. {}".format(response.result()))
        logging.debug("Total training time: {}".format(time.time() - start_time))

        return response

    def publish_model(self, model, region="us-central1"):
        """Publish the model.

        Args:
            model (str): Full Model ID
            region (str):  GC Region ID (default: us-central1)

        Returns:
            PipelineMod: The published Pipeline Module.
        """
        self.reactor.emit_status("Saving model: {}".format(self.app_model.name))
        model_dir = tempfile.mkdtemp() + '/' + self.app_model.name
        os.makedirs(model_dir)

        # add labels.txt from DataFrame
        labels = self.df[1].unique()
        with open(os.path.join(model_dir, "_labels.txt"), "w") as fp:
            for label in labels:
                fp.write("{}\n".format(label))
        self.reactor.emit_status("Labels are in " + model_dir + "_labels.txt")

        # deploy model
        model_full_id = self.client.model_path(self.project_id, region, model)
        self.client.deploy_model(model_full_id)

        # publish
        pmod = file_storage.models.save_model(model_dir, self.app_model, self.arg_value('deploy'))
        self.reactor.emit_status("Published model {}".format(self.app_model.name))
        return pmod
