import logging
import random
import string

from botocore.exceptions import ClientError

from boonflow.base import BoonEnv
from boonai_analysis.aws.util import AwsEnv

logger = logging.getLogger(__name__)


class AwsCloudResources:
    """
    A class that builds the necessary cloud resources to process
    video using Aws, then deletes them.  The process involves:

        1. Make an SQS queue
        2. Make a SNS topic.
        3. Subscribe the topic to the queue so events written to topic end up in queue.
        4. Authorize the topic to write into the queue.

    The naming of the queues and topics are unique to guarantee they are empty.
    """

    def __init__(self):
        self.sns = AwsEnv.general_aws_client(service='sns')
        self.sqs = AwsEnv.general_aws_client(service='sqs')

        rand = ''.join(random.choice(string.ascii_lowercase) for i in range(10))
        name = "AmazonRekognition_{}_{}".format(BoonEnv.get_task_id(), rand)
        logger.info("Creating AWS resources: {}".format(name))

        self.queue = self.create_queue(name)
        self.topic = self.create_topic(name)

        # Need to fetch the queue ARN separately, for some reason.
        q_attrs = self.sqs.get_queue_attributes(
            QueueUrl=self.queue_url, AttributeNames=['QueueArn'])
        self.queue_arn = q_attrs['Attributes']['QueueArn']

        self.sns.subscribe(
            TopicArn=self.topic_arn,
            Protocol='sqs',
            Endpoint=self.queue_arn)

        # Authorize SNS to write SQS queue
        policy = """{{
            "Version":"2012-10-17",
            "Statement":[
            {{
              "Sid":"MyPolicy",
              "Effect":"Allow",
              "Principal" : {{"AWS" : "*"}},
              "Action":"SQS:SendMessage",
              "Resource": "{}",
              "Condition":{{
                "ArnEquals":{{
                  "aws:SourceArn": "{}"
                }}
              }}
            }}
            ]
            }}""".format(self.queue_arn, self.topic_arn)

        self.sqs.set_queue_attributes(
            QueueUrl=self.queue_url,
            Attributes={'Policy': policy}
        )

    @property
    def topic_arn(self):
        """The AWS ARN that points to the topic."""
        return self.topic['TopicArn']

    @property
    def queue_url(self):
        """The URL that points to the queue."""
        return self.queue['QueueUrl']

    def teardown(self):
        """
        Removes the topic and queue from AWS.
        """
        success = 0
        try:
            self.sqs.delete_queue(QueueUrl=self.queue_url)
            success += 1
        except Exception as e:
            logger.debug("Failed to cleanup AWS SQS queue", e)

        try:
            self.sns.delete_topic(TopicArn=self.topic_arn)
            success += 1
        except Exception as e:
            logger.debug("Failed to cleanup AWS SNS topic", e)

        return success == 2

    def create_queue(self, name):
        """
        Create an AWS queue with the given name.
        Args:
            name (str): the name of the queue.

        Returns:
            dict: The SQS queue
        """
        try:
            queue = self.sqs.create_queue(
                QueueName=name,
                Attributes={}
            )
            logger.debug("Created queue '%s' with URL=%s", name, queue)
            return queue
        except ClientError as error:
            logger.exception("Couldn't create queue named '%s'.", name)
            raise error

    def create_topic(self, name):
        """
        Create an AWS topc with the given name.
        Args:
            name (str): the name of the queue.

        Returns:
            dict: The SNS topic
        """
        try:
            topic = self.sns.create_topic(Name=name)
            logger.debug("Created topic %s with ARN %s.", name, topic)

            return topic
        except ClientError:
            logger.exception("Couldn't create topic %s.", name)
            raise

    def __del__(self):
        logger.info("Tearing down AWS resources")
        self.teardown()
