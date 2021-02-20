import logging
import os
import shutil
from unittest.mock import patch

from boonsdk.app import ModelApp
from boonsdk.entity import Model
from boonai_analysis.custom import TensorflowTransferLearningClassifier
from boonflow.base import Frame
from boonflow.storage import file_storage
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path

logging.basicConfig()


class TensorflowTransferLearningClassifierTests(PluginUnitTestCase):
    model_id = "model-id-12345"
    base_dir = os.path.dirname(__file__)

    def setUp(self):
        try:
            shutil.rmtree("/tmp/model-cache/models_model-id-12345_foo_bar")
        except FileNotFoundError:
            print("Didn't clear out model cache, this is ok.")

    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch("boonai_analysis.custom.labels.get_proxy_level_path")
    def test_predict(self, proxy_patch, file_patch, model_patch):
        name = "custom-flowers-label-detection-tf2-xfer-mobilenet2"
        model_file = test_path("training/{}.zip".format(name))
        file_patch.return_value = model_file
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "BOONAI_LABEL_DETECTION",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": name,
                "moduleName": name
            }
        )

        args = {
            "model_id": self.model_id,
        }

        flower_paths = [
            test_path("training/test_dsy.jpg"),
            test_path("training/test_rose.png")
        ]
        for paths in flower_paths:
            proxy_patch.return_value = paths
            frame = Frame(TestAsset(paths))

            processor = self.init_processor(
                TensorflowTransferLearningClassifier(), args
            )
            processor.process(frame)
