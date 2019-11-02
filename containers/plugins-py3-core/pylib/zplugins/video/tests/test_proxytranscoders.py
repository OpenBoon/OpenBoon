import unittest

from pathlib2 import Path

from zplugins.video.importers import VideoImporter
from zplugins.video.proxytranscoders import FFMpegProxyTranscoder
from zplugins.util.media import ffprobe
from zorroa.zsdk.testing import zorroa_test_data


class FFMpegProxyTranscoderUnitTestCase(unittest.TestCase):
    def setUp(self):
        super(FFMpegProxyTranscoderUnitTestCase, self).setUp()
        self.movie_path = zorroa_test_data('video/sample_ipad.m4v')
        processor = VideoImporter()
        self.transcoder = FFMpegProxyTranscoder(processor)

    def test_transcode_width(self):
        destination = Path('./ffmpeg_proxy_transcoder_test.mp4')
        self.transcoder.transcode(source_path=self.movie_path,
                                  destination_path=str(destination),
                                  width=200)
        assert destination.exists()
        try:
            ffprobe_info = ffprobe(destination)
            video_stream = ffprobe_info['streams'][0]
            assert video_stream['width'] == 200
            assert video_stream['height'] == 112
        finally:
            destination.unlink()

    def test_transcode_height(self):
        destination = Path('./ffmpeg_proxy_transcoder_test.mp4')
        self.transcoder.transcode(source_path=self.movie_path,
                                  destination_path=str(destination),
                                  height=112)
        assert destination.exists()
        try:
            ffprobe_info = ffprobe(destination)
            video_stream = ffprobe_info['streams'][0]
            assert video_stream['width'] == 200
            assert video_stream['height'] == 112
        finally:
            destination.unlink()

    def test_transcode_width_height(self):
        destination = Path('./ffmpeg_proxy_transcoder_test.mp4')
        if destination.exists():
            destination.unlink()
        self.transcoder.transcode(source_path=self.movie_path,
                                  destination_path=str(destination),
                                  height=100, width=100)
        assert destination.exists()
        try:
            ffprobe_info = ffprobe(destination)
            video_stream = ffprobe_info['streams'][0]
            assert video_stream['width'] == 100
            assert video_stream['height'] == 100
        finally:
            destination.unlink()
