# Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
# PDX-License-Identifier: MIT-0 (For details, see https://github.com/awsdocs/amazon-rekognition-developer-guide/blob/master/LICENSE-SAMPLECODE.)

import os
import tempfile

from zmlp_analysis.aws.util import AwsEnv
from zmlp_analysis.aws.videos.util import *
from zmlpsdk import AssetProcessor, FileTypes, ZmlpEnv, file_storage, proxy, clips, video

MAX_LENGTH_SEC = 120


class AbstractVideoDetectProcessor(AssetProcessor):
    """ AWS Rekognition for Video Detection"""
    namespace = 'aws-video-detection'
    file_types = FileTypes.videos

    def __init__(self):
        super(AbstractVideoDetectProcessor, self).__init__()

        self.rek_client = None
        self.s3_client = None
        self.sqs_client = None
        self.sns_client = None

        self.jobId = None
        self.roleArn = None
        self.bucket = None
        self.video = None
        self.startJobId = None

        self.sqsQueueUrl = None
        self.snsTopicArn = None
        self.processType = None

    def init(self):
        self.rek_client = AwsEnv.rekognition()
        self.s3_client = AwsEnv.s3()
        self.sqs_client = AwsEnv.general_aws_client(service='sqs')
        self.sns_client = AwsEnv.general_aws_client(service='sns')

        self.jobId = ''
        self.roleArn = 'arn:aws:iam::018430816410:role/ZRgRekog'
        self.bucket = ''
        self.video = ''
        self.startJobId = ''

        self.sqsQueueUrl = ''
        self.snsTopicArn = ''
        self.processType = ''

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        final_time = asset.get_attr('media.length')

        if final_time > MAX_LENGTH_SEC:
            self.logger.warning(
                'Skipping, video is longer than {} seconds.'.format(MAX_LENGTH_SEC))
            return

        video_proxy = proxy.get_video_proxy(asset)

        if not video_proxy:
            self.logger.warning(f'No video could be found for {asset_id}')
            return

        local_path = file_storage.localize_file(video_proxy)
        output_path = tempfile.mkstemp(".jpg")[1]
        clip_tracker = clips.ClipTracker(asset, self.namespace)

        ext = os.path.splitext(local_path)[1]
        bucket_file = f'{ZmlpEnv.get_project_id()}/video/{asset_id}{ext}'
        bucket_name = AwsEnv.get_bucket_name()

        # upload to s3
        self.s3_client.upload_file(local_path, bucket_name, bucket_file)

        # get audio s3 uri
        s3_uri = f's3://{bucket_name}/{bucket_file}'

        sns_topic_arn, sqs_queue_url = self.create_topic_queue(asset_id)

        try:
            clip_tracker = self.start_detection_analysis(
                clip_tracker=clip_tracker,
                output_dir=local_path,
                output_path=output_path,
                role_arn=self.roleArn,
                bucket=bucket_name,
                video=bucket_file,
                sns_topic_arn=sns_topic_arn,
                sqs_queue_url=sqs_queue_url
            )
        finally:
            self.sqs_client.delete_queue(QueueUrl=sqs_queue_url)
            self.sns_client.delete_topic(TopicArn=sns_topic_arn)

        timeline = clip_tracker.build_timeline(final_time)
        video.save_timeline(timeline)

    def create_topic_queue(self, name):
        """
        Create AWS SNS Topic and SQS Queue

        Args:
            name: (str) the name that will be prepended to the topic and queue

        Returns:
            tuple(str, str) (SNS Topic ARN, SQS Queue URL)
        """
        prepend_name = "AmazonRekognition"

        return create_topic_and_queue(
            self.sns_client,
            self.sqs_client,
            topic_name=f"{prepend_name}-{name}-topic",
            queue_name=f"{prepend_name}-{name}-queue"
        )

    def start_detection_analysis(self, clip_tracker, output_dir, output_path, role_arn, bucket,
                                 video, sns_topic_arn, sqs_queue_url):
        """
        Start Detection Analysis

        Args:
            role_arn: (str) AWS Role ARN
            bucket: (str) Bucket name only (i.e. "zorroa-dev" in "gs://zorroa-dev")
            video: (str) video name without extension ("video" instead of "video.mp4")
            sns_topic_arn: (str) SNS Topic ARN
            sqs_queue_url: (str) SQS Queue URL

        Returns:
            (dict) Label Detection Results
        """
        raise NotImplementedError


class LabelVideoDetectProcessor(AbstractVideoDetectProcessor):
    """ Label Detection for Videos using AWS """
    def __init__(self):
        super(LabelVideoDetectProcessor, self).__init__()

    def start_detection_analysis(self, clip_tracker, output_dir, output_path, role_arn, bucket, \
                                 video, sns_topic_arn, sqs_queue_url):
        """
        Start Label Detection Analysis

        Args:
            role_arn: (str) AWS Role ARN
            bucket: (str) Bucket name only (i.e. "zorroa-dev" in "gs://zorroa-dev")
            video: (str) video name without extension ("video" instead of "video.mp4")
            sns_topic_arn: (str) SNS Topic ARN
            sqs_queue_url: (str) SQS Queue URL

        Returns:
            (dict) Label Detection Results
        """
        start_job_id = start_label_detection(
            self.rek_client,
            bucket=bucket,
            video=video,
            role_arn=role_arn,
            sns_topic_arn=sns_topic_arn
        )
        if get_sqs_message_success(self.sqs_client, sqs_queue_url, start_job_id):
            response = get_label_detection_results(self.rek_client, start_job_id)

        return response


class SegmentVideoDetectProcessor(AbstractVideoDetectProcessor):
    """ Segment Detection for Videos using AWS """
    def __init__(self):
        super(SegmentVideoDetectProcessor, self).__init__()

    def start_detection_analysis(self, clip_tracker, output_dir, output_path, role_arn, bucket, \
                                 video, sns_topic_arn, sqs_queue_url):
        """
        Start Segment Detection Analysis

        Args:
            role_arn: (str) AWS Role ARN
            bucket: (str) Bucket name only (i.e. "zorroa-dev" in "gs://zorroa-dev")
            video: (str) video name without extension ("video" instead of "video.mp4")
            sns_topic_arn: (str) SNS Topic ARN
            sqs_queue_url: (str) SQS Queue URL

        Returns:
            (dict) Label Detection Results
        """
        start_job_id = start_segment_detection(
            self.rek_client,
            bucket=bucket,
            video=video,
            role_arn=role_arn,
            sns_topic_arn=sns_topic_arn
        )
        if get_sqs_message_success(self.sqs_client, sqs_queue_url, start_job_id):
            clip_tracker = self.get_segment_detection_results(
                clip_tracker=clip_tracker,
                rek_client=self.rek_client,
                start_job_id=start_job_id,
                output_dir=output_dir,
                output_path=output_path
            )

        return clip_tracker

    def get_segment_detection_results(self, clip_tracker, rek_client, start_job_id, output_dir,
                                      output_path, max_results=10):
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
                    print('\tConfidence: ' + str(segment['TechnicalCueSegment']['Confidence']))
                    print('\tType: ' + segment['TechnicalCueSegment']['Type'])

                    segment_type = segment['TechnicalCueSegment']['Type']
                    start_time = segment['StartTimestampMillis'] / 1000

                    video.extract_thumbnail_from_video(output_dir, output_path, start_time)
                    clip_tracker.append(start_time, [segment_type])

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

        return clip_tracker
