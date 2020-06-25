import os
import logging
from unittest.mock import patch, PropertyMock

from google.cloud import automl_v1beta1 as automl
from google.api_core.operation import Operation

from zmlp.app import ModelApp
from zmlp.entity import Model, PipelineMod, StoredFile
from zmlp_train.automl import AutoMLModelTrainer
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_data

logging.basicConfig()

model_id = "model-id-12345"
ds_id = "ds-id-12345"


class AutoMLModelProcessorTests(PluginUnitTestCase):

    @patch.object(AutoMLModelTrainer, 'import_dataset')
    @patch.object(AutoMLModelTrainer, 'create_model')
    @patch.object(AutoMLModelTrainer, '_get_id')
    @patch.object(ModelApp, 'publish_model')
    @patch.object(ModelApp, 'get_model')
    @patch.object(automl.AutoMlClient, 'deploy_model')
    @patch.object(file_storage.projects, "store_file_by_id")
    def test_process(self, upload_patch, deploy_patch, model_patch, pub_patch, dataset_id_patch,
                     create_model_patch, import_patch):
        # Prep the frame, asset, and proxy
        toucan_fname = zorroa_test_data('training/test_dsy.jpg').split("file://")[-1]
        asset = TestAsset(toucan_fname)
        frame = Frame(asset)

        name = "flowers"
        pub_patch.return_value = PipelineMod({
            'id': "12345",
            'name': name
        })
        model_patch.return_value = Model({
            'id': model_id,
            'dataSetId': ds_id,
            'type': "LABEL_DETECTION_PERCEPTRON",
            'fileId': 'models/{}/foo/bar'.format(model_id),
            'name': name
        })
        import_patch.return_value = None
        dataset_id_patch.return_value = "ICN443750798342488064"
        create_model_patch.return_value = None
        deploy_patch.return_value = None
        upload_patch.return_value = StoredFile({"id": "12345"})

        # Prep the processor
        self.processor = AutoMLModelTrainer()
        project_id = 'zorroa-dev-rg'
        creds_path = os.path.join(zorroa_test_data(), 'creds', 'zorroa-dev-rg-access.json')
        creds_path = creds_path.split("file://")[-1]
        args = {
            'project_id': project_id,
            'region': 'us-central1',
            'model_id': model_id,
            'gcp_credentials_path': creds_path,
            'display_name': name,
            'project_path': 'gs://{}-vcm/csv/csv_some_data.csv'.format(project_id),
            'model_path': "ICN655064838573129728"
        }
        self.init_processor(self.processor, args)

        # Process the frame with the processor
        self.processor.process(frame)
