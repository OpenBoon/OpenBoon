import json
import os

import pytest

from boonai_analysis.aws.awscloud import AwsCloudResources
from boonflow.testing import PluginUnitTestCase


@pytest.mark.skip(reason='dont run automatically')
class AmazonCloudResourcesTest(PluginUnitTestCase):

    def setUp(self):

        with open('aws-env.json', 'r') as fp:
            self.aws_env = json.load(fp)

        os.environ['BOONAI_TASK_ID'] = 'test-task-1'
        for k, v in self.aws_env.items():
            os.environ[k] = v

        self.aws = AwsCloudResources()

    def tearDown(self):
        for k in self.aws_env.keys():
            del os.environ[k]
        self.aws.teardown()

    def test_init(self):
        self.aws = AwsCloudResources()
        assert self.aws.queue_url
        assert self.aws.topic_arn
        assert self.aws.sqs
        assert self.aws.sns
        assert self.aws.queue
        assert self.aws.topic
