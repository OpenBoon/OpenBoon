import sys
import time
import json
import logging

from zmlpsdk import AssetProcessor, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path
from zmlpsdk.cloud import get_aws_client

from .util import get_zvi_rekognition_client

logger = logging.getLogger(__name__)


class RekognitionImageLabelDetection(AssetProcessor):
    """Get labels for an image using AWS Rekognition """

    namespace = 'aws-label-detection'

    file_types = FileTypes.documents | FileTypes.images

    def __init__(self):
        super(RekognitionImageLabelDetection, self).__init__()
        self.client = None

    def init(self):
        # AWS client
        self.client = get_zvi_rekognition_client()

    def process(self, frame):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        """
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 0)
        analysis = LabelDetectionAnalysis()

        for ls in self.predict(proxy_path, analysis.max_predictions):
            analysis.add_label_and_score(ls[0], ls[1])

        asset.add_analysis(self.namespace, analysis)

    def predict(self, path, max_predictions):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path
            max_predictions (int): max number of predictions save.

        Returns:
            list: a list of predictions
        """
        with open(path, 'rb') as f:
            source_bytes = f.read()

        # get predictions
        img_json = {'Bytes': source_bytes}
        response = self.client.detect_labels(
            Image=img_json,
            MaxLabels=max_predictions
        )

        # get list of labels
        return [(r['Name'], r['Confidence']) for r in response['Labels']]


