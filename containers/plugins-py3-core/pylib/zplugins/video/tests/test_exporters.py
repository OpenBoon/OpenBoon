from pathlib2 import Path

from zsdk import Frame, Asset
from zsdk.testing import PluginUnitTestCase, zorroa_test_data
from zsdk.util import clean_export_root_dir
from zplugins.util.media import ffprobe

from zplugins.video.exporters import VideoExporter


class VideoExporterUnitTestCase(PluginUnitTestCase):
    def setUp(self):
        super(VideoExporterUnitTestCase, self).setUp()
        clean_export_root_dir()
        self.ipad_movie = zorroa_test_data('video/sample_ipad.m4v')
        self.frame = Frame(Asset(str(self.ipad_movie)))
        self.frame.asset.set_attr('media.frameRate', 29.97)

    def get_video_stream(self, path):
        ffprobe_info = ffprobe(path)
        return ffprobe_info.get('streams')[0]

    def test_default_process(self):
        processor = self.init_processor(VideoExporter())
        processor.process(self.frame)
        destination_path = Path(processor.export_root_dir, 'sample_ipad.mp4')
        assert self.frame.asset.get_attr('exported.path') == str(destination_path)
        assert destination_path.exists()
        video_stream = self.get_video_stream(destination_path)
        assert video_stream.get('codec_name') == 'h264'
        assert video_stream.get('width') == 640
        assert video_stream.get('height') == 360

    def test_process_scaling(self):
        processor = self.init_processor(VideoExporter(), {'scale': '200:100'})
        processor.process(self.frame)
        destination_path = Path(processor.export_root_dir, 'sample_ipad.mp4')
        assert destination_path.exists()
        video_stream = self.get_video_stream(destination_path)
        assert video_stream.get('codec_name') == 'h264'
        assert video_stream.get('width') == 200
        assert video_stream.get('height') == 100

    def test_process_clip(self):
        self.frame.asset.set_attr('media.clip.start', 1.5)
        self.frame.asset.set_attr('media.clip.stop', 3.0)
        processor = self.init_processor(VideoExporter())
        processor.process(self.frame)
        destination_path = Path(processor.export_root_dir, 'sample_ipad_1.5-3.0.mp4')
        assert destination_path.exists()
        video_stream = self.get_video_stream(destination_path)
        assert video_stream.get('codec_name') == 'h264'
        assert round(float(video_stream.get('duration')), 2) == 1.50
