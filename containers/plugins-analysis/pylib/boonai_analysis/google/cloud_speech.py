import time

import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import speech_v1p1beta1 as speech

from boonflow import Argument, AssetProcessor, file_storage, FileTypes
from boonflow.lang import LanguageCodes
from boonflow.analysis import ContentDetectionAnalysis
from boonai_analysis.utils.prechecks import Prechecks
from boonflow.audio import has_audio_channel
from boonflow.proxy import get_audio_proxy_uri
from .cloud_timeline import save_speech_to_text_webvtt, save_speech_to_text_timeline
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

    def __init__(self):
        super(AsyncSpeechToTextProcessor, self).__init__()

        primary_lang = LanguageCodes.lang_defaults[0]
        alt_langs = LanguageCodes.lang_defaults[1:]

        self.add_arg(Argument('language', 'string', default=primary_lang,
                              toolTip=self.tool_tips['language']))
        self.add_arg(Argument('alt_languages', 'list',
                              toolTip=self.tool_tips['alt_languages'],
                              default=alt_langs))
        self.speech_client = None
        self.audio_channels = 2
        self.audio_sample_rate = 44100

    def init(self):
        self.speech_client = initialize_gcp_client(speech.SpeechClient)

    def process(self, frame):
        asset = frame.asset

        if not Prechecks.is_valid_video_length(asset):
            return

        if not has_audio_channel(file_storage.localize_file(asset)):
            self.logger.warning('Skipping, video has no audio.')
            return

        audio_uri = get_audio_proxy_uri(asset, auto_create=True)
        audio_result = self.recognize_speech(audio_uri, self.get_languages(asset))

        if audio_result.results:
            self.set_analysis(asset, audio_result)
            self.save_raw_result(asset, audio_result)
            self.save_timelines(asset, audio_result)

    def get_languages(self, asset):
        """
        Fetch the list of languages to pass to Cloud Speech

        Args:
            asset (Asset): The Asset

        Returns:
            list: The list of language code.
        """
        langs = asset.get_attr("media.languages")
        if not langs:
            langs = [self.arg_value('language')]
            alt_langs = self.arg_value('alt_languages')
            if alt_langs:
                langs += alt_langs
        return langs

    def save_timelines(self, asset, audio_result):
        save_speech_to_text_webvtt(asset, audio_result)
        save_speech_to_text_timeline(asset, audio_result)

    def save_raw_result(self, asset, audio_result):
        file_storage.assets.store_blob(audio_result._pb.SerializeToString(),
                                       asset,
                                       'gcp',
                                       'gcp-speech-to-text.dat')

    def set_analysis(self, asset, audio_result):
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

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_tries=3, max_time=3600)
    def recognize_speech(self, audio_uri, languages):
        """
        Call Google Speech2Text in Async mode and wait for the result.

        Args:
            audio_uri (str): The URI to the audio dump.
            languages (list): An array of language codes.
        Returns:
            LongRunningRecognizeResponse: Google Speech2Text response
        """
        audio = {
            'uri': audio_uri
        }

        pri_lang = languages[0]
        if len(languages) > 1:
            alt_langs = languages[1:]
        else:
            alt_langs = None

        config = speech.types.RecognitionConfig(
            encoding=speech.RecognitionConfig.AudioEncoding.FLAC,
            enable_word_time_offsets=True,
            audio_channel_count=self.audio_channels,
            sample_rate_hertz=self.audio_sample_rate,
            language_code=pri_lang,
            alternative_language_codes=alt_langs,
            max_alternatives=5)

        op = self.speech_client.long_running_recognize(config=config, audio=audio)
        while not op.done():
            self.logger.info('Waiting no google speech to text: {}'.format(audio['uri']))
            time.sleep(0.5)
        return op.result()
