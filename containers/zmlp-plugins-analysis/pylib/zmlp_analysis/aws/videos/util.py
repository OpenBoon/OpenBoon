import time
import sys
import json
import logging

logger = logging.getLogger(__name__)


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

    dot_line = 0
    while not job_found:
        sqs_response = sqs_client.receive_message(QueueUrl=sqs_queue_url,
                                                  MessageAttributeNames=['ALL'],
                                                  MaxNumberOfMessages=10)

        if sqs_response:
            if 'Messages' not in sqs_response:
                if dot_line < 40:
                    print('.', end='')
                    dot_line = dot_line + 1
                else:
                    print()
                    dot_line = 0
                sys.stdout.flush()
                time.sleep(5)
                continue

            for message in sqs_response['Messages']:
                notification = json.loads(message['Body'])
                rek_message = json.loads(notification['Message'])
                print(rek_message['JobId'])
                print(rek_message['Status'])
                if rek_message['JobId'] == start_job_id:
                    logging.info('Matching Job Found:' + rek_message['JobId'])
                    job_found = True
                    if rek_message['Status'] == 'SUCCEEDED':
                        succeeded = True

                    sqs_client.delete_message(QueueUrl=sqs_queue_url,
                                              ReceiptHandle=message['ReceiptHandle'])
                else:
                    logging.info(
                        "Job didn't match:" + str(rek_message['JobId']) + ' : ' + start_job_id
                    )
                # Delete the unknown message. Consider sending to dead letter queue
                sqs_client.delete_message(QueueUrl=sqs_queue_url,
                                          ReceiptHandle=message['ReceiptHandle'])

    return succeeded


def start_label_detection(rek_client, bucket, video, role_arn, sns_topic_arn):
    """
    Start AWS Rekog label detection

    Args:
        rek_client: AWS Rekog Client
        bucket: (str) bucket name
        video: (str) video name with extension
        role_arn: (str) AWS ARN
        sns_topic_arn: (str) SNS Topic ARN

    Returns:
        (str) Job ID created for label detection
    """
    response = rek_client.start_label_detection(
        Video={'S3Object': {'Bucket': bucket, 'Name': video}},
        NotificationChannel={'RoleArn': role_arn, 'SNSTopicArn': sns_topic_arn})

    start_job_id = response['JobId']
    print('Start Job Id: ' + start_job_id)
    return start_job_id


def get_label_detection_results(rek_client, start_job_id, max_results=10):
    """
    Run AWS Rekog label detection and get results

    Args:
        rek_client: AWS Rekog Client
        start_job_id: (str) Job ID
        max_results: (int) maximum results to get, default 10

    Returns:
        (dict) label detection response
    """
    pagination_token = ''
    finished = False

    while not finished:
        response = rek_client.get_label_detection(JobId=start_job_id,
                                                  MaxResults=max_results,
                                                  NextToken=pagination_token,
                                                  SortBy='TIMESTAMP')

        print('Codec: ' + response['VideoMetadata']['Codec'])
        print('Duration: ' + str(response['VideoMetadata']['DurationMillis']))
        print('Format: ' + response['VideoMetadata']['Format'])
        print('Frame rate: ' + str(response['VideoMetadata']['FrameRate']))
        print()

        for labelDetection in response['Labels']:
            label = labelDetection['Label']

            print("Timestamp: " + str(labelDetection['Timestamp']))
            print("   Label: " + label['Name'])
            print("   Confidence: " + str(label['Confidence']))
            print("   Instances:")
            for instance in label['Instances']:
                print("      Confidence: " + str(instance['Confidence']))
                print("      Bounding box")
                print("        Top: " + str(instance['BoundingBox']['Top']))
                print("        Left: " + str(instance['BoundingBox']['Left']))
                print("        Width: " + str(instance['BoundingBox']['Width']))
                print("        Height: " + str(instance['BoundingBox']['Height']))
                print()
            print()
            print("   Parents:")
            for parent in label['Parents']:
                print("      " + parent['Name'])
            print()

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

    return response


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

    response = sqs_client.set_queue_attributes(
        QueueUrl=sqs_queue_url,
        Attributes={'Policy': policy}
    )

    return sns_topic_arn, sqs_queue_url


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


def start_segment_detection(rek_client, bucket, video, role_arn, sns_topic_arn):
    """
    Start AWS Rekog segment detection

    Args:
        rek_client: AWS Rekog Client
        bucket: (str) bucket name
        video: (str) video name with extension
        role_arn: (str) AWS ARN
        sns_topic_arn: (str) SNS Topic ARN

    Returns:
        (str) Job ID created for label detection
    """
    min_techincal_cue_confidence = 80.0
    min_shot_confidence = 80.0

    response = rek_client.start_segment_detection(
        Video={'S3Object': {'Bucket': bucket, 'Name': video}},
        NotificationChannel={'RoleArn': role_arn, 'SNSTopicArn': sns_topic_arn},
        SegmentTypes=['TECHNICAL_CUE', 'SHOT'],
        Filters={'TechnicalCueFilter': {'MinSegmentConfidence': min_techincal_cue_confidence},
                 'ShotFilter': {'MinSegmentConfidence': min_shot_confidence}})

    start_job_id = response['JobId']
    print('Start Job Id: ' + start_job_id)
    return start_job_id


def get_segment_detection_results(rek_client, start_job_id, max_results=10):
    """
        Run AWS Rekog label detection and get results

        Args:
            rek_client: AWS Rekog Client
            start_job_id: (str) Job ID
            max_results: (int) maximum results to get, default 10

        Returns:
            (dict) segment detection response
        """
    pagination_token = ''
    finished = False

    while not finished:
        response = rek_client.get_segment_detection(JobId=start_job_id,
                                                    MaxResults=max_results,
                                                    NextToken=pagination_token)
        for segment in response['Segments']:

            if segment['Type'] == 'TECHNICAL_CUE':
                print('Technical Cue')
                print('\tConfidence: ' +
                      str(segment['TechnicalCueSegment']['Confidence']))
                print('\tType: ' +
                      segment['TechnicalCueSegment']['Type'])

            if segment['Type'] == 'SHOT':
                print('Shot')
                print('\tConfidence: ' +
                      str(segment['ShotSegment']['Confidence']))
                print('\tIndex: ' +
                      str(segment['ShotSegment']['Index']))

            print('\tDuration (milliseconds): ' + str(segment['DurationMillis']))
            print('\tStart Timestamp (milliseconds): ' + str(segment['StartTimestampMillis']))
            print('\tEnd Timestamp (milliseconds): ' + str(segment['EndTimestampMillis']))
            print('\tStart timecode: ' + segment['StartTimecodeSMPTE'])
            print('\tEnd timecode: ' + segment['EndTimecodeSMPTE'])
            print('\tDuration timecode: ' + segment['DurationSMPTE'])
            print()

        if 'NextToken' in response:
            pagination_token = response['NextToken']
        else:
            finished = True

    return response
