import os
import csv
import boto3

from unittest.mock import patch
from pytest import approx

from zmlp.app import ModelApp
from zmlp.entity import Model
from zmlp_analysis.aws import RekognitionLabelClassifier
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path


class RekognitionLabelClassifierTests(PluginUnitTestCase):
    model_id = "model-id-12345"

    def setUp(self):
        aws_creds = zorroa_test_path('creds/zorroa_aws_credentials.csv')
        with open(aws_creds, 'r') as f:
            next(f)
            reader = csv.reader(f)
            for line in reader:
                os.environ['AWS_ACCESS_KEY_ID'] = line[2]
                os.environ['AWS_SECRET_ACCESS_KEY'] = line[3]

    def tearDown(self):
        del os.environ['AWS_ACCESS_KEY_ID']
        del os.environ['AWS_SECRET_ACCESS_KEY']

    @patch.object(ModelApp, "get_model")
    @patch("zmlp_analysis.aws.labels.get_proxy_level_path")
    @patch.object(boto3, "client")
    def test_predict(self, detect_patch, proxy_patch, model_patch):
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
        detect_patch.return_value = MockAWSClient()

        flower_paths = zorroa_test_path("training/test_dsy.jpg")
        proxy_patch.return_value = flower_paths
        frame = Frame(TestAsset(flower_paths))

        args = expected_results[0][0]
        expected = expected_results[0][1]

        processor = self.init_processor(RekognitionLabelClassifier(), args)
        processor.process(frame)

        assert processor.label_and_score == expected


expected_results = [
    (
        {"model_id": "model-id-12345"},
        [
            ('Plant', approx(99.90, 0.01)),
            ('Daisy', approx(99.59, 0.01))
        ]
    )
]


class MockAWSClient:

    def detect_labels(self, Image=None, MaxLabels=3):
        return {
            'Labels': [
                {
                    'Name': 'Plant',
                    'Confidence': 99.90
                },
                {
                    'Name': 'Daisy',
                    'Confidence': 99.59
                }
            ]
        }
