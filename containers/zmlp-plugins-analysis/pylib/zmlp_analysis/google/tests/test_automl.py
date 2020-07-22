import logging
import os
from pytest import approx
from unittest.mock import patch

from google.cloud import automl_v1beta1 as automl

from zmlp.app import ModelApp
from zmlp.entity import Model
from zmlp_analysis.google import AutoMLModelClassifier
from zmlpsdk import file_storage
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
    @patch.object(automl.PredictionServiceClient, "predict")
    @patch("zmlp_analysis.google.automl.get_proxy_level_path")
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_predict(self, proxy_patch, native_uri_patch, predict_patch, model_patch):
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
        native_uri_patch.return_value = "gs://foo/bar"
        test_asset = TestAsset(self.test_img)
        test_asset.set_attr('files', [
            {
                "attrs": {
                    "width": 10,
                    "height": 10
                },
                "mimetype": "image/jpeg",
                "category": "proxy"
            }
        ])
        predict_patch.return_value = MockPrediction()

        args = {"model_id": self.model, "automl_model_id": MockAutoMLClient()}

        proxy_patch.return_value = self.test_img
        frame = Frame(test_asset)

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
