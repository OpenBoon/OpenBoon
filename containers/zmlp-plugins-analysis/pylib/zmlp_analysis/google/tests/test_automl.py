import logging
import os
from pytest import approx
from unittest.mock import patch

from zmlp.app import ModelApp
from zmlp.entity import Model
from zmlp_analysis.google import AutoMLModelClassifier
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path

logging.basicConfig()


class AutoMLModelClassifierTests(PluginUnitTestCase):
    def setUp(self):
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = zorroa_test_path(
            'creds/zorroa-poc-dev-access.json')
        self.model = "ICN94225947477147648"
        self.test_img = zorroa_test_path("training/test_dsy.jpg")

    def tearDown(self):
        del os.environ["GOOGLE_APPLICATION_CREDENTIALS"]

    @patch.object(ModelApp, "get_model")
    @patch("zmlp_analysis.google.automl.get_proxy_level_path")
    def test_predict(self, proxy_patch, model_patch):
        name = "flowers"
        model_patch.return_value = Model(
            {
                "id": self.model,
                "type": "GCP_LABEL_DETECTION",
                "fileId": "models/{}/foo/bar".format(self.model),
                "name": name,
                "moduleName": name
            }
        )

        args = {"model_id": self.model, "score_threshold": "0.5"}

        proxy_patch.return_value = self.test_img
        frame = Frame(TestAsset(self.test_img))

        processor = self.init_processor(AutoMLModelClassifier(), args)
        processor.process(frame)

        for result in processor.predictions.payload:
            assert result.display_name == "daisy"
            assert result.classification.score == approx(0.99, 0.01)
