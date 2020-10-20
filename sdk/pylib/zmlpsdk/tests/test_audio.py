from unittest import TestCase

import zmlpsdk.audio as audio
from zmlpsdk.testing import zorroa_test_path


class AudioFunctionTests(TestCase):

    def test_extract_audio_file(self):
        path = audio.extract_audio_file(zorroa_test_path('video/ted_talk.mp4'))
        assert path.endswith('.flac')

    def test_has_audio_channel(self):
        assert audio.has_audio_channel(zorroa_test_path('video/ted_talk.mp4'))
        assert not audio.has_audio_channel(zorroa_test_path('video/dc.webm'))
