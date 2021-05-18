import logging
import os

from pytest import approx
from unittest.mock import patch

from boonsdk import BoonClient
from boonsdk.app import ModelApp
from boonsdk.entity import Model
from shutil import copyfile
from boonai_analysis.google import AutoMLModelClassifier
from boonflow.base import Frame
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path
import tempfile


logging.basicConfig()


class AutoMLModelClassifierTests(PluginUnitTestCase):
    model = "ICN94225947477147648"
    test_img = test_path("training/test_dsy.jpg")

    # @patch("boonai_analysis.google.automl.automl.PredictionServiceClient")
    # @patch.object(ModelApp, "get_model")
    # @patch("boonai_analysis.google.automl.get_proxy_level_path")
    # def test_predict(self, proxy_patch, model_patch, client_patch):
    #     name = "flowers"
    #     model_patch.return_value = Model(
    #         {
    #             "id": self.model,
    #             "type": "GCP_AUTOML_CLASSIFIER",
    #             "fileId": "models/{}/foo/bar".format(self.model),
    #             "name": name,
    #             "moduleName": name
    #         }
    #     )
    #     client_patch.return_value = MockAutoMLClient()
    #
    #     args = {"model_id": self.model, "automl_model_id": MockAutoMLClient()}
    #
    #     proxy_patch.return_value = self.test_img
    #     frame = Frame(TestAsset(self.test_img))
    #
    #     processor = self.init_processor(AutoMLModelClassifier(), args)
    #     processor.process(frame)
    #
    #     for result in processor.predictions.payload:
    #         assert result.display_name == "daisy"
    #         assert result.classification.score == approx(0.99, 0.01)

    @patch("boonai_analysis.google.automl.get_proxy_level_path")
    @patch.object(tempfile, "mkdtemp")
    @patch.object(BoonClient, 'stream')
    @patch.object(ModelApp, 'find_one_model')
    @patch.object(ModelApp, "get_model")
    def test_predict(self, model_patch, find_model, client_stream, temp_dir, proxy_patch):
        frame = Frame(TestAsset(self.test_img))
        args = {"model_id": self.model, "automl_model_id": MockAutoMLClient()}
        name = "flowers"

        model = Model(
            {
                "id": self.model,
                "type": "GCP_AUTOML_CLASSIFIER",
                "fileId": "models/{}/foo/bar".format(self.model),
                "name": name,
                "moduleName": name
            }
        )

        model_patch.return_value = model
        find_model.return_value = model
        client_stream.return_value = test_path("models/tflite/model.zip")
        temp_dir.return_value = test_path("models/tflite")
        proxy_patch.return_value = self.test_img

        copyfile(test_path("models/tflite/model_test.zip"), test_path("models/tflite/model.zip"))

        processor = self.init_processor(AutoMLModelClassifier(), args)
        processor.process(frame)

        for result in processor.analysis.pred_list:
            assert result.label == "daisy"
            assert result.score == approx(0.255, 0.01)

        os.unlink("labels_mobilenet_quant_v1_224.txt")
        os.unlink("mobilenet_v1_1.0_224_quant.tflite")

class MockAutoMLClient:

    def result(self):
        return self

    @property
    def name(self):
        return 'projects/boonai-poc-dev/locations/us-central1/models/ICN94225947477147648'

    def predict(self, *args):
        return MockPrediction()


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
