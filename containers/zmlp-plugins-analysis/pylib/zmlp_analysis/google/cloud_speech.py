import subprocess
import tempfile
import time
from os import path

import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import speech_v1p1beta1 as speech

from zmlpsdk import Argument, AssetProcessor, file_storage, FileTypes
from zmlpsdk.analysis import ContentDetectionAnalysis
from .gcp_client import initialize_gcp_client


class AsyncSpeechToTextProcessor(AssetProcessor):
    file_types = FileTypes.videos

    tool_tips = {
        'language': 'A ISO 639-1 standard language code indicating the primary '
                    'language to be expected in the given assets. (Default: '
                    '"en-US")',
        'alt_languages':
            'A set of alternative languages that your audio data might contain.'
    }

    namespace = 'gcp-speech-to-text'

    max_length_sec = 30 * 60

    def __init__(self):
        super(AsyncSpeechToTextProcessor, self).__init__()
        self.add_arg(Argument('language', 'string', default='en-US',
                              toolTip=self.tool_tips['language']))
        self.add_arg(Argument('alt_languages', 'list',
                              toolTip=self.tool_tips['alt_languages']))
        self.speech_client = None
        self.audio_channels = 2
        self.audio_sample_rate = 44100

    def init(self):
        self.speech_client = initialize_gcp_client(speech.SpeechClient)

    def process(self, frame):
        asset = frame.asset

        # Cannot run on clips without transcoding the clip
        if asset.get_attr('clip.track') != 'full':
            self.logger.info('Skipping, cannot run processor on clips.')
            return -1

        if asset.get_attr('media.length') > self.max_length_sec:
            self.logger.warning(
                'Skipping, video is longer than {} seconds.'.format(self.max_length_sec))
            return

        audio_uri = self.get_audio_proxy_uri(asset)
        if audio_uri is None:
            self.logger.warning('Skipping, video has no audio.')
            return

        audio_result = self.recognize_speech(audio_uri)

        # The speech to text results come with multiple possibilities per segment, we
        # only keep the highest confidence.
        analysis = ContentDetectionAnalysis()
        languages = set()

        for r in audio_result.results:
            sorted_results = sorted(r.alternatives, key=lambda i: i.confidence, reverse=True)
            analysis.add_content(sorted_results[0].transcript)
            languages.add(r.language_code)

        analysis.set_attr('language', languages)
        asset.add_analysis(self.namespace, analysis)

        # This stores the raw google result in case we need it later.
        file_storage.assets.store_blob(audio_result.SerializeToString(),
                                       asset,
                                       'gcp',
                                       'speech-to-text.dat')

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_tries=3, max_time=3600)
    def recognize_speech(self, audio_uri):
        """
        Call Google Speech2Text in Async mode and wait for the result.

        Args:
            audio_uri (str): The URI to the audio dump.

        Returns:
            LongRunningRecognizeResponse: Google Speech2Text response
        """
        audio = {
            'uri': audio_uri
        }
        config = speech.types.RecognitionConfig(
            encoding=speech.enums.RecognitionConfig.AudioEncoding.FLAC,
            audio_channel_count=self.audio_channels,
            sample_rate_hertz=self.audio_sample_rate,
            language_code=self.arg_value('language'),
            alternative_language_codes=self.arg_value('alt_languages') or None,
            max_alternatives=5)

        op = self.speech_client.long_running_recognize(config=config, audio=audio)
        while not op.done():
            self.logger.info('Waiting no google speech to text: {}'.format(audio['uri']))
            time.sleep(0.5)
        return op.result()

    def get_audio_proxy_uri(self, asset):
        """
        Get a URI to the audio proxy.  We either have one already
        made or have to make it.
        Args:
            asset: (Asset): The asset to find an audio proxy for.

        Returns:
            str: A URI to an audio proxy.

        """
        audio_proxy = asset.get_files(category="audio", name="audio_proxy.flac")
        if audio_proxy:
            return file_storage.assets.get_native_uri(audio_proxy)
        else:
            audio_fname = tempfile.mkstemp(suffix=".flac", prefix="audio", )[1]
            cmd_line = ['ffmpeg',
                        '-y',
                        '-i', file_storage.localize_file(asset),
                        '-vn',
                        '-acodec', 'flac',
                        '-ar', str(self.audio_sample_rate),
                        '-ac', str(self.audio_channels),
                        audio_fname]

            self.logger.info('Executing {}'.format(" ".join(cmd_line)))
            try:
                subprocess.check_call(cmd_line)
            except subprocess.CalledProcessError:
                pass

        if not path.exists(audio_fname):
            return None

        sfile = file_storage.assets.store_file(
            audio_fname, asset, 'audio', rename='audio_proxy.flac')
        return file_storage.assets.get_native_uri(sfile)