class RekognitionVideoLabelDetection(AssetProcessor):
    """Get labels for a video using AWS Rekognition """

    namespace = 'aws-video-label-detection'

    file_types = FileTypes.documents | FileTypes.images

    def __init__(self, role_arn, bucket, video):
        super(RekognitionVideoLabelDetection, self).__init__()

        self.rek = None
        self.sns = None
        self.sqs = None

        self.sqs_queue_url = None
        self.start_job_id = None
        self.sns_topic_arn = None

        self.role_arn = role_arn
        self.bucket = bucket
        self.video = video

        self.analysis = None

    def init(self):
        # Rekognition client
        self.rek = get_zvi_rekognition_client()
        # SNS Topic client
        self.sns = get_aws_client('sns')
        # SQS Queue client
        self.sns = get_aws_client('sqs')

    def process(self, frame):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        """
        asset = frame.asset
        self.analysis = LabelDetectionAnalysis()

        # create topic and queue
        self.create_topic_and_queue()

        # start label detection for a video
        self.start_label_detection()

        # get results if successful sqs message
        if self.get_sqs_message_success():
            for ls in self.get_label_detection_results():
                self.analysis.add_label_and_score(ls[0], ls[1], timestamp=ls[2], bbox=ls[3])

        asset.add_analysis(self.namespace, self.analysis)

        # delete the topic and queue
        self.delete_topic_and_queue()

    def get_sqs_message_success(self):
        """ Get SQS message

        Returns:
            (bool) True if successful SQS message else False
        """
        job_found = False
        succeeded = False

        dot_line = 0
        while not job_found:
            sqs_response = self.sqs.receive_message(QueueUrl=self.sqs_queue_url,
                                                    MessageAttributeNames=['ALL'],
                                                    MaxNumberOfMessages=10)
            if sqs_response:
                if 'Messages' not in sqs_response:
                    if dot_line < 40:
                        logger.debug('.', end='')
                        dot_line = dot_line + 1
                    else:
                        logger.debug()
                        dot_line = 0
                    sys.stdout.flush()
                    time.sleep(5)
                    continue

                for message in sqs_response['Messages']:
                    notification = json.loads(message['Body'])
                    rek_message = json.loads(notification['Message'])
                    if rek_message['JobId'] == self.start_job_id:
                        logger.debug('Matching Job Found:' + rek_message['JobId'])
                        job_found = True
                        if rek_message['Status'] == 'SUCCEEDED':
                            succeeded = True
                        self.sqs.delete_message(QueueUrl=self.sqs_queue_url,
                                                ReceiptHandle=message['ReceiptHandle'])
                    else:
                        logger.debug("Job didn't match:" + str(rek_message['JobId']) + ' : ' +
                                     self.start_job_id)
                    # Delete the unknown message. Consider sending to dead letter queue
                    self.sqs.delete_message(QueueUrl=self.sqs_queue_url,
                                            ReceiptHandle=message['ReceiptHandle'])
        return succeeded

    def start_label_detection(self):
        """ Run Rekognition Video Label Detection

        Returns:
            None
        """
        response = self.rek.start_label_detection(
            Video={
                'S3Object': {
                    'Bucket': self.bucket,
                    'Name': self.video
                }
            },
            NotificationChannel={
                'RoleArn': self.role_arn,
                'SNSTopicArn': self.sns_topic_arn
            }
        )

        self.start_job_id = response['JobId']
        logger.debug('Start Job Id: ' + self.start_job_id)

    def get_label_detection_results(self):
        """ Get Label Detection results

        Returns:
            (List[str]): list of predictions
        """
        results = []
        pagination_token = ''
        finished = False

        while not finished:
            response = self.rek.get_label_detection(JobId=self.start_job_id,
                                                    MaxResults=self.analysis.max_predictions,
                                                    NextToken=pagination_token,
                                                    SortBy='TIMESTAMP')
            for r in response['Labels']:
                timestamp = r['Timestamp']
                label = r['Label']
                name = label['Name']
                confidence = label['Confidence']
                instances = label['Instances']

                logger.debug("Timestamp: " + str(timestamp))
                logger.debug("   Label: " + name)
                logger.debug("   Confidence: " + str(confidence))
                logger.debug("   Instances:")
                if instances:
                    for instance in instances:
                        i_confidence = instance['Confidence']
                        bbox = instance['BoundingBox']

                        left = bbox['Left']
                        top = bbox['Top']
                        width = bbox['Width']
                        height = bbox['Height']

                        bbox_result = [left, top, left + width, top + height]
                        results.append((name, i_confidence, timestamp, bbox_result))
                else:
                    results.append((name, confidence, timestamp, []))

                if 'NextToken' in response:
                    pagination_token = response['NextToken']
                else:
                    finished = True

        return results

    def create_topic_and_queue(self):
        """ Create SNS Topic and SQS Queue

        Returns:
            None
        """
        millis = str(int(round(time.time() * 1000)))

        # Create SNS topic

        sns_topic_name = "AmazonRekognitionExample" + millis

        topic_response = self.sns.create_topic(Name=sns_topic_name)
        self.sns_topic_arn = topic_response['TopicArn']

        # create SQS queue
        sqs_queue_name = "AmazonRekognitionQueue" + millis
        self.sqs.create_queue(QueueName=sqs_queue_name)
        self.sqs_queue_url = self.sqs.get_queue_url(QueueName=sqs_queue_name)['QueueUrl']

        attribs = self.sqs.get_queue_attributes(QueueUrl=self.sqs_queue_url,
                                                AttributeNames=['QueueArn'])['Attributes']

        sqs_queue_arn = attribs['QueueArn']

        # Subscribe SQS queue to SNS topic
        self.sns.subscribe(
            TopicArn=self.sns_topic_arn,
            Protocol='sqs',
            Endpoint=sqs_queue_arn)

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
        }}""".format(sqs_queue_arn, self.sns_topic_arn)

        _ = self.sqs.set_queue_attributes(
            QueueUrl=self.sqs_queue_url,
            Attributes={'Policy': policy}
        )

    def delete_topic_and_queue(self):
        """ Delete SNS Topic and SQS Queue

        Returns:
            None
        """
        self.sqs.delete_queue(QueueUrl=self.sqs_queue_url)
        self.sns.delete_topic(TopicArn=self.sns_topic_arn)
