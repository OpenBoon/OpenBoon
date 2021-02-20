import logging
import os
import string
import random
from unittest.mock import patch

from boonsdk.app import ModelApp
from boonsdk.entity import Model
from boonflow.base import Frame
from boonflow.storage import file_storage
from boonai_analysis.custom.perceptron import LabelDetectionPerceptionClassifier
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path

logging.basicConfig()

assets = [
    TestAsset(
        "flowers/roses/99383371_37a5ac12a3_n.jpg",
        id="9f0f8a1d-4719-5cf8-b427-4612c5597811",
    )
]


def id_generator(size=6, chars=string.ascii_uppercase):
    """Generate a random simhash

    Args:
        size: (int) size of hash
        chars: (str) values to use for the hash

    Returns:
        (str) generated similarity hash
    """
    return "".join(random.choice(chars) for _ in range(size))


class LabelDetectionClassifiersUnitTests(PluginUnitTestCase):

    model_id = "model-id-12345"
    base_dir = os.path.dirname(__file__)
    test_shash = id_generator(size=2048)

    def prep_assets(self):
        for asset in assets:
            asset.set_attr(
                "files",
                [
                    {
                        "id": asset.id + ".jpg",
                        "mimetype": "image/jpeg",
                        "category": "proxy",
                        "attrs": {
                            "width": 100,
                            "height": 100,
                            "path": asset.get_attr("source.path"),
                        },
                    }
                ],
            )

            asset.set_attr("shash", self.test_shash)

        return assets

    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.models, "install_model")
    @patch("boonai_analysis.custom.perceptron.LabelDetectionPerceptionClassifier.get_labels")
    def test_PerceptronClassifier(
        self, label_patch, file_patch, model_patch
    ):
        file_patch.return_value = test_path("training/pets")
        label_patch.return_value = ["dogs", "cats"]

        # dummy model
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "BOONAI_LABEL_DETECTION",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": "pets",
                "moduleName": "boonai-pets"
            }
        )
        args = {"model_id": self.model_id, "attr": "shash"}

        for asset in self.prep_assets():
            frame = Frame(asset)
            processor = self.init_processor(
                LabelDetectionPerceptionClassifier(), args
            )
            processor.process(frame)

            # since all hashes are random, prediction could be either dog or
            # cat so just need to check that it made a prediction at all
            assert asset.get_attr("analysis")["boonai-pets"]["pred0"]
