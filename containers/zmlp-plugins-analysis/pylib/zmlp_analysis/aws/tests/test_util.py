import os

from unittest.mock import patch
from zmlp_analysis.aws.tests.video.conftest import MockRekClient
import pytest

from zmlp_analysis.aws.util import AwsEnv, CustomModelTrainer
from zmlpsdk.testing import PluginUnitTestCase

rek_patch_path = 'zmlp_analysis.aws.util.AwsEnv.rekognition'


class UtilTests(PluginUnitTestCase):
    model_id = "model-id-12345"

    def test_get_zvi_rekognition_client_no_key(self):
        os.environ['ZORROA_AWS_SECRET'] = "abc123"
        try:
            with pytest.raises(RuntimeError):
                AwsEnv.rekognition()
        finally:
            del os.environ['ZORROA_AWS_SECRET']

    def test_get_zvi_rekognition_client_no_secret(self):
        os.environ['ZORROA_AWS_KEY'] = "abc123"
        try:
            with pytest.raises(RuntimeError):
                AwsEnv.rekognition()
        finally:
            del os.environ['ZORROA_AWS_KEY']

    def test_get_zvi_rekognition_client(self):
        os.environ['ZORROA_AWS_KEY'] = "abc123"
        os.environ['ZORROA_AWS_SECRET'] = "abc123"
        try:
            AwsEnv.rekognition()
        finally:
            del os.environ['ZORROA_AWS_KEY']
            del os.environ['ZORROA_AWS_SECRET']


class CustomModelTrainerTests(PluginUnitTestCase):
    test_arn = 'abc123'
    version = 'v1.0.0'

    @patch(rek_patch_path, side_effect=MockRekClient)
    def setUp(self, _):
        self.cmt = CustomModelTrainer(MockRekClient())
        self.cmt.init()

    def test_create_project(self):
        response = self.cmt.create_project(self.test_arn)
        assert response['ProjectArn'] == 'testArn'

    def test_start_model(self):
        response = self.cmt.start_model(self.test_arn, self.version, self.test_arn, 1)
        assert response['Status'] == 'STARTING'

    def test_stop_model(self):
        response = self.cmt.stop_model(self.test_arn)
        assert response['Status'] == 'STOPPED'

    def test_get_model_status(self):
        response = self.cmt.get_model_status(self.test_arn, self.version)
        assert response == 'RUNNING'

    def test_train_model(self):
        response = self.cmt.train_model(
            project_arn=self.test_arn,
            version_name=self.version,
            output_s3bucket='rgz-test',
            output_s3_key_prefix='eval',
            training_dataset_bucket='training_bucket',
            training_dataset_name='manifest'
        )
        assert response['ProjectVersionArn'] == 'test_projectVersionArn'
