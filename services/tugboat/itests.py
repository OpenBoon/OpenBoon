import os
import unittest
import pytest

import server


@pytest.skip("Don't run automatically")
class ServerIntegrationTests(unittest.TestCase):
    def setUp(self):
        os.environ['GCLOUD_PROJECT'] = 'zvi-dev'
        os.environ['TEMPLATE_PATH'] = 'tmpl'

    def tearDown(self):
        del os.environ['GCLOUD_PROJECT']
        del os.environ['TEMPLATE_PATH']

    def test_build_and_deploy(self):
        spec = {
            'modelType': 'TORCH_MAR_CLASSIFIER',
            'modelId': 'd96733a8-de94-40c3-baef-92de50eb8398',
            'modelFile': 'https://storage.googleapis.com/zorroa-public/models/resnet-152-batch.mar',
            'image': 'gcr.io/zvi-dev/models/d96733a8-de94-40c3-baef-92de50eb8398'
        }

        server.build_and_deploy(spec)
