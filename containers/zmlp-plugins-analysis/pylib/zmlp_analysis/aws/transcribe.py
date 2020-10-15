from pathlib import Path
import requests

import boto3
from botocore.exceptions import ClientError

from zmlpsdk import file_storage
from zmlpsdk.analysis import ContentDetectionAnalysis
from zmlpsdk.video import WebvttBuilder
from zmlp.entity import TimelineBuilder
from zmlp_analysis.google.cloud_timeline import save_timeline
from zmlp_analysis.aws.util import TranscribeCompleteWaiter, AWSClient
from zmlp_analysis.utils.audio import AudioProcessor


class AmazonTranscribeProcessor(AudioProcessor):
    """ AWS Transcribe Processor for transcribing videos"""
    namespace = 'aws-transcribe'

    def __init__(self):
        super(AmazonTranscribeProcessor, self).__init__()
        self.transcribe_client = None
        self.s3_resource = None

    def init(self):
        aws_client = AWSClient()
        self.transcribe_client = aws_client.get_aws_client(
            service_type=boto3.client,
            service='transcribe'
        )
        self.s3_resource = aws_client.get_aws_client(
            service_type=boto3.resource,
            service='s3'
        )

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id

        if asset.get_attr('media.length') > self.max_length_sec:
            self.logger.warning(
                'Skipping, video is longer than {} seconds.'.format(self.max_length_sec))
            return

        local_audio = file_storage.localize_file(asset)
        local_filename = Path(local_audio).name  # filename with extension
        if not self.has_audio(local_audio):
            self.logger.warning('Skipping, video has no audio.')
            return

        # create temporary s3 bucket
        bucket = self.create_bucket(bucket_name=asset_id)
        # upload to s3
        bucket.upload_file(local_audio, local_filename)
        # get audio s3 uri
        audio_uri = f's3://{bucket.name}/{local_filename}'
        # run transcribe
        audio_result = self.recognize_speech(audio_uri, asset_id)

        if audio_result['results']:
            self.set_analysis(asset, audio_result)
            self.save_raw_result(asset, audio_result)
            self.save_timelines(asset, audio_result)

            # delete bucket and jobs
            try:
                self.transcribe_client.delete_transcription_job(TranscriptionJobName=asset_id)
            except ClientError:
                pass
            bucket.objects.delete()
            bucket.delete()

    def create_bucket(self, bucket_name=''):
        """ Create an S3 bucket

        Args:
            bucket_name: bucket name

        Returns:
            s3.Bucket object
        """
        return self.s3_resource.create_bucket(
            Bucket=bucket_name,
            CreateBucketConfiguration={
                'LocationConstraint': self.transcribe_client.meta.region_name
            }
        )

    def save_webvtt(self, asset, audio_result):
        """
        Create a webvtt file for speech to text.

        Args:
            asset (Asset): The asset to register the file to.
            audio_result (obj): The speech to text result.

        Returns:
            StoredFile
        """
        with WebvttBuilder() as webvtt:
            for r in audio_result['results']['items']:
                if r['type'] != 'punctuation':
                    start_time = r['start_time']
                    end_time = r['end_time']
                    content = sorted(r['alternatives'], key=lambda i: i['confidence'], reverse=True)
                    webvtt.append(start_time, end_time, content[0]['content'])

        self.logger.info(f'Saving speech-to-text data from {webvtt.path}')
        sf = file_storage.assets.store_file(webvtt.path, asset,
                                            'captions',
                                            'aws-transcribe.vtt')
        return webvtt.path, sf

    def save_transcribe_timeline(self, asset, audio_result):
        """ Save the results of Transcribe to a timeline.

        Args:
            asset (Asset): The asset to register the file to.
            audio_result (obj): The speech to text result.

        Returns:
            Timeline: The generated timeline.
        """
        timeline = TimelineBuilder(asset, "aws-transcribe")

        for r in audio_result['results']['items']:
            if r['type'] != 'punctuation':
                start_time = r['start_time']
                end_time = r['end_time']
                best_result = sorted(r['alternatives'], key=lambda i: i['confidence'], reverse=True)

                timeline.add_clip(
                    "Detected Text",
                    start_time,
                    end_time,
                    best_result[0]['content'],
                    best_result[0]['confidence'])

        save_timeline(timeline)
        return timeline

    def save_timelines(self, asset, audio_result):
        self.save_webvtt(asset, audio_result)
        self.save_transcribe_timeline(asset, audio_result)

    def save_raw_result(self, asset, audio_result):
        transcript = audio_result['results']['transcripts'][0]['transcript']
        file_storage.assets.store_blob(transcript.encode(),
                                       asset,
                                       'aws',
                                       'aws-transcribe.dat')

    def set_analysis(self, asset, audio_result):
        """ The speech to text results come with multiple possibilities per segment, we only keep
        the highest confidence.

        Args:
            asset (Asset): The asset to register the file to.
            audio_result(dict): transcribed audio result

        Returns:
            None
        """
        transcript = ''
        analysis = ContentDetectionAnalysis()

        for r in audio_result['results']['items']:
            sorted_results = sorted(r['alternatives'], key=lambda i: i['confidence'],
                                    reverse=True)
            transcript += "{}{}".format(
                "" if r['type'] == 'punctuation' else " ",  # make clean for punctuations
                sorted_results[0]['content']
            )
        analysis.add_content(transcript.strip())
        asset.add_analysis(self.namespace, analysis)

    def recognize_speech(self, audio_uri, job_name):
        """
        Call Amazon Transcribe and wait for the result.

        Args:
            audio_uri (str): The URI to the audio dump.
            job_name (str): job name

        Returns:
            (dict) AWS Transcribe response
        """
        self.start_job(job_name, audio_uri)
        transcribe_waiter = TranscribeCompleteWaiter(self.transcribe_client)
        transcribe_waiter.wait(job_name)
        job = self.get_job(job_name)
        transcript_simple = requests.get(job['Transcript']['TranscriptFileUri']).json()

        return transcript_simple

    def start_job(
            self, job_name, media_uri, media_format='mp4', language_code='en-US'):
        """

        Args:
            job_name (str): The name of the transcription job. This must be unique for your AWS
            account.
            media_uri (str): The URI where the audio file is stored. This is typically in an
            Amazon S3 bucket
            media_format (str): The format of the audio file. For example, mp4 or wav.
            language_code (str): The language code of the audio file. For example, en-US or ja-JP

        Returns:
            (dict) Data about the job
        """
        try:
            job_args = {
                'TranscriptionJobName': job_name,
                'Media': {'MediaFileUri': media_uri},
                'MediaFormat': media_format,
                'LanguageCode': language_code}
            response = self.transcribe_client.start_transcription_job(**job_args)
            job = response['TranscriptionJob']
            self.logger.debug("Started transcription job %s.", job_name)
        except ClientError:
            self.logger.exception("Couldn't start transcription job %s.", job_name)
            raise
        else:
            return job

    def get_job(self, job_name):
        """ Gets details about a transcription job.

        Args:
            job_name (str): The name of the job to retrieve.

        Returns:
            (dict) The retrieved transcription job.
        """
        try:
            response = self.transcribe_client.get_transcription_job(TranscriptionJobName=job_name)
            job = response['TranscriptionJob']
            self.logger.debug("Got job %s.", job['TranscriptionJobName'])
        except ClientError:
            self.logger.exception("Couldn't get job %s.", job_name)
            raise
        else:
            return job
