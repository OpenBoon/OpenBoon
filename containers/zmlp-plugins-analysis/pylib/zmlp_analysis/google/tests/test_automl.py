import logging
from pytest import approx
from unittest.mock import patch

from google.cloud import automl_v1beta1 as automl

from zmlp.app import ModelApp
from zmlp.entity import Model
from zmlp_analysis.google import AutoMLModelClassifier
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path

logging.basicConfig()


class AutoMLModelClassifierTests(PluginUnitTestCase):
    def setUp(self):
        self.model = "ICN94225947477147648"
        self.test_img = zorroa_test_path("training/test_dsy.jpg")

    @patch.object(ModelApp, "get_model")
    @patch.object(automl.PredictionServiceClient, "predict")
    @patch("zmlp_analysis.google.automl.get_proxy_level_path")
    def test_predict(self, proxy_patch, predict_patch, model_patch):
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

        predict_patch.return_value = MockPrediction()

        args = {"model_id": self.model, "automl_model_id": MockAutoMLClient()}

        proxy_patch.return_value = self.test_img
        frame = Frame(TestAsset(self.test_img))

        processor = self.init_processor(AutoMLModelClassifier(), args)
        processor.process(frame)

        for result in processor.predictions.payload:
            assert result.display_name == "daisy"
            assert result.classification.score == approx(0.99, 0.01)


class MockAutoMLClient:

    def result(self):
        return self

    @property
    def name(self):
        return 'projects/zorroa-poc-dev/locations/us-central1/models/ICN94225947477147648'


class MockPrediction:

    @property
    def payload(self):
        return [MockPayload()]


class MockPayload:

    @property
    def display_name(self):
        return "daisy"

    @property
    def classification(self):
        return MockScore()


class MockScore:

    @property
    def score(self):
        return 0.99
