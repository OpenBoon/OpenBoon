import os
import shutil
from unittest.mock import patch

from boonsdk.app import ModelApp
from boonsdk.entity import Model
from boonai_analysis.custom.pytorch import PytorchTransferLearningClassifier
from boonflow.base import Frame
from boonflow.storage import file_storage
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path, get_prediction_labels


class PytorchModelImageClassifierTests(PluginUnitTestCase):
    model_id = "model-id-34568"
    base_dir = os.path.dirname(__file__)

    def setUp(self):
        try:
            shutil.rmtree("/tmp/model-cache/models_model-id-34568_foo_bar")
        except FileNotFoundError:
            print("Didn't clear out model cache, this is ok.")

    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch("boonai_analysis.custom.pytorch.get_proxy_level_path")
    def test_predict(self, proxy_patch, file_patch, model_patch):
        name = "pytorch_ants_and_bees"

        model_file = test_path("training/{}.zip".format(name))
        file_patch.return_value = model_file
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "BOONAI_PYTORCH_LABEL_DETECTION",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": name,
                "moduleName": name
            }
        )

        args = {
            "model_id": self.model_id,
            "input_size": (321, 321)
        }

        flower_paths = [
            # Using a greyscale image from the test set to test grey->RGB
            test_path("images/channels/greyscale.jpg"),
            # Also using a jpg with alpha because sometimes proxies are like that too
            test_path("images/channels/rgb_alpha.jpg"),
            test_path("training/test_rose.png")
        ]
        for paths in flower_paths:
            proxy_patch.return_value = paths
            frame = Frame(TestAsset(paths))

            processor = self.init_processor(
                PytorchTransferLearningClassifier(), args
            )
            processor.process(frame)
            analysis = frame.asset.get_analysis(name)
            assert 'bees' in get_prediction_labels(analysis)
            assert analysis['count'] == 2
            assert 'labels' == analysis['type']
