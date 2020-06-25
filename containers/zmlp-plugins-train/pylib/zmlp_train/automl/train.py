import os
import time
import tempfile
import logging

import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import automl_v1beta1 as automl

from zmlpsdk import Argument, AssetProcessor, file_storage

logging.basicConfig()


class AutoMLModelTrainer(AssetProcessor):
    """Create Google AutoML Model"""

    tool_tips = {
        'project_id': 'The project ID for the AutoML model (e.g. "zorroa-autoedl")',
        'region': 'The region ID for the AutoML model (e.g. "us-central1")',
        'gcp_credentials_path': 'JSON credentials path for GCP',
        'model_id': '(Optional) The model ID for the AutoML model (e.g. "ICN1653624923981482691") '
                    'If this parameter is omitted, the most recently created model will be used.',
        'display_name': 'Name of the dataset',
        'project_path': 'Path to data CSV'
    }

    def __init__(self):
        super(AutoMLModelTrainer, self).__init__()
        self.add_arg(Argument("project_id", "string", required=True,
                              toolTip=AutoMLModelTrainer.tool_tips['project_id']))
        self.add_arg(Argument("gcp_credentials_path", "string", required=True,
                              toolTip=AutoMLModelTrainer.tool_tips['gcp_credentials_path']))
        self.add_arg(Argument("model_id", "string", required=True,
                              toolTip=AutoMLModelTrainer.tool_tips['model_id']))
        self.add_arg(Argument("display_name", "string", required=True,
                              toolTip=AutoMLModelTrainer.tool_tips['display_name']))
        self.add_arg(Argument("region", "string", default="us-central1",
                              toolTip=AutoMLModelTrainer.tool_tips['region']))
        self.add_arg(Argument("project_path", "string",
                              toolTip=AutoMLModelTrainer.tool_tips['project_path']))
        self.add_arg(Argument("model_path", "string",
                              toolTip=AutoMLModelTrainer.tool_tips['project_path']))

        self.model_id = None
        self.model_path = None
        self.client = None

        self.dataset = None
        self.project_id = None
        self.project_path = None
        self.display_name = None
        self.gcp_credentials_path = None

        self.region = None

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))
        self.project_id = self.arg_value('project_id')
        self.region = self.arg_value('region')
        self.model_id = self.arg_value('model_id')
        self.display_name = self.arg_value('display_name')
        self.project_path = self.arg_value('project_path')
        self.model_path = self.arg_value('model_path')

        self.gcp_credentials_path = self.arg_value('gcp_credentials_path')
        os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = self.gcp_credentials_path

        self.client = automl.AutoMlClient()

    def process(self, frame):
        self.dataset = self.create_dataset(self.project_id, self.display_name, self.region)
        dataset_id = self._get_id(self.dataset)

        self.import_dataset(self.project_id, dataset_id, self.project_path, self.region)

        self.model = self.create_model(self.project_id, dataset_id, self.display_name, self.region)
        # self.model_path = self.model.operation.name
        # model_id = self.model.operation.name.split("/")[-1]  # self._get_id(self.model.operation)

        self.publish_model(self.model_path)

    @staticmethod
    def _get_id(name):
        """Parse a response name for its ID

        Args:
            name: AutoML class (e.g. Dataset or Model class)

        Returns:
            (str) the parsed name's ID (or its location basename)
        """
        return name.name.split("/")[-1]

    def create_dataset(self, project_id, display_name, region="us-central1"):
        """Create an empty dataset that will eventually hold the training data for the model.
        Specify the type of classification you want your custom model to perform:

        MULTICLASS assigns a single label to each classified image
        MULTILABEL allows an image to be assigned multiple labels
        https://cloud.google.com/automl/docs/reference/rpc/google.cloud.automl.v1#classificationtype

        Args:
            project_id: GC Project ID
            display_name: Project name
            region: GC Region ID (default: us-central1)

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
        print("Dataset name: {}".format(created_dataset.name))
        print("Dataset id: {}".format(created_dataset.name.split("/")[-1]))

        return created_dataset

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def import_dataset(self, project_id, dataset_id, path, region="us-central1"):
        """Takes as input a .csv file that lists the locations of all training images and the
        proper label for each one

        Args:
            project_id: GC Project ID
            dataset_id: Dataset ID
            path: Path to CSV
            region: GC Region ID (default: us-central1)

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
        self.reactor.emit_status("Total import time: {}".format(time.time()-start_time))

    def create_model(self, project_id, dataset_id, display_name, region="us-central1"):
        """Create and train a model

        Leave model unset to use the default base model provided by Google
        train_budget_milli_node_hours: The actual train_cost will be equal or
        less than this value.
        https://cloud.google.com/automl/docs/reference/rpc/google.cloud.automl.v1#imageclassificationmodelmetadata

        Args:
            project_id: GC Project ID
            dataset_id: Dataset ID
            display_name: Project name
            region: GC Region ID (default: us-central1)

        Returns:
            (google.cloud.automl_v1.types.Model¶) Model metadata
        """
        start_time = time.time()

        # A resource that represents Google Cloud Platform location.
        project_location = self.client.location_path(project_id, region)
        metadata = automl.types.ImageClassificationModelMetadata(train_budget=100)
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
        self.reactor.emit_status("Total training time: {}".format(time.time() - start_time))

        return response

    def publish_model(self, model):
        """Publish the model.

        Args:
            model: Full Model ID

        Returns:
            PipelineMod: The published Pipeline Module.
        """
        self.reactor.emit_status("Saving model: {}".format(self.app_model.name))
        model_dir = tempfile.mkdtemp() + '/' + self.app_model.name
        os.makedirs(model_dir)

        # deploy model
        model_full_id = self.client.model_path(self.project_id, self.region, model)
        self.client.deploy_model(model_full_id)

        # publish
        pmod = file_storage.models.save_model(model_dir, self.app_model)
        self.reactor.emit_status("Published model {}".format(self.app_model.name))
        return pmod

#
# class AutoMLDataSetBuilder:
#     """Build a Dataset in Google AutoML format"""
#
#     def __init__(self):
#         self.client = None
#
#     def init(self):
#         self.client = cloud.get_google_storage_client()
#
#     def build_dataset(self):
#
