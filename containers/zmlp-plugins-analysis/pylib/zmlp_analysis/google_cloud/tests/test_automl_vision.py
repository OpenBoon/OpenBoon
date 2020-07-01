#!/usr/bin/env python

import os
import logging
import pytest
from unittest.mock import patch

from zmlp.app import ModelApp
from zmlp.entity import Model
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_data, zorroa_test_path, TestAsset
from ..automl import AutoMLVisionModelProcessor, AutoMLModelClassifier

logging.basicConfig()
CREDS = zorroa_test_path('creds/zorroa-poc-dev-access.json')


class AutoMLVisionUnitTests(PluginUnitTestCase):
    ds_id = "ds-id-12345"
    model_id = "model-id-12345"
    toucan_fname = zorroa_test_path("training/test_dsy.jpg")

    def setUp(self):
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = CREDS

    def tearDown(self):
        del os.environ['GOOGLE_APPLICATION_CREDENTIALS']

    # Test disabled until Travis or GitLab have latest version of zorroa-test-data
    def do_not_test_model_eval(self):
        # Prep the frame, asset, and proxy
        asset = TestAsset(self.toucan_fname)
        frame = Frame(asset)

        # Prep the processor
        # The model_id below corresponds to a pre-trained model in the zorroa-poc-dev project:
        # https://cloud.google.com/automl/ui/vision/datasets/evaluate?dataset=ICN77934513457048404
        # &model=ICN8132661290393741944&project=zorroa-poc-dev
        # The creds file is required, because if you're not running in the zorroa-poc-dev
        # project, you need the creds to access the model.
        self.processor = AutoMLVisionModelProcessor()
        args = {
            'project_id': 'zorroa-poc-dev',
            'location_id': 'us-central1',
            'model_id': 'ICN8132661290393741944',
            'gcp_credentials_path': os.path.join(zorroa_test_data(), 'creds',
                                                 'zorroa-poc-dev-access.json'),
        }
        self.init_processor(self.processor, args)

        # Process the frame with the processor
        self.processor.process(frame)

        # Get the results
        scores = frame.asset.get_attr('analysis.google.automl_vision.vision_01')
        self.processor.logger.info(scores)

        # Check the number of keys
        self.assertEqual(len(scores.keys()), 1)
        self.assertAlmostEqual(scores['fruit'], 0.9670690298080444)

    @patch.object(ModelApp, "get_model")
    @patch("zmlp_analysis.google_cloud.automl.get_proxy_level_path")
    def test_process(self, proxy_patch, model_patch):
        model_name = "flowers"
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "dataSetId": self.ds_id,
                "type": "LABEL_DETECTION_MOBILENET2",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": model_name,
            }
        )
        proxy_patch.return_value = self.toucan_fname

        asset = TestAsset(self.toucan_fname)
        frame = Frame(asset)

        self.processor = AutoMLModelClassifier()
        args = {
            'project_id': 'zorroa-poc-dev',
            'region': 'us-central1',
            'model_id': self.model_id,
            'model_path': 'ICN94225947477147648',
            'score_threshold': "0.1"
        }
        self.init_processor(self.processor, args)

        # Process the frame with the processor
        self.processor.process(frame)

        # Get the results
        scores = frame.asset.get_attr('analysis.{}'.format(model_name))

        # Check the number of keys
        assert list(scores.keys()) == ['type', 'count', 'predictions']
        for prediction in scores['predictions']:
            assert prediction['label'] == 'daisy'
            assert prediction['score'] == pytest.approx(0.99, rel=1e-2)
