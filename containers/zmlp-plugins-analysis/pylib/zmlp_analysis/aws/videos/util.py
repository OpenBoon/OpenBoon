import time
import json
import logging

logger = logging.getLogger(__name__)


def create_topic_and_queue(sns_client, sqs_client, topic_name, queue_name):
    """
    Create a Topic and Queue

    Args:
        sns_client: SNS Topic Client
        sqs_client: SQS Queue Client
        topic_name: (str) Topic name
        queue_name: (str) Queue name

    Returns:
        (str, str, dict) SNS Topic ARN, SQS Queue URL
    """
    millis = str(int(round(time.time() * 1000)))

    # Create SNS topic
    sns_topic_name = f"{topic_name}_{millis}"

    topic_response = sns_client.create_topic(Name=sns_topic_name)
    sns_topic_arn = topic_response['TopicArn']

    # create SQS queue
    sqs_queue_name = f"{queue_name}_{millis}"
    sqs_client.create_queue(QueueName=sqs_queue_name)
    sqs_queue_url = sqs_client.get_queue_url(QueueName=sqs_queue_name)['QueueUrl']

    attribs = sqs_client.get_queue_attributes(QueueUrl=sqs_queue_url,
                                              AttributeNames=['QueueArn'])['Attributes']

    sqs_queue_arn = attribs['QueueArn']

    # Subscribe SQS queue to SNS topic
    sns_client.subscribe(
        TopicArn=sns_topic_arn,
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
        }}""".format(sqs_queue_arn, sns_topic_arn)

    sqs_client.set_queue_attributes(
        QueueUrl=sqs_queue_url,
        Attributes={'Policy': policy}
    )

    return sns_topic_arn, sqs_queue_url


def get_sqs_message_success(sqs_client, sqs_queue_url, start_job_id):
    """
    Check for SQS response meaning label detection has finished

    Args:
        sqs_client: AWS SNS Client
        sqs_queue_url: (str) SQS Queue URL
        start_job_id: (str) Job ID

    Returns:
        (bool) whether the job successfully finished or not
    """
    job_found = False
    succeeded = False

    while not job_found:
        sqs_response = sqs_client.receive_message(QueueUrl=sqs_queue_url,
                                                  MessageAttributeNames=['ALL'],
                                                  MaxNumberOfMessages=10)
        if sqs_response:
            if 'Messages' not in sqs_response:
                time.sleep(5)
                continue

            for message in sqs_response['Messages']:
                notification = json.loads(message['Body'])
                rek_message = json.loads(notification['Message'])
                logger.debug(rek_message['JobId'])
                logger.debug(rek_message['Status'])
                if rek_message['JobId'] == start_job_id:
                    logger.info('Matching Job Found:' + rek_message['JobId'])
                    job_found = True
                    if rek_message['Status'] == 'SUCCEEDED':
                        succeeded = True

                    sqs_client.delete_message(QueueUrl=sqs_queue_url,
                                              ReceiptHandle=message['ReceiptHandle'])
                else:
                    logger.info(
                        "Job didn't match:" + str(rek_message['JobId']) + ' : ' + start_job_id
                    )
                # Delete the unknown message. Consider sending to dead letter queue
                sqs_client.delete_message(QueueUrl=sqs_queue_url,
                                          ReceiptHandle=message['ReceiptHandle'])

    return succeeded


def delete_topic_and_queue(sqs_client, sns_client, sqs_queue_url, sns_topic_arn):
    """
    Delete Topic and Queues

    Args:
        sqs_client: SQS Client
        sns_client: SNS Client
        sqs_queue_url: SQS Queue URL to be deleted
        sns_topic_arn: SNS Topic ARN to be deleted

    Returns:
        None
    """
    sqs_client.delete_queue(QueueUrl=sqs_queue_url)
    sns_client.delete_topic(TopicArn=sns_topic_arn)


def start_detection(rek_client, bucket, video, role_arn, sns_topic_arn, func, **kwargs):
    """
    Start AWS Rekog video detection
    Args:
        rek_client: AWS Rekog Client
        bucket: (str) bucket name
        video: (str) video name with extension
        role_arn: (str) AWS ARN
        sns_topic_arn: (str) SNS Topic ARN
        func: (str) type of detection to run (label, face, text, segment)
        **kwargs: parameters to add when getting response

    Returns:
        (str) Job ID created for label detection
    """
    response = getattr(rek_client, func)(
        Video={'S3Object': {'Bucket': bucket, 'Name': video}},
        NotificationChannel={'RoleArn': role_arn, 'SNSTopicArn': sns_topic_arn},
        **kwargs
    )

    start_job_id = response['JobId']
    logger.debug('Start Job Id: ' + start_job_id)

    import json
    with open('text_detection.json', 'w') as fp:
        json.dump(response, fp)

    return start_job_id
