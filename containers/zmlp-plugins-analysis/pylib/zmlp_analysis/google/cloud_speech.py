import io
import os
import subprocess
import tempfile

import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import speech_v1p1beta1 as speech

from zmlpsdk import Argument, AssetProcessor
from zmlpsdk.proxy import get_proxy_level_path

from .gcp_client import initialize_gcp_client


class CloudSpeechToTextProcessor(AssetProcessor):
    file_types = ['mov', 'mp4', 'mpg', 'mpeg', 'm4v', 'webm', 'ogv', 'ogg',
                  'aac', 'mp3', 'flac', 'wav']

    tool_tips = {'overwrite_existing': 'If the metadata this processor creates is already set on an'
                                       'asset, processing of this asset is skipped. (Default: '
                                       'False)',
                 'primary_language': 'A ISO 639-1 standard language code indicating the primary '
                                     'language to be expected in the given assets. (Default: '
                                     '"en-US")',
                 'alternate_languages': 'Up to 10 ISO 639-1 language codes indicating potential'
                                        'secondary languages found in the assets being processed.'}

    def __init__(self):
        super(CloudSpeechToTextProcessor, self).__init__()
        self.add_arg(Argument('overwrite_existing', 'bool', default=False,
                              toolTip=self.tool_tips['overwrite_existing']))
        self.add_arg(Argument('primary_language', 'string', default='en-US',
                              toolTip=self.tool_tips['primary_language']))
        self.add_arg(Argument('alternate_languages', 'list', default=[],
                              toolTip=self.tool_tips['alternate_languages']))
        self.speech_client = None
        self.audio_channels = 2
        self.audio_sample_rate = 44100

    def init(self):
        super(CloudSpeechToTextProcessor, self).init()
        self.speech_client = initialize_gcp_client(speech.SpeechClient)

    def process(self, frame):
        asset = frame.asset
        analysis_field = 'google.speechRecognition'
        if not asset.attr_exists("clip"):
            self.logger.warning('Skipping, this asset is not a clip.')
            return
        if not self.arg_value('overwrite_existing') and asset.get_attr('analysis.%s' %
                                                                       analysis_field):
            self.logger.warning('Skipping, this asset has already been processed.')
        audio = speech.types.RecognitionAudio(content=self._get_audio_clip_content(asset))
        attributes = self._recognize_speech(audio)
        # if no speech was recognized, attributes == None
        if attributes:
            asset.add_analysis(analysis_field, attributes)
        else:
            self.logger.info('Asset contains no discernible speech.')

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _recognize_speech(self, audio):
        config = speech.types.RecognitionConfig(
            encoding=speech.enums.RecognitionConfig.AudioEncoding.FLAC,
            audio_channel_count=self.audio_channels,
            sample_rate_hertz=self.audio_sample_rate,
            language_code=self.arg_value('primary_language'),
            alternative_language_codes=self.arg_value('alternate_languages'),
            max_alternatives=10)
        response = self.speech_client.recognize(config=config, audio=audio)
        confidence = 0
        content = ''
        language = ''
        pieces = 0
        for r in response.results:
            language = r.language_code
            best_alt_confidence = 0
            best_alt_transcript = ''
            for alt in r.alternatives:
                if alt.confidence > best_alt_confidence:
                    best_alt_confidence = alt.confidence
                    best_alt_transcript = alt.transcript
            confidence += best_alt_confidence
            content += ' ' + best_alt_transcript
            pieces += 1
        if pieces == 0:
            return None
        confidence /= pieces
        return {'language': language, 'confidence': confidence, 'content': content}

    def _get_audio_clip_content(self, asset):
        clip_start = asset.get_attr('clip.start')
        clip_length = asset.get_attr('clip.length')
        video_length = asset.get_attr('media.duration')
        seek = max(clip_start - 0.25, 0)
        duration = min(clip_length + 0.5, video_length)
        self.logger.info('Original time in & duration: {}, {}'.format(clip_start, clip_length))
        self.logger.info('Expanded time in & duration: {}, {}'.format(seek, duration))
        audio_fname = os.path.join(tempfile.gettempdir(),
                                   next(tempfile._get_candidate_names())) + ".flac"

        # Construct ffmpeg command line
        cmd_line = ['ffmpeg',
                    '-i', get_proxy_level_path(asset, 3, mimetype="video/"),
                    '-vn',
                    '-acodec', 'flac',
                    '-ar', str(self.audio_sample_rate),
                    '-ac', str(self.audio_channels),
                    '-ss', str(seek),
                    '-t', str(duration),
                    audio_fname]

        self.logger.info('Executing %s' % cmd_line)
        subprocess.check_call(cmd_line)
        with io.open(audio_fname, 'rb') as audio_file:
            return audio_file.read()
