import os

import pytest

from boonai_analysis.aws.util import AwsEnv
from boonflow.testing import PluginUnitTestCase


class UtilTests(PluginUnitTestCase):
    model_id = "model-id-12345"

    def test_get_zvi_rekognition_client_no_key(self):
        os.environ['BOONAI_AWS_SECRET'] = "abc123"
        try:
            with pytest.raises(RuntimeError):
                AwsEnv.rekognition()
        finally:
            del os.environ['BOONAI_AWS_SECRET']

    def test_get_zvi_rekognition_client_no_secret(self):
        os.environ['BOONAI_AWS_KEY'] = "abc123"
        try:
            with pytest.raises(RuntimeError):
                AwsEnv.rekognition()
        finally:
            del os.environ['BOONAI_AWS_KEY']

    def test_get_zvi_rekognition_client(self):
        os.environ['BOONAI_AWS_KEY'] = "abc123"
        os.environ['BOONAI_AWS_SECRET'] = "abc123"
        try:
            AwsEnv.rekognition()
        finally:
            del os.environ['BOONAI_AWS_KEY']
            del os.environ['BOONAI_AWS_SECRET']
