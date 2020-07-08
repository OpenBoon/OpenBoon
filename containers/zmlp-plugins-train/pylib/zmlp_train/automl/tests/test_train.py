import os
import logging
from unittest.mock import patch

from google.cloud import automl_v1beta1 as automl

from zmlp.app import ModelApp
from zmlp.entity import Model, PipelineMod, StoredFile
from zmlp.training import TrainingSetDownloader
from zmlp_train.automl import AutoMLModelTrainer
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path

logging.basicConfig()


class AutoMLModelProcessorTests(PluginUnitTestCase):
    model_id = "model-id-12345"
    ds_id = "ds-id-12345"

    def setUp(self):
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = \
            zorroa_test_path('creds/zorroa-poc-dev-access.json')

    def tearDown(self):
        del os.environ['GOOGLE_APPLICATION_CREDENTIALS']

    @patch.object(AutoMLModelTrainer, 'import_dataset')
    @patch.object(AutoMLModelTrainer, 'create_model')
    @patch.object(AutoMLModelTrainer, '_get_id')
    @patch.object(file_storage.models, 'publish_model')
    @patch.object(ModelApp, 'get_model')
    @patch.object(automl.AutoMlClient, 'deploy_model')
    @patch.object(file_storage.projects, "store_file_by_id")
    @patch.object(TrainingSetDownloader, 'build')
    @patch.object(AutoMLModelTrainer, '_upload_to_gcs_bucket')
    def test_process(self, gcs_upload_patch, build_patch, upload_patch, deploy_patch, model_patch,
                     pub_patch, dataset_id_patch, create_model_patch, import_patch):
        # Prep the frame, asset, and proxy
        daisy_fname = zorroa_test_path('training/test_dsy.jpg')
        asset = TestAsset(daisy_fname)
        frame = Frame(asset)

        name = "flowers"
        pub_patch.return_value = PipelineMod({
            'id': "12345",
            'name': name
        })
        model_patch.return_value = Model({
            'id': self.model_id,
            'dataSetId': self.ds_id,
            'type': "AUTOML",
            'fileId': 'models/{}/foo/bar'.format(self.model_id),
            'name': name
        })
        dataset_id_patch.return_value = "ICN977145879209181184"
        upload_patch.return_value = StoredFile({"id": "12345"})

        # Prep the processor
        self.processor = AutoMLModelTrainer()
        project_id = 'zorroa-poc-dev'
        build_patch.return_value = 'csv_some_data.csv'
        gcs_upload_patch.return_value = 'gs://{}-vcm/csv/csv_some_data.csv'.format(project_id)
        args = {
            'model_id': self.model_id,
            'display_name': name,
            'model_path': "ICN94225947477147648"
        }
        self.init_processor(self.processor, args)

        # Process the frame with the processor
        self.processor.process(frame)
