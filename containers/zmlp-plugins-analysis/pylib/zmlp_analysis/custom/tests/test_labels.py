import logging
import os
import shutil
from unittest.mock import patch

from zmlp.app import ModelApp
from zmlp.entity import Model
from zmlp_analysis.custom import TensorflowTransferLearningClassifier
from zmlpsdk.base import Frame
from zmlpsdk.storage import file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset

logging.basicConfig()


class TensorflowTransferLearningClassifierTests(PluginUnitTestCase):

    def setUp(self):
        try:
            shutil.rmetrree("/tmp/model-cache/models_model-id-12345_foo_bar")
        except Exception:
            print("Didn't clear out model cache, this is ok.")

    ds_id = "ds-id-12345"
    model_id = "model-id-12345"
    base_dir = os.path.dirname(__file__)

    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch("zmlp_analysis.custom.labels.get_proxy_level_path")
    def test_predict(self, proxy_patch, file_patch, model_patch):
        name = "custom-flowers-label-detection-tf2-xfer-mobilenet2"
        file_patch.return_value = "{}/{}.zip".format(self.base_dir, name)
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "dataSetId": self.ds_id,
                "type": "LABEL_DETECTION_MOBILENET2",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": name,
            }
        )

        args = {
            "model_id": self.model_id,
        }

        flower_paths = [
            "{}/test_dsy.jpg".format(self.base_dir),
            "{}/test_rose.png".format(self.base_dir),
        ]
        for paths in flower_paths:
            proxy_patch.return_value = paths
            frame = Frame(TestAsset(paths))

            processor = self.init_processor(
                TensorflowTransferLearningClassifier(), args
            )
            processor.process(frame)
