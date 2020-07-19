from unittest.mock import patch
from pytest import approx

from zmlp.app import ModelApp
from zmlp.entity import Model
from zmlp_analysis.aws import RekognitionLabelClassifier
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path


class RekognitionLabelClassifierTests(PluginUnitTestCase):
    model_id = "model-id-12345"

    @patch.object(ModelApp, "get_model")
    @patch("zmlp_analysis.aws.labels.get_proxy_level_path")
    def test_predict(self, proxy_patch, model_patch):
        name = "flowers"
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "AWS_LABEL_DETECTION",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": name,
                "moduleName": name
            }
        )

        flower_paths = [
            zorroa_test_path("training/test_dsy.jpg"),
            zorroa_test_path("images/detect/dogbike.jpg")
        ]
        for i, paths in enumerate(flower_paths):
            proxy_patch.return_value = paths
            frame = Frame(TestAsset(paths))

            args = expected_results[i][0]
            expected = expected_results[i][1]

            processor = self.init_processor(RekognitionLabelClassifier(), args)
            processor.process(frame)

            assert processor.label_and_score == expected


expected_results = [
    (
        {"model_id": "model-id-12345", "max_labels": 2},
        [
            ('Plant', approx(99.90, 0.01)),
            ('Daisy', approx(99.59, 0.01))
        ]
    ),
    (
        {"model_id": "model-id-12345", "max_labels": 3},
        [
            ('Wheel', approx(99.74, 0.01)),
            ('Bicycle', approx(99.49, 0.01)),
            ('Dog', approx(95.98, 0.01))
        ]
    ),
]
