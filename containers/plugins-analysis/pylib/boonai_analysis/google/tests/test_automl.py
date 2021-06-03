import logging

from pytest import approx
from unittest.mock import patch

from boonsdk.app import ModelApp
from boonai_analysis.google import AutoMLModelClassifier
from boonflow.base import Frame
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path

logging.basicConfig()


class AutoMLModelClassifierTests(PluginUnitTestCase):
    test_img = test_path("training/test_dsy.jpg")

    @patch("boonflow.cloud.get_google_storage_client")
    @patch("boonflow.file_storage.projects.get_directory_location")
    @patch("boonai_analysis.google.automl.get_proxy_level_path")
    @patch.object(ModelApp, 'find_one_model')
    @patch.object(ModelApp, "get_model")
    def test_predict(self, model_patch,
                     find_model,
                     proxy_patch,
                     directory_location_patch,
                     gs_patch):
        frame = Frame(TestAsset(self.test_img))
        ml_client = MockAutoMLClient()
        args = {"model_id": ml_client.id, "automl_model_id": ml_client}

        model_patch.return_value = MockBoonaiModel()
        find_model.return_value = MockBoonaiModel()
        directory_location_patch.return_value = test_path("models/tflite")
        proxy_patch.return_value = self.test_img
        gs_patch.return_value = MockGsClient()

        processor = self.init_processor(AutoMLModelClassifier(), args)
        processor.process(frame)

        for result in processor.analysis.pred_list:
            assert result.label == "red_flower"
            assert result.score == approx(0.154, 0.01)


class MockGsClient:
    def list_blobs(self, *args, **kwargs):
        base = 'model-export_icn_tflite-model_exportable_2-2021-05-28T21_55_41.973132Z_'
        label_file_name = '%sdict.txt' % base
        model_file_name = '%smodel.tflite' % base
        return [MockBlob(test_path(f"models/tflite/{model_file_name}")),
                MockBlob(test_path(f"models/tflite/{label_file_name}"))]


class MockBoonaiModel:
    @property
    def id(self):
        return '02624c35-4626-4a3a-9f8f-c6b1c5a1f582'

    @property
    def name(self):
        return 'flowers'


class MockAutoMLClient:

    def result(self):
        return self

    @property
    def id(self):
        return 'ICN94225947477147648'

    @property
    def name(self):
        return 'projects/boonai-poc-dev/locations/us-central1/models/ICN94225947477147648'

    def predict(self, *args):
        return MockPrediction()


class MockAutoMlModel:

    @property
    def id(self):
        return 'ICN94225947477147648'

    @property
    def type(self):
        return "GCP_AUTOML_CLASSIFIER"

    @property
    def name(self):
        return f"gs://cloud-blucket/project/{self.id}/models/ICN94225947477147648/foo/bar"

    @property
    def display_name(self):
        return 'flowers'


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


class MockBlob:
    def __init__(self, name):
        self.name = name


class MockScore:

    @property
    def score(self):
        return 0.99
