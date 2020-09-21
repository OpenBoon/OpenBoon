import subprocess
import tempfile

from zmlpsdk import AssetProcessor, StopWatch
from zmlpsdk.storage import file_storage
from ..util.media import store_media_proxy, MediaInfo


class VideoProxyProcessor(AssetProcessor):
    """
    Makes a proxy video for a full video file.  Clip assets will reference
    this video file.
    """
    file_types = ['mov', 'mp4', 'mpg', 'mpeg', 'm4v', 'webm', 'ogv', 'ogg', 'mxf']

    # Always transcode this media
    always_transcode = ['webm', 'ogv', 'ogg', 'mxf']

    def __init__(self):
        super(VideoProxyProcessor, self).__init__()

    def process(self, frame):
        asset = frame.asset

        process = self.get_transcoding_process(asset)
        if process == 'COPY':
            self.copy_source(asset)
        elif process == 'OPTIMIZE':
            self.optimize_source(asset)
        else:
            self.transcode(asset)

    def transcode(self, asset):
        src_path = file_storage.localize_file(asset)
        dst_path = tempfile.mkstemp('.mp4')[1]

        cmd = ['ffmpeg',
               '-v', 'warning',
               '-y',
               '-i', src_path,
               '-c:v', 'libx264',
               '-crf', '23',
               '-c:a', 'aac',
               '-threads', '0',
               '-pix_fmt', 'yuv420p',
               '-movflags', '+faststart'
               ]

        # the width/height of yuv420p videos must be divisible by 2
        # non yuv420p mp4s don't have a lot of support.
        # This pads the video with black pixels.
        if asset.get_attr('media.height') % 2 != 0 or asset.get_attr('media.width') % 2 != 0:
            cmd.extend(('-vf', 'pad=ceil(iw/2)*2:ceil(ih/2)*2'))
        cmd.append(dst_path)

        self.logger.debug(f'Running: {cmd}')
        with StopWatch("Transcode video proxy"):
            subprocess.check_call(cmd, shell=False)
        store_media_proxy(asset, dst_path, 'video', None, {'transcode': 'full'})

    def optimize_source(self, asset):
        src_path = file_storage.localize_file(asset)
        dst_path = tempfile.mkstemp('.mp4')[1]

        cmd = [
            'ffmpeg',
            '-v', 'warning',
            '-y',
            '-i', src_path,
            '-movflags', '+faststart',
            '-threads', '0',
            '-acodec', 'aac',
            '-vcodec', 'copy',
            dst_path
        ]

        self.logger.debug(f'Running: {cmd}')
        with StopWatch("Optimize video proxy"):
            subprocess.check_call(cmd, shell=False)
        store_media_proxy(asset, dst_path, 'video', None, {'transcode': 'optimize'})

    def copy_source(self, asset):
        with StopWatch("Copy video proxy"):
            src_path = file_storage.localize_file(asset)
        store_media_proxy(asset, src_path, 'video', None, {'transcode': 'none'})

    def get_transcoding_process(self, asset):
        if asset.get_attr('source.extension').lower() in self.always_transcode:
            return 'TRANSCODE'

        path = file_storage.localize_file(asset)
        info = MediaInfo(path)
        is_h264 = asset.get_attr('media.videoCodec') == 'h264'
        is_streamable = info.is_streamable()
        if is_h264:
            if is_streamable:
                return 'COPY'
            else:
                return 'OPTIMIZE'
        else:
            return 'TRANSCODE'


class ExtractVideoClipProxyProcessor(AssetProcessor):
    """
    Extracts a physical video clip proxy at assets clip start/stop.  This
    processor will only run on non-full video clips.
    """
    file_types = ['mov', 'mp4', 'mpg', 'mpeg', 'm4v', 'webm', 'ogv', 'ogg', 'mxf']

    def __init__(self):
        super(ExtractVideoClipProxyProcessor, self).__init__()

    def process(self, frame):
        asset = frame.asset
        # If there is no clip there is some kind of issue
        # because a clip should be set at this point.
        clip = asset.get_attr('clip')
        if not clip:
            self.logger.warning('ExtractVideoClipProxyProcessor cannot continue, no clip defined')
            return

        # Don't make a cut of the full movie.
        if clip.get('track') == 'full':
            return

        self.make_h264_proxy_cut(asset)

    def make_h264_proxy_cut(self, asset):
        source_path = file_storage.localize_file(asset)
        with tempfile.NamedTemporaryFile(suffix=".mp4") as tf:
            cmd = ['ffmpeg',
                   '-v', 'warning',
                   '-y',
                   '-i', str(source_path),
                   '-ss', str(asset.get_attr('clip.start')),
                   '-t', str(asset.get_attr('clip.length')),
                   '-c:v', 'libx264',
                   '-crf', '23',
                   '-movflags', '+faststart',
                   '-pix_fmt', 'yuv420p',
                   tf.name]

            self.logger.debug(cmd)
            subprocess.check_call(cmd)
            store_media_proxy(asset, tf.name, 'video')
