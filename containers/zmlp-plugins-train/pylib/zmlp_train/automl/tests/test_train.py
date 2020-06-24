import os
import logging
from unittest.mock import patch

from zmlp.app import ModelApp
from zmlp.entity import Model, PipelineMod
from zmlp_train.automl import AutoMLModelTrainer
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_data

logging.basicConfig()

model_id = "model-id-12345"
ds_id = "ds-id-12345"


class AutoMLModelProcessorTests(PluginUnitTestCase):

    @patch.object(ModelApp, 'publish_model')
    @patch.object(ModelApp, 'get_model')
    def do_not_test_model_eval(self, model_patch, pub_patch):
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
            'project_path': 'gs://{}-vcm/csv/all_data.csv'.format(project_id)
        }
        self.init_processor(self.processor, args)

        # Process the frame with the processor
        self.processor.process(frame)
