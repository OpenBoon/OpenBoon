import os
import random
import string
import json

import requests
from botocore.exceptions import ClientError

from zmlp.entity import TimelineBuilder
from zmlp_analysis.aws.util import TranscribeCompleteWaiter, AwsEnv
from zmlp_analysis.google.cloud_timeline import save_timeline
from zmlpsdk import file_storage, FileTypes, Argument, AssetProcessor, ZmlpEnv
from zmlpsdk.analysis import ContentDetectionAnalysis
from zmlpsdk.proxy import get_audio_proxy, get_video_proxy
from zmlpsdk.audio import has_audio_channel
from zmlpsdk.video import WebvttBuilder


class AmazonTranscribeProcessor(AssetProcessor):
    """ AWS Transcribe Processor for transcribing videos"""
    namespace = 'aws-transcribe'
    file_types = FileTypes.videos

    def __init__(self):
        super(AmazonTranscribeProcessor, self).__init__()
        self.add_arg(Argument('languages', 'list',
                              toolTip="List of languages to auto-detect",
                              default=['en-US', 'en-GB', 'fr-FR', 'es-US']))

        self.transcribe_client = None
        self.s3_client = None

    def init(self):
        self.transcribe_client = AwsEnv.transcribe()
        self.s3_client = AwsEnv.s3()

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
        bucket_file = f'{ZmlpEnv.get_project_id()}/transcribe/{asset_id}{ext}'
        bucket_name = AwsEnv.get_bucket_name()

        # upload to s3
        self.s3_client.upload_file(local_path, bucket_name, bucket_file)

        # get audio s3 uri
        s3_uri = f's3://{bucket_name}/{bucket_file}'

        job_name, audio_result = self.recognize_speech(s3_uri)
        try:
            if audio_result['results']:
                self.set_analysis(asset, audio_result)
                self.save_raw_result(asset, audio_result)
                self.save_timelines(asset, audio_result)
        finally:
            self.cleanup_aws_resources(bucket_file, job_name)

    def cleanup_aws_resources(self, bucket_file, job_name):

        # delete bucket and jobs
        try:
            self.logger.info("deleting job {}".format(job_name))
            self.transcribe_client.delete_transcription_job(TranscriptionJobName=job_name)
        except Exception:
            self.logger.exception("Failed to delete AWS transcription job.")

        try:
            self.logger.info("deleting object {} {}".format(
                AwsEnv.get_bucket_name(),
                bucket_file
            ))
            self.s3_client.delete_object(
                Bucket=AwsEnv.get_bucket_name(),
                Key=bucket_file
            )
        except Exception:
            self.logger.exception("Failed to delete AWS audio file.")

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
        results = audio_result['results']
        track = 'Language {}'.format(results.get('language_code', 'en-US'))
        for r in results['items']:
            if r['type'] != 'punctuation':
                start_time = r['start_time']
                end_time = r['end_time']
                best_result = sorted(r['alternatives'], key=lambda i: i['confidence'], reverse=True)

                timeline.add_clip(
                    track,
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
        """
        Save a JSON version of the raw AWS result.

        Args:
            asset (Asset): The asset
            audio_result (dict): A transcribe result.
        """
        jstr = json.dumps(audio_result)
        file_storage.assets.store_blob(jstr.encode(),
                                       asset,
                                       'aws',
                                       'aws-transcribe.json')

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

    def recognize_speech(self, audio_uri):
        """
        Call Amazon Transcribe and wait for the result.

        Args:
            audio_uri (str): The URI to the audio dump.
        Returns:
            (dict) AWS Transcribe response
        """
        job = self.start_job(audio_uri)
        job_name = job['TranscriptionJobName']
        transcribe_waiter = TranscribeCompleteWaiter(self.transcribe_client)

        transcribe_waiter.wait(job_name)
        job = self.get_job(job_name)
        return job_name, requests.get(job['Transcript']['TranscriptFileUri']).json()

    def start_job(self, media_uri):
        """

        Args:
            media_uri (str): The URI where the audio file is stored. This is typically in an
            Amazon S3 bucket
        Returns:
            (dict) Data about the job
        """
        try:
            job_name = ''.join(random.choice(string.ascii_lowercase) for i in range(32))
            media_format = os.path.splitext(media_uri)[1][1:]
            job_args = {
                'TranscriptionJobName': job_name,
                'Media': {'MediaFileUri': media_uri},
                'MediaFormat': media_format,
                'IdentifyLanguage': True,
                'LanguageOptions': self.arg_value('languages')
            }

            self.logger.info("Starting transcription job %s.", job_name)
            response = self.transcribe_client.start_transcription_job(**job_args)
            return response['TranscriptionJob']
        except ClientError:
            self.logger.exception("Couldn't start transcription job %s.", job_name)
            raise

    def get_job(self, job_name):
        """ Gets details about a transcription job.

        Args:
            job_name (str): The name of the job to retrieve.

        Returns:
            (dict) The retrieved transcription job.
        """
        try:
            response = self.transcribe_client.get_transcription_job(TranscriptionJobName=job_name)
            return response['TranscriptionJob']
        except ClientError:
            self.logger.exception("Couldn't get job %s.", job_name)
            raise
