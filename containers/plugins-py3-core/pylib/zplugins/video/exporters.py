import sys

import subprocess32 as subprocess
from pathlib2 import Path

from zplugins.video.importers import VideoImporter
from zsdk import Argument
from zsdk.processor import AbstractExporter
from zsdk.util import get_export_file_path


class VideoExporter(AbstractExporter):
    """Exporter responsible for exporting standard video files.

    Args:
        format(str): File format to be used for the exported video. Must be a standard
          container supported by ffmpeg that can handle the h264 codec such as mp4.
        quality(str): H264 quality preset. See valid options at
          https://trac.ffmpeg.org/wiki/Encode/H.264.
        scale(str): Resolution to scale the exported video to. This is passed directly to
          ffmpeg and examples of valid values can be found at https://trac.ffmpeg.org/wiki/Scaling.

    """
    toolTips = {
        'format': 'File format to be used for the exported video. Must be a standard ' +
                  'container supported by ffmpeg that can handle the h264 codec such as mp4.',
        'quality': 'H264 quality preset. See valid options at ' +
                   'https://trac.ffmpeg.org/wiki/Encode/H.264.',
        'scale': 'Resolution to scale the exported video to. This is passed directly ' +
                 'to ffmpeg and examples of valid values can be found at ' +
                 'https://trac.ffmpeg.org/wiki/Scaling.'
    }
    file_types = VideoImporter.file_types

    def __init__(self):
        super(VideoExporter, self).__init__()
        self.add_arg(Argument('format', 'str', default='mp4', toolTip=self.toolTips['format']))
        self.add_arg(Argument('quality', 'str', default='medium', toolTip=self.toolTips['quality']))
        self.add_arg(Argument('scale', 'str', toolTip=self.toolTips['scale']))

    def export(self, frame):
        asset = frame.asset
        scale = self.arg_value('scale')
        export_root_dir = self.export_root_dir
        source_path = self.get_source_path(asset)
        file_format = self.arg_value('format')
        destination_path = get_export_file_path('%s.%s' % (source_path.stem, file_format))
        start = asset.get_attr('media.clip.start')
        stop = asset.get_attr('media.clip.stop')
        ffmpeg_command = ['ffmpeg', '-hide_banner']

        # If the document has all the correct clip data add the options to just export
        # the clip and update the destination file name.
        if start and stop:
            start_time = float(start)
            end_time = float(stop)
            duration = end_time - start_time
            ffmpeg_command.extend(['-ss', str(start_time)])
            ffmpeg_command.extend(['-t', str(duration)])
            filename = '%s_%s-%s.%s' % (source_path.stem, start, stop, file_format)
            destination_path = Path(export_root_dir, filename)

        ffmpeg_command.extend(['-i', str(source_path)])
        ffmpeg_command.extend(['-crf', '18',
                               '-f', file_format,
                               '-vcodec', 'libx264',
                               '-preset', self.arg_value('quality'),
                               '-profile:v', 'main',
                               '-strict', '-2',
                               '-acodec', 'aac'])
        if scale:
            scale_operator = ('scale={scale}:force_original_aspect_ratio=decrease,'
                              'pad={scale}:(ow-iw)/2:(oh-ih)/2'.format(scale=scale))
            ffmpeg_command.extend(['-vf', scale_operator])
        ffmpeg_command.append(str(destination_path))
        self.logger.info(ffmpeg_command)
        subprocess.check_call(ffmpeg_command, stdout=sys.stdout, stderr=sys.stderr)
        self.set_exported_metadata(destination_path, asset)
