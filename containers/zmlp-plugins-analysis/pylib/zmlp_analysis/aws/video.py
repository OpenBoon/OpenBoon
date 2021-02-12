import json
import os
import time

from zmlp_analysis.aws.awscloud import AwsCloudResources
from zmlp_analysis.aws.util import AwsEnv
from zmlp_analysis.utils.prechecks import Prechecks
from zmlpsdk import AssetProcessor, FileTypes, ZmlpEnv, file_storage, proxy, clips
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.video import save_timeline

__all__ = [
    'RekognitionLabelDetection',
    'RekognitionCelebrityDetection',
    'RekognitionUnsafeDetection',
    'AbstractVideoDetectProcessor'
]


class AbstractVideoDetectProcessor(AssetProcessor):
    """ AWS Rekognition for Video Detection"""
    namespace = 'aws-video-detection'
    file_types = FileTypes.videos
    use_threads = False

    def __init__(self, detector_func=None):
        super(AbstractVideoDetectProcessor, self).__init__()
        self.detector_func = detector_func
        self.aws = None
        self.rek_client = None
        self.s3_client = None
        self.role_arn = None
        self.jobs = {}

    def init(self):
        self.aws = AwsCloudResources()
        self.rek_client = AwsEnv.rekognition()
        self.s3_client = AwsEnv.s3()
        self.role_arn = AwsEnv.get_rekognition_role_arn()

    def teardown(self):
        if self.aws:
            self.aws.teardown()

    def preprocess(self, assets):
        """
        Iterate all of the assets and send them over to Rekognition.  Once

        Args:
            assets (list): A list of assets.
        """
        bucket_name = AwsEnv.get_bucket_name()

        for asset in assets:
            asset_id = asset.id
            if not Prechecks.is_valid_video_length(asset):
                continue

            video_proxy = proxy.get_video_proxy(asset)
            if not video_proxy:
                self.logger.warning(f'No video could be found for {asset_id}')
                continue

            # Make a bucket path
            local_path = file_storage.localize_file(video_proxy)
            ext = os.path.splitext(local_path)[1]
            bucket_file = f'{ZmlpEnv.get_project_id()}/video/{asset_id}{ext}'

            # Save file to bucket.
            self.s3_client.upload_file(local_path, bucket_name, bucket_file)
            self.logger.debug("uploading file: {} {} {}".format(
                local_path, bucket_name, bucket_file))

            job = self.start_detection_analysis(
                asset_id,
                bucket_file,
                self.detector_func
            )
            self.jobs[asset_id] = job

        # Now we wait on all the jobs to complete.
        job_count = len(self.jobs)
        job_completed_count = 0

        self.logger.info(f'Waiting for {job_count} assets submitted to AWS')

        # While our job count greater than completed job count, then
        # we wait for more jobs to complete.
        # we'll probably have to time this out somehow
        sleep_counter = 0
        while job_count > job_completed_count:

            rsp = self.aws.sqs.receive_message(QueueUrl=self.aws.queue_url,
                                               MessageAttributeNames=['ALL'],
                                               WaitTimeSeconds=5,
                                               MaxNumberOfMessages=10)
            if 'Messages' not in rsp:
                time.sleep(2)
                sleep_counter += 1
                if sleep_counter >= 30:
                    self.logger.info(f'Waiting on AWS for {job_count} assets')
                    sleep_counter = 0
                continue

            for message in rsp['Messages']:
                body = json.loads(message['Body'])
                rek = json.loads(body['Message'])
                job_tag = rek.get('JobTag', "unknown job tag")

                self.logger.info(f'Rekognition completed Asset "{job_tag}"')
                job_completed_count += 1

                # Assuming we could care less about the content of the message
                # Pretty sure we just need to know its processed.
                self.aws.sqs.delete_message(QueueUrl=self.aws.queue_url,
                                            ReceiptHandle=message['ReceiptHandle'])

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id

        # If the asset id isn't in our jobs dict created by preprocess
        # Then we don't process it.  This means the asset wasn't processed.
        if asset_id not in self.jobs:
            return

        final_time = asset.get_attr('media.length')
        clip_tracker = clips.ClipTracker(asset, self.namespace)
        analysis = LabelDetectionAnalysis(collapse_labels=True)

        self.handle_detection_results(clip_tracker, analysis, self.jobs[asset_id])

        asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(asset, timeline)

    def start_detection_analysis(self, asset_id, video, func):
        """
        Start Detection Analysis

        Args:
            asset_id: (str) The Asset ID.
            video: (str) the S3  URI to the video
            func: (str) type of detection to run (label, face, text, segment)

        Returns:
            str: The job ID.
        """
        bucket = AwsEnv.get_bucket_name()
        rsp = getattr(self.rek_client, func)(
            Video={'S3Object': {'Bucket': bucket, 'Name': video}},
            NotificationChannel={'RoleArn': self.role_arn, 'SNSTopicArn': self.aws.topic_arn},
            JobTag="{}:{}".format(asset_id, func)
        )
        start_job_id = rsp['JobId']
        self.logger.info('Start Job Id: ' + start_job_id)
        return start_job_id

    def handle_detection_results(self, clip_tracker, analysis, job_id):
        """
        Get detection results

        Args:
            clip_tracker: ClipTracker for building Timeline
            analysis: A LabelDetectionAnalysis instance
            job_id: (str) Job ID

        Returns:
            (ClipTracker) built clip tracker clips for timeline building
        """
        raise NotImplementedError

    def get_detection_results(self, job_id, page_token):
        """
        Gets 1 page of detection results.

        Returns:
            list: A list of Rekognition detection results.
        """
        raise NotImplementedError


