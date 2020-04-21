#!/usr/bin/env python

import os

from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_data, TestAsset
from ..automl import AutoMLVisionModelProcessor


class AutoMLVisionUnitTests(PluginUnitTestCase):

    # Test disabled until Travis or GitLab have latest version of zorroa-test-data
    def do_not_test_model_eval(self):
        # Prep the frame, asset, and proxy
        toucan_fname = zorroa_test_data('images/set01/toucan.jpg')
        asset = TestAsset(toucan_fname)
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
