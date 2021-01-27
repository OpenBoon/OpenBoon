# Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
# PDX-License-Identifier: MIT-0 (For details,
# see https://github.com/awsdocs/amazon-rekognition-developer-guide/blob/master/LICENSE-SAMPLECODE.)

import os

from zmlp_analysis.aws.util import AwsEnv
from zmlp_analysis.aws.video import util
from zmlp_analysis.utils.prechecks import Prechecks
from zmlpsdk import AssetProcessor, Argument, FileTypes, ZmlpEnv, file_storage, proxy, clips, video
from zmlpsdk.analysis import LabelDetectionAnalysis

__all__ = [
    'RekognitionLabelDetection',
    'RekognitionCelebrityDetection',
    'RekognitionUnsafeDetection',
    'RekognitionPeoplePathingDetection',
    'EndCreditsVideoDetectProcessor',
    'BlackFramesVideoDetectProcessor',
    # Need changes to these.
    'RekognitionTextDetection',
    'RekognitionFaceDetection',
]


class AbstractVideoDetectProcessor(AssetProcessor):
    """ AWS Rekognition for Video Detection"""
    namespace = 'aws-video-detection'
    file_types = FileTypes.videos

    def __init__(self, detector_func=None, reactor=None):
        super(AbstractVideoDetectProcessor, self).__init__()
        self.add_arg(Argument('debug', 'bool', default=False))
        self.detector_func = detector_func
        self.reactor = reactor

        self.rek_client = None
        self.s3_client = None
        self.sqs_client = None
        self.sns_client = None

        self.roleArn = None
        self.sns_topic_arn = None
        self.sqs_queue_url = None

        self.jobId = None
        self.bucket = None
        self.video = None

    def init(self):
        self.rek_client = AwsEnv.rekognition()
        self.s3_client = AwsEnv.s3()
        self.sqs_client = AwsEnv.general_aws_client(service='sqs')
        self.sns_client = AwsEnv.general_aws_client(service='sns')

        self.roleArn = AwsEnv.get_rekognition_role_arn()
        self.sns_topic_arn = AwsEnv.get_sns_topic_arn()
        self.sqs_queue_url = AwsEnv.get_sqs_queue_url()

        self.jobId = ''
        self.bucket = ''
        self.video = ''

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        final_time = asset.get_attr('media.length')

        if not Prechecks.is_valid_video_length(asset):
            return

        video_proxy = proxy.get_video_proxy(asset)

        if not video_proxy:
            self.logger.warning(f'No video could be found for {asset_id}')
            return

        local_path = file_storage.localize_file(video_proxy)
        clip_tracker = clips.ClipTracker(asset, self.namespace)

        ext = os.path.splitext(local_path)[1]
        bucket_file = f'{ZmlpEnv.get_project_id()}/video/{asset_id}{ext}'
        bucket_name = AwsEnv.get_bucket_name()

        # upload to s3
        self.s3_client.upload_file(local_path, bucket_name, bucket_file)

        analysis = LabelDetectionAnalysis(collapse_labels=True)
        attribs = set()

        start_job_id = self.start_detection_analysis(
            role_arn=self.roleArn,
            bucket=bucket_name,
            video=bucket_file,
            sns_topic_arn=self.sns_topic_arn,
            sqs_queue_url=self.sqs_queue_url,
            func=self.detector_func
        )

        self.logger.info(f'Waiting on sqs for job {start_job_id} asset "{asset_id}"')
        if util.get_sqs_message_success(self.sqs_client, self.sqs_queue_url, start_job_id):
            clip_tracker, attribs = self.get_detection_results(
                clip_tracker=clip_tracker,
                rek_client=self.rek_client,
                start_job_id=start_job_id,
                local_video_path=local_path,
            )

        if attribs:
            [analysis.add_label_and_score(ls[0], ls[1]) for ls in attribs]
            asset.add_analysis(self.namespace, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        video.save_timeline(asset, timeline)

    def start_detection_analysis(self, role_arn, bucket, video, sns_topic_arn, sqs_queue_url, func):
        """
        Start Detection Analysis

        Args:
            role_arn: (str) AWS Role ARN
            bucket: (str) Bucket name only (i.e. "zorroa-dev" in "gs://zorroa-dev")
            video: (str) video name without extension ("video" instead of "video.mp4")
            sns_topic_arn: (str) SNS Topic ARN
            sqs_queue_url: (str) SQS Queue URL
            func: (str) type of detection to run (label, face, text, segment)

        Returns:
            (dict) Label Detection Results
        """
        return util.start_detection(
            self.rek_client,
            bucket=bucket,
            video=video,
            role_arn=role_arn,
            sns_topic_arn=sns_topic_arn,
            func=func
        )

    def get_detection_results(self, clip_tracker, rek_client, start_job_id, local_video_path,
                              max_results=10):
        """
        Get detection results

        Args:
            clip_tracker: ClipTracker for building Timeline
            rek_client: AWS Rekog Client
            start_job_id: (str) Job ID
            local_video_path: (str) locally created video file
            max_results: (int) maximum results to get, default 10

        Returns:
            (ClipTracker) built clip tracker clips for timeline building
        """
        raise NotImplementedError


class RekognitionLabelDetection(AbstractVideoDetectProcessor):
    """ Label Detection for Videos using AWS Rekognition """
    namespace = 'aws-label-detection'

    def __init__(self):
        super(RekognitionLabelDetection, self).__init__(detector_func='start_label_detection')

    def get_detection_results(self, clip_tracker, rek_client, start_job_id, local_video_path,
                              max_results=10):
        """
        Get detection results

        Args:
            clip_tracker: ClipTracker for building Timeline
            rek_client: AWS Rekog Client
            start_job_id: (str) Job ID
            local_video_path: (str) locally created video file
            max_results: (int) maximum results to get, default 10

        Returns:
            (ClipTracker) built clip tracker clips for timeline building
        """
        attribs = set()
        pagination_token = ''
        finished = False

        while not finished:
            response = rek_client.get_label_detection(JobId=start_job_id,
                                                      MaxResults=max_results,
                                                      NextToken=pagination_token,
                                                      SortBy='TIMESTAMP')
            for labelDetection in response['Labels']:
                label = labelDetection['Label']
                name = label['Name']
                confidence = label['Confidence']
                start_time = labelDetection['Timestamp'] / 1000.0  # ms to s

                attribs.add((name, confidence))
                clip_tracker.append(start_time, [name])

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

        return clip_tracker, attribs


class RekognitionTextDetection(AbstractVideoDetectProcessor):
    """ Text Detection for Videos using AWS Rekognition """
    namespace = 'aws-text-detection'

    def __init__(self):
        super(RekognitionTextDetection, self).__init__(detector_func='start_text_detection')

    def get_detection_results(self, clip_tracker, rek_client, start_job_id, local_video_path,
                              max_results=10):
        """
        Get detection results

        Args:
            clip_tracker: ClipTracker for building Timeline
            rek_client: AWS Rekog Client
            start_job_id: (str) Job ID
            local_video_path: (str) locally created video file
            max_results: (int) maximum results to get, default 10

        Returns:
            (ClipTracker) built clip tracker clips for timeline building
        """
        attribs = set()
        pagination_token = ''
        finished = False

        while not finished:
            response = rek_client.get_text_detection(
                JobId=start_job_id,
                MaxResults=max_results,
                NextToken=pagination_token
            )

            for textDetection in response['TextDetections']:
                text = textDetection['TextDetection']
                detected_text = text['DetectedText']
                confidence = text['Confidence']
                start_time = textDetection['Timestamp'] / 1000.0  # ms to s

                attribs.add((detected_text, confidence))
                clip_tracker.append(start_time, [detected_text])

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

        return clip_tracker, attribs


class RekognitionFaceDetection(AbstractVideoDetectProcessor):
    """ Face Detection for Videos using AWS Rekognition """
    namespace = 'aws-face-detection'

    def __init__(self):
        super(RekognitionFaceDetection, self).__init__(detector_func='start_face_detection')

    def get_detection_results(self, clip_tracker, rek_client, start_job_id, local_video_path,
                              max_results=10):
        """
        Get detection results

        Args:
            clip_tracker: ClipTracker for building Timeline
            rek_client: AWS Rekog Client
            start_job_id: (str) Job ID
            local_video_path: (str) locally created video file
            max_results: (int) maximum results to get, default 10

        Returns:
            (ClipTracker) built clip tracker clips for timeline building
        """
        attribs = set()
        pagination_token = ''
        finished = False
        counter = 0

        while not finished:
            response = rek_client.get_face_detection(
                JobId=start_job_id,
                MaxResults=max_results,
                NextToken=pagination_token
            )

            for i, faceDetection in enumerate(response['Faces'], counter):
                face = faceDetection['Face']
                confidence = face['Confidence']
                start_time = faceDetection['Timestamp'] / 1000.0  # ms to s

                attribs.add((f"face{i}", confidence))
                clip_tracker.append(start_time, [f"face{i}"])
            counter = i

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

        return clip_tracker, attribs


class RekognitionUnsafeDetection(AbstractVideoDetectProcessor):
    """ Content Moderation Detection for Videos using AWS Rekognition """
    namespace = 'aws-unsafe-detection'

    def __init__(self):
        super(RekognitionUnsafeDetection, self).__init__(detector_func='start_content_moderation')

    def get_detection_results(self, clip_tracker, rek_client, start_job_id, local_video_path,
                              max_results=10):
        """
        Get detection results

        Args:
            clip_tracker: ClipTracker for building Timeline
            rek_client: AWS Rekog Client
            start_job_id: (str) Job ID
            local_video_path: (str) locally created video file
            max_results: (int) maximum results to get, default 10

        Returns:
            (ClipTracker) built clip tracker clips for timeline building
        """
        attribs = set()
        pagination_token = ''
        finished = False

        while not finished:
            response = rek_client.get_content_moderation(
                JobId=start_job_id,
                MaxResults=max_results,
                NextToken=pagination_token
            )

            for contentModerationDetection in response['ModerationLabels']:
                content = contentModerationDetection['ModerationLabel']
                name = content['Name']
                confidence = content['Confidence']
                start_time = contentModerationDetection['Timestamp'] / 1000.0  # ms to s

                attribs.add((name, confidence))
                clip_tracker.append(start_time, {name: confidence})

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

        return clip_tracker, attribs


class RekognitionCelebrityDetection(AbstractVideoDetectProcessor):
    """ Celebrity Detection for Videos using AWS Rekognition """
    namespace = 'aws-celebrity-detection'

    def __init__(self):
        super(RekognitionCelebrityDetection, self).__init__(
            detector_func='start_celebrity_recognition')

    def get_detection_results(self, clip_tracker, rek_client, start_job_id, local_video_path,
                              max_results=10):
        """
        Get detection results

        Args:
            clip_tracker: ClipTracker for building Timeline
            rek_client: AWS Rekog Client
            start_job_id: (str) Job ID
            local_video_path: (str) locally created video file
            max_results: (int) maximum results to get, default 10

        Returns:
            (ClipTracker) built clip tracker clips for timeline building
        """
        attribs = set()
        pagination_token = ''
        finished = False

        while not finished:
            response = rek_client.get_celebrity_recognition(
                JobId=start_job_id,
                MaxResults=max_results,
                NextToken=pagination_token
            )

            for celebrityRecognition in response['Celebrities']:
                content = celebrityRecognition['Celebrity']
                name = content['Name']
                confidence = content['Confidence']
                start_time = celebrityRecognition['Timestamp'] / 1000.0  # ms to s

                attribs.add((name, confidence))
                clip_tracker.append(start_time, {name: confidence})

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

        return clip_tracker, attribs


class RekognitionPeoplePathingDetection(AbstractVideoDetectProcessor):
    """ People Tracking for Videos using AWS Rekognition """
    namespace = 'aws-person-tracking-detection'

    def __init__(self):
        super(RekognitionPeoplePathingDetection, self).__init__(
            detector_func='start_person_tracking')

    def get_detection_results(self, clip_tracker, rek_client, start_job_id, local_video_path,
                              max_results=10):
        """
        Get detection results

        Args:
            clip_tracker: ClipTracker for building Timeline
            rek_client: AWS Rekog Client
            start_job_id: (str) Job ID
            local_video_path: (str) locally created video file
            max_results: (int) maximum results to get, default 10

        Returns:
            (ClipTracker) built clip tracker clips for timeline building
        """
        attribs = set()
        pagination_token = ''
        finished = False
        counter = 0

        while not finished:
            response = rek_client.get_person_tracking(
                JobId=start_job_id,
                MaxResults=max_results,
                NextToken=pagination_token
            )

            for i, personDetection in enumerate(response['Persons'], counter):
                person = personDetection['Person']
                try:
                    face = person['Face']
                except KeyError:  # no person detected
                    continue
                confidence = face['Confidence']
                start_time = personDetection['Timestamp'] / 1000.0  # ms to s

                attribs.add((f"person{i}", confidence))
                clip_tracker.append(start_time, {f"person{i}": confidence})
            counter = i

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

        return clip_tracker, attribs


class SegmentVideoDetectProcessor(AbstractVideoDetectProcessor):
    """ Segment Detection for Videos using AWS Rekognition """
    namespace = 'aws-segment-detection'

    def __init__(self, cue=None):
        super(SegmentVideoDetectProcessor, self).__init__(detector_func='start_segment_detection')
        self.cue = cue

    def start_detection_analysis(self, role_arn, bucket, video, sns_topic_arn, sqs_queue_url, func):
        """
        Start Segment Detection Analysis

        Args:
            role_arn: (str) AWS Role ARN
            bucket: (str) Bucket name only (i.e. "zorroa-dev" in "gs://zorroa-dev")
            video: (str) video name without extension ("video" instead of "video.mp4")
            sns_topic_arn: (str) SNS Topic ARN
            sqs_queue_url: (str) SQS Queue URL
            func: (str) type of detection to run (label, face, text, segment)

        Returns:
            (dict) Label Detection Results
        """
        segment_types = ['TECHNICAL_CUE', 'SHOT']
        filters = {
            'TechnicalCueFilter': {
                'MinSegmentConfidence': 80.0
            },
            'ShotFilter': {
                'MinSegmentConfidence': 80.0
            }
        }

        return util.start_detection(
            self.rek_client,
            bucket=bucket,
            video=video,
            role_arn=role_arn,
            sns_topic_arn=sns_topic_arn,
            func=func,
            SegmentTypes=segment_types,
            Filters=filters
        )

    def get_detection_results(self, clip_tracker, rek_client, start_job_id, local_video_path,
                              max_results=10):
        """
        Get detection results

        Args:
            clip_tracker: ClipTracker for building Timeline
            rek_client: AWS Rekog Client
            start_job_id: (str) Job ID
            local_video_path: (str) locally created video file
            max_results: (int) maximum results to get, default 10

        Returns:
            (ClipTracker) built clip tracker clips for timeline building
        """
        attribs = set()
        pagination_token = ''
        finished = False

        while not finished:
            response = rek_client.get_segment_detection(JobId=start_job_id,
                                                        MaxResults=max_results,
                                                        NextToken=pagination_token)
            for segment in response['Segments']:
                if segment['Type'] == 'TECHNICAL_CUE':
                    segment_type = segment['TechnicalCueSegment']['Type']
                    if segment_type == self.cue:
                        confidence = segment['TechnicalCueSegment']['Confidence']
                        start_time = segment['StartTimestampMillis'] / 1000.0  # ms to s

                        attribs.add((segment_type, confidence))
                        clip_tracker.append(start_time, {segment_type: confidence})

            if 'NextToken' in response:
                pagination_token = response['NextToken']
            else:
                finished = True

        return clip_tracker, attribs


class BlackFramesVideoDetectProcessor(SegmentVideoDetectProcessor):
    """Black Frames Detector in a video using AWS Rekognition """
    namespace = 'aws-black-frames-detection'

    def __init__(self):
        super(BlackFramesVideoDetectProcessor, self).__init__(cue='BlackFrames')


class EndCreditsVideoDetectProcessor(SegmentVideoDetectProcessor):
    """Rolling Credits Detector in a video using AWS Rekognition """
    namespace = 'aws-credits-detection'

    def __init__(self):
        super(EndCreditsVideoDetectProcessor, self).__init__(cue='EndCredits')
