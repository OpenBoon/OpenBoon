import subprocess
import tempfile

from boonflow import AssetProcessor, StopWatch, FileTypes
from boonflow.storage import file_storage
from ..util.media import store_media_proxy, MediaInfo


class VideoProxyProcessor(AssetProcessor):
    """
    Makes a proxy video for a full video file.  Clip assets will reference
    this video file.
    """
    file_types = FileTypes.videos

    # Always transcode this media
    always_transcode = ['webm', 'ogv', 'ogg', 'mxf', 'avi']

    # The higher this is, the slower and more costly ML processing will be.
    # We're not a dam solution for reviewing media, and there is not a single
    # ML process we need full HD for.
    max_resolution = 1280

    # Don't use threads for transcoding, ffmpeg already does.
    use_threads = False

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
        width = asset.get_attr('media.width')
        height = asset.get_attr('media.height')

        filters = []
        if width > self.max_resolution:
            filters.append("scale={}:-2:flags=lanczos".format(self.max_resolution))
        elif height > self.max_resolution:
            filters.append("scale=-2:{}:flags=lanczos".format(self.max_resolution))

        cmd = [
            'ffmpeg',
            '-v', 'warning',
            '-y',
            '-i', src_path,
            '-c:v', 'libx264',
            '-crf', '23',
            '-c:a', 'aac',
            '-threads', '0',
            '-pix_fmt', 'yuv420p',
            '-movflags', '+faststart',
            '-preset', 'slow'
        ]

        # the width/height of yuv420p videos must be divisible by 2
        # non yuv420p mp4s don't have a lot of support.
        # This pads the video with black pixels.
        if asset.get_attr('media.height') % 2 != 0 or asset.get_attr('media.width') % 2 != 0:
            filters.append('pad=ceil(iw/2)*2:ceil(ih/2)*2')

        if filters:
            cmd.append('-vf')
            cmd.extend(filters)

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
        path = file_storage.localize_file(asset)

        if self.needs_video_transcoding(asset):
            return 'TRANSCODE'
        else:
            info = MediaInfo(path)
            if not info.is_streamable():
                return 'OPTIMIZE'
            else:
                return 'COPY'

    def needs_video_transcoding(self,  asset):
        """
        Return true if the video needs transcoding.

        Args:
            asset (Asset): The Asset.

        Returns:
            bool: true if we should transcode
        """
        if asset.get_attr('source.extension').lower() in self.always_transcode:
            return True

        if not asset.get_attr('media.videoCodec') == 'h264':
            return True

        if asset.get_attr('media.width') > self.max_resolution \
                or asset.get_attr('media.height') > self.max_resolution:
            return True

        return False
