# Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
# PDX-License-Identifier: MIT-0 (For details, see https://github.com/awsdocs/amazon-rekognition-developer-guide/blob/master/LICENSE-SAMPLECODE.)

import os

from zmlp_analysis.aws.util import AwsEnv
from zmlp_analysis.aws.videos.util import *
from zmlpsdk import file_storage, FileTypes, AssetProcessor, ZmlpEnv
from zmlpsdk.proxy import get_audio_proxy, get_video_proxy
from zmlpsdk.audio import has_audio_channel

MAX_LENGTH_SEC = 120


class VideoDetect(AssetProcessor):
    """ AWS Rekognition for Video Label Detection"""
    namespace = 'aws-video-label-detection'
    file_types = FileTypes.videos

    def __init__(self):
        super(VideoDetect, self).__init__()

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

        if asset.get_attr('media.length') > 120:
            self.logger.warning(
                'Skipping, video is longer than {} seconds.'.format(self.max_length_sec))
            return

        # Look for a .flac audio proxy first.  If one doesn't exist we can
        # use the MP4, but it's larger and thus slower.
        audio_proxy = get_audio_proxy(asset, False)
        if not audio_proxy:
            # We can use the video proxy.
            audio_proxy = get_video_proxy(asset)

        if not audio_proxy:
            self.logger.warning(f'No audio could be found for {asset_id}')
            return

        local_path = file_storage.localize_file(audio_proxy)
        if not has_audio_channel(local_path):
            self.logger.warning(f'No audio channel could be found for {asset_id}')
            return

        ext = os.path.splitext(local_path)[1]
        bucket_file = f'{ZmlpEnv.get_project_id()}/video/{asset_id}{ext}'
        bucket_name = AwsEnv.get_bucket_name()

        # upload to s3
        self.s3_client.upload_file(local_path, bucket_name, bucket_file)

        # get audio s3 uri
        s3_uri = f's3://{bucket_name}/{bucket_file}'

        response = self.start_detection(self.roleArn, bucket_name, bucket_file, asset_id)

    def start_detection(self, role_arn, bucket, video, asset_id):
        """
        Start Detection Analysis

        Args:
            role_arn: (str) AWS Role ARN
            bucket: (str) Bucket name only (i.e. "zorroa-dev" in "gs://zorroa-dev")
            video: (str) video name without extension ("video" instead of "video.mp4")
            asset_id: (str) Asset ID

        Returns:
            (dict) Label Detection Results
        """
        prepend_name = "AmazonRekognition"

        sns_topic_arn, sqs_queue_url = create_topic_and_queue(
            self.sns_client,
            self.sqs_client,
            topic_name=f"{prepend_name}-{asset_id}-topic",
            queue_name=f"{prepend_name}-{asset_id}-queue"
        )

        try:
            start_job_id = start_label_detection(
                self.rek_client,
                bucket=bucket,
                video=video,
                role_arn=role_arn,
                sns_topic_arn=sns_topic_arn
            )
            if get_sqs_message_success(self.sqs_client, sqs_queue_url, start_job_id):
                response = get_label_detection_results(self.rek_client, start_job_id)
        finally:
            self.sqs_client.delete_queue(QueueUrl=sqs_queue_url)
            self.sns_client.delete_topic(TopicArn=sns_topic_arn)

        return response
