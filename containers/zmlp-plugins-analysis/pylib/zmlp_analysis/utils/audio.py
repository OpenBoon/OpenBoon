import json
from subprocess import check_output

from zmlpsdk import Argument, AssetProcessor, FileTypes


class AudioProcessor(AssetProcessor):
    """ Perform speech to text functions on videos"""
    file_types = FileTypes.videos

    tool_tips = {
        'language': 'A ISO 639-1 standard language code indicating the primary '
                    'language to be expected in the given assets. (Default: '
                    '"en-US")',
        'alt_languages':
            'A set of alternative languages that your audio data might contain.'
    }

    namespace = 'aws-transcribe'

    max_length_sec = 120 * 60

    def __init__(self):
        super(AudioProcessor, self).__init__()
        self.add_arg(Argument('language', 'string', default='en-US',
                              toolTip=self.tool_tips['language']))
        self.add_arg(Argument('alt_languages', 'list',
                              toolTip=self.tool_tips['alt_languages']))
        # audio params
        self.audio_channels = 2
        self.audio_sample_rate = 44100

    def process(self, frame):
        """ Process speech to text"""
        raise NotImplementedError

    def save_timelines(self, asset, audio_result):
        """ Create a webvtt file for speech to text and save the results of Speech to Text to a
        timeline.

        Args:
            asset (Asset): The asset to register the file to.
            audio_result (obj): The speech to text result.

        Returns:
            None

        Examples:
            from zmlp_analysis.google.cloud_speech.py:
                def save_timelines(self, asset, audio_result):
                    save_speech_to_text_webvtt(asset, audio_result)
                    save_speech_to_text_timeline(asset, audio_result)
        """
        raise NotImplementedError

    def save_raw_result(self, asset, audio_result):
        # The speech to text results come with multiple possibilities per segment, we
        # only keep the highest confidence.
        raise NotImplementedError

    def set_analysis(self, asset, audio_result):
        """ The speech to text results come with multiple possibilities per segment, we only keep
        the highest confidence.

        Args:
            asset (Asset): The asset to register the file to.
            audio_result(dict): transcribed audio result

        Returns:
            None
        """
        raise NotImplementedError

    def has_audio(self, src_path):
        """Returns the json results of an ffprobe command as a dictionary.

        Args:
            src_path (str): Path the the media.

        Returns:
            True is media has at least one audio stream, False otherwise.
        """

        cmd = ['ffprobe',
               str(src_path),
               '-show_streams',
               '-select_streams', 'a',
               '-print_format', 'json',
               '-loglevel', 'error']

        self.logger.debug("running command: %s" % cmd)

        ffprobe_result = check_output(cmd, shell=False)
        n_streams = len(json.loads(ffprobe_result)['streams'])
        if n_streams > 0:
            return True
        else:
            return False

    def recognize_speech(self, audio_uri):
        """
        Call Amazon Transcribe and wait for the result.

        Args:
            audio_uri (str): The URI to the audio dump.

        Returns:
            API response
        """
        raise NotImplementedError