class RekognitionLabelDetection(AbstractVideoDetectProcessor):
    """ Label Detection for Videos using AWS Rekognition """
    namespace = 'aws-label-detection'

    def __init__(self):
        super(RekognitionLabelDetection, self).__init__(detector_func='start_label_detection')

    def get_detection_results(self, job_id, page_token):
        return self.rek_client.get_label_detection(JobId=job_id,
                                                   MaxResults=50,
                                                   NextToken=page_token,
                                                   SortBy='TIMESTAMP')

    def handle_detection_results(self, clip_tracker, analysis, start_job_id):

        pagination_token = ''
        finished = False

        current_time = -1
        labels = {}
        while not finished:
            response = self.get_detection_results(start_job_id, pagination_token)

            for detection in response['Labels']:

                # We have to build up a label/conf dict for the current time
                # Then add that ass a single batch to clip tracker.
                if detection['Timestamp'] != current_time:
                    if labels:
                        clip_tracker.append(current_time / 1000.0, labels)

                    # Reset
                    current_time = detection['Timestamp']
                    labels = {}

                label = detection['Label']
                name = label['Name']
                confidence = label['Confidence']

                labels[name] = confidence
                analysis.add_label_and_score(name, confidence)

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

        # append the final labels.
        if labels:
            clip_tracker.append(current_time / 1000.0, labels)


class RekognitionUnsafeDetection(AbstractVideoDetectProcessor):
    """ Content Moderation Detection for Videos using AWS Rekognition """
    namespace = 'aws-unsafe-detection'

    def __init__(self):
        super(RekognitionUnsafeDetection, self).__init__(detector_func='start_content_moderation')

    def get_detection_results(self, job_id, page_token):
        return self.rek_client.get_content_moderation(
            JobId=job_id,
            MaxResults=50,
            NextToken=page_token
        )

    def handle_detection_results(self, clip_tracker, analysis, job_id):
        print("handing detection results")
        pagination_token = ''
        finished = False

        current_time = -1
        labels = {}

        while not finished:
            response = self.get_detection_results(job_id, pagination_token)

            for detection in response['ModerationLabels']:

                if detection['Timestamp'] != current_time:
                    if labels:
                        clip_tracker.append(current_time / 1000.0, labels)

                    # Reset
                    current_time = detection['Timestamp']
                    labels = {}

                content = detection['ModerationLabel']
                name = content['Name']
                confidence = content['Confidence']

                labels[name] = confidence
                analysis.add_label_and_score(name, confidence)

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

        # append the final labels.
        if labels:
            clip_tracker.append(current_time / 1000.0, labels)


class RekognitionCelebrityDetection(AbstractVideoDetectProcessor):
    """ Celebrity Detection for Videos using AWS Rekognition """
    namespace = 'aws-celebrity-detection'

    def __init__(self):
        super(RekognitionCelebrityDetection, self).__init__(
            detector_func='start_celebrity_recognition')

    def get_detection_results(self, job_id, page_token):
        return self.rek_client.get_celebrity_recognition(
            JobId=job_id,
            MaxResults=50,
            NextToken=page_token)

    def handle_detection_results(self, clip_tracker, analysis, job_id):

        pagination_token = ''
        finished = False

        current_time = -1
        labels = {}

        while not finished:
            response = self.get_detection_results(job_id, pagination_token)

            for detection in response['Celebrities']:

                if detection['Timestamp'] != current_time:
                    if labels:
                        clip_tracker.append(current_time / 1000.0, labels)

                    # Reset
                    current_time = detection['Timestamp']
                    labels = {}

                content = detection['Celebrity']
                name = content['Name']
                confidence = content['Confidence']

                labels[name] = confidence
                analysis.add_label_and_score(name, confidence)

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

        if labels:
            clip_tracker.append(current_time / 1000.0, labels)
