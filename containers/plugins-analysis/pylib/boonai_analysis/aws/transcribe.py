import os
import random
import string

import requests
from botocore.exceptions import ClientError

from boonai_analysis.aws.util import TranscribeCompleteWaiter, AwsEnv
from boonai_analysis.utils.prechecks import Prechecks
from boonflow import file_storage, FileTypes, Argument, AssetProcessor
from boonflow.lang import LanguageCodes
from boonflow.env import BoonEnv
from boonflow.analysis import ContentDetectionAnalysis
from boonflow.audio import has_audio_channel
from boonflow.proxy import get_audio_proxy, get_video_proxy
from .timeline import save_transcribe_timeline, save_transcribe_webvtt, save_raw_transcribe_result


class AmazonTranscribeProcessor(AssetProcessor):
    """ AWS Transcribe Processor for transcribing videos"""
    namespace = 'aws-transcribe'
    file_types = FileTypes.videos

    def __init__(self):
        super(AmazonTranscribeProcessor, self).__init__()
        self.add_arg(Argument('languages', 'list',
                              toolTip='List of languages to auto-detect',
                              default=LanguageCodes.lang_defaults))

        self.transcribe_client = None
        self.s3_client = None

    def init(self):
        self.transcribe_client = AwsEnv.transcribe()
        self.s3_client = AwsEnv.s3()

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id

        if not Prechecks.is_valid_video_length(asset):
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
        bucket_file = f'{BoonEnv.get_project_id()}/transcribe/{asset_id}{ext}'
        bucket_name = AwsEnv.get_bucket_name()

        # upload to s3
        self.s3_client.upload_file(local_path, bucket_name, bucket_file)

        # get audio s3 uri
        s3_uri = f's3://{bucket_name}/{bucket_file}'

        job_name, audio_result = self.recognize_speech(s3_uri, asset)
        try:
            if audio_result['results']:
                self.set_analysis(asset, audio_result)
                save_raw_transcribe_result(asset, audio_result)
                save_transcribe_timeline(asset, audio_result)
                save_transcribe_webvtt(asset, audio_result)
        finally:
            self.cleanup_aws_resources(bucket_file, job_name)

    def cleanup_aws_resources(self, bucket_file, job_name):

        # delete bucket and jobs
        try:
            self.logger.info('deleting job {}'.format(job_name))
            self.transcribe_client.delete_transcription_job(TranscriptionJobName=job_name)
        except Exception:
            self.logger.exception('Failed to delete AWS transcription job.')

        try:
            self.logger.info('deleting object {} {}'.format(
                AwsEnv.get_bucket_name(),
                bucket_file
            ))
            self.s3_client.delete_object(
                Bucket=AwsEnv.get_bucket_name(),
                Key=bucket_file
            )
        except Exception:
            self.logger.exception('Failed to delete AWS audio file.')

    def set_analysis(self, asset, audio_result):
        """ The speech to text results come with multiple possibilities per segment, we only keep
        the highest confidence.

        Args:
            asset (Asset): The asset to register the file to.
            audio_result(dict): transcribed audio result

        Returns:
            None
        """
        analysis = ContentDetectionAnalysis()
        content = []
        for trans in audio_result['results']['transcripts']:
            content.append(trans['transcript'].strip())

        analysis.add_content(' '.join(content))
        asset.add_analysis(self.namespace, analysis)

    def recognize_speech(self, audio_uri, asset):
        """
        Call Amazon Transcribe and wait for the result.

        Args:
            audio_uri (str): The URI to the audio dump.
        Returns:
            (dict) AWS Transcribe response
        """
        job = self.start_job(audio_uri, asset)
        job_name = job['TranscriptionJobName']
        transcribe_waiter = TranscribeCompleteWaiter(self.transcribe_client)

        transcribe_waiter.wait(job_name)
        job = self.get_job(job_name)
        return job_name, requests.get(job['Transcript']['TranscriptFileUri']).json()

    def start_job(self, media_uri, asset):
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
                'LanguageOptions': self.get_languages(asset)
            }

            self.logger.info('Starting transcription job %s.', job_name)
            response = self.transcribe_client.start_transcription_job(**job_args)
            return response['TranscriptionJob']
        except ClientError:
            self.logger.exception('Couldn\'t start transcription job %s.', job_name)
            raise

    def get_languages(self, asset):
        """
        Get list of languages to pass to transcribe.
        Args:
            asset: (Asset): The asset which may define languagess

        Returns:
            list: A list of languagee codes.
        """
        langs = asset.get_attr('media.languages')
        if not langs:
            langs = self.arg_value('languages')
        return langs

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
            self.logger.exception('Couldn\'t get job %s.', job_name)
            raise
