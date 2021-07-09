import os
import shutil
from unittest.mock import patch

import pytest

from boonsdk.app import ModelApp
from boonsdk.entity import Model
from boonflow.base import Frame
from boonflow.storage import file_storage
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path


from boonai_analysis.deployed.mar import TorchModelArchiveClassifier


class TorchModelArchiveTests(PluginUnitTestCase):
    model_id = "model-id-34568"
    base_dir = os.path.dirname(__file__)

    def setUp(self):
        try:
            shutil.rmtree("/tmp/boonai/model-cache")
        except FileNotFoundError:
            print("Didn't clear out model cache, this is ok.")

    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch("boonai_analysis.custom.pytorch.get_proxy_level_path")
    @patch.object(TorchModelArchiveClassifier, "predict")
    def test_image_classifier(self, predict_patch, proxy_patch, file_patch, model_patch):
        name = "custom-flowers-label-detection-tf2-xfer-mobilenet2"
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_CLASSIFIER",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": name,
                "moduleName": name
            }
        )
        predict_patch.return_value = [
            ("daisy", 0.998),
            ("cat", 0.222)
        ]

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080/predictions/resnet_152"
        }

        path = test_path("training/test_dsy.jpg")
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))

        processor = self.init_processor(
            TorchModelArchiveClassifier(), args
        )
        processor.process(frame)
        analysis = frame.asset.get_analysis(name)
        assert len(analysis['predictions']) == 2
        assert analysis['predictions'][0]['label'] == 'daisy'


@pytest.mark.skip(reason='dont run automatically')
class TorchModelArchiveIntegrationTests(PluginUnitTestCase):

    @patch.object(ModelApp, "get_model")
    @patch("boonai_analysis.deployed.mar.get_proxy_level_path")
    def test_image_classsifier(self, proxy_patch, model_patch):
        """
        Should have a resnet152 server deployed locally.
        https://github.com/pytorch/serve/tree/master/examples/image_classifier/resnet_152_batch

        """
        name = "custom-flowers-label-detection-tf2-xfer-mobilenet2"
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_CLASSIFIER",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": name,
                "moduleName": name
            }
        )
        path = test_path("images/set01/toucan.jpg")
        proxy_patch.return_value = path

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080/predictions/resnet_152"
        }

        frame = Frame(TestAsset(path))

        processor = self.init_processor(
            TorchModelArchiveClassifier(), args
        )
        processor.process(frame)
        analysis = frame.asset.get_analysis(name)

        assert len(analysis['predictions']) == 1
        assert analysis['predictions'][0]['label'] == 'toucan'
