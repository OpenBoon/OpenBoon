import os
import json

from zmlp_analysis.aws.videos import util
from zmlpsdk.testing import PluginUnitTestCase


class MockSNSClient:
    def create_topic(self, Name=None):
        return {'TopicArn': 'abc123'}

    def subscribe(self, TopicArn=None, Protocol=None, Endpoint=None):
        return self

    def delete_topic(self, TopicArn=None):
        return self


class MockSQSClient:
    def create_queue(self, QueueName=None):
        return self

    def get_queue_url(self, QueueName=None):
        return {'QueueUrl': 'aws.com'}

    def get_queue_attributes(self, QueueUrl=None, AttributeNames=None):
        return {'Attributes': {'QueueArn': 'def456'}}

    def set_queue_attributes(self, QueueUrl=None, Attributes=None):
        return self

    def receive_message(self, QueueUrl=None, MessageAttributeNames=None, MaxNumberOfMessages=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data", "sqs_response.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def delete_message(self, QueueUrl=None, ReceiptHandle=None):
        return self

    def delete_queue(self, QueueUrl=None):
        self


class MockRekClient:

    def start_label_detection(self, Video=None, NotificationChannel=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "label_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def start_segment_detection(self, Video=None, NotificationChannel=None, SegmentTypes=None,
                                Filters=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "segment_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)


sns_client = MockSNSClient()
sqs_client = MockSQSClient()
rek_client = MockRekClient()


class RekognitionVideoDetectionUtilsTests(PluginUnitTestCase):

    def test_create_topic_and_queue(self):
        topic_name = 'MockTopicName'
        queue_name = 'MockQueueName'

        sns_topic_arn, sqs_queue_url = util.create_topic_and_queue(
            sns_client=sns_client,
            sqs_client=sqs_client,
            topic_name=topic_name,
            queue_name=queue_name
        )

        assert sns_topic_arn == 'abc123'
        assert sqs_queue_url == 'aws.com'

    def test_get_sqs_message_success(self):
        sqs_queue_url = 'aws.com'
        start_job_id = 'f5e123a1b8ab751ddd99e6ceec1ea6e829844eceb9b561df9a727e9074c3ec32'

        is_success = util.get_sqs_message_success(
            sqs_client=sqs_client,
            sqs_queue_url=sqs_queue_url,
            start_job_id=start_job_id
        )

        assert is_success

    def test_delete_topic_and_queue(self):
        sqs_queue_url = 'aws.com'
        sns_topic_arn = 'abc123'

        util.delete_topic_and_queue(
            sqs_client=sqs_client,
            sns_client=sns_client,
            sqs_queue_url=sqs_queue_url,
            sns_topic_arn=sns_topic_arn
        )

    def test_start_label_detection(self):
        job_id = util.start_label_detection(
            rek_client=rek_client,
            bucket='rgz-test',
            video='ted_talk.mp4',
            role_arn='abcd1234',
            sns_topic_arn='abc123'
        )

        assert job_id == '313bc6725817ff2740d34e104ef4187cab09cf86d0fc5f2d29703139b21c2575'

    def test_start_segment_detection(self):
        job_id = util.start_segment_detection(
            rek_client=rek_client,
            bucket='rgz-test',
            video='ted_talk.mp4',
            role_arn='abcd1234',
            sns_topic_arn='abc123'
        )

        assert job_id == 'c792050cf2a88c38d4ea1bf2de182c7d07d4b6497062b019ba99cd8b90be24ef'
