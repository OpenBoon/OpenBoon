import os
import logging
from unittest.mock import patch

from zmlpsdk.storage import FileStorage
from zmlp.app import ModelApp
from zmlp.entity import Model
from zmlp_train.tf2 import TensorflowTransferLearningClassifier
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset

logging.basicConfig()


class TensorflowTransferLearningClassifierTests(PluginUnitTestCase):
    ds_id = "ds-id-12345"
    model_id = "model-id-12345"
    base_dir = os.path.dirname(__file__)

    @patch.object(ModelApp, "get_model")
    @patch.object(FileStorage, "localize_file")
    @patch("zmlp_train.tf2.classify.get_proxy_level_path")
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
            "{}/flowers/test_daisy.jpg".format(self.base_dir),
            "{}/flowers/test_rose.png".format(self.base_dir),
        ]
        for paths in flower_paths:
            proxy_patch.return_value = paths
            frame = Frame(TestAsset(paths))

            processor = self.init_processor(
                TensorflowTransferLearningClassifier(), args
            )
            processor.process(frame)
