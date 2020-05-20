import subprocess
import tempfile

from zmlp_core.util.media import get_video_metadata
from zmlpsdk.base import AssetProcessor
from zmlpsdk.storage import file_storage
from ..util.media import store_asset_proxy


class VideoProxyProcessor(AssetProcessor):
    """
    Makes a proxy video for a full video file.  Clip assets will reference
    this video file.
    """
    file_types = ['mov', 'mp4', 'mpg', 'mpeg', 'm4v', 'webm', 'ogv', 'ogg', 'mxf']

    def __init__(self):
        super(VideoProxyProcessor, self).__init__()

    def process(self, frame):
        asset = frame.asset
        clip = asset.get_attr('clip')
        if not clip:
            self.logger.warning('VideoProxyProcessor cannot continue, no clip defined')
            return -1

        # We only make proxies for full clips but this isn't
        # an error or warning.
        if clip.get('track') != 'full':
            return -1

        self.make_h264_proxy(asset)

    def make_h264_proxy(self, asset):
        source_path = file_storage.localize_file(asset)
        with tempfile.NamedTemporaryFile(suffix=".mp4") as tf:
            cmd = ['ffmpeg',
                   '-y',
                   '-i', str(source_path),
                   '-c:v', 'libx264',
                   '-preset', 'ultrafast',
                   '-vf', 'scale=%s:%s' % (min(1024, asset.get_attr("media.width")), -2),
                   '-movflags', '+faststart',
                   '-pix_fmt', 'yuv420p',
                   tf.name]
            self.logger.info(cmd)
            subprocess.check_call(cmd)
            store_video_proxy(asset, tf.name)


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
                   '-y',
                   '-i', str(source_path),
                   '-ss', str(asset.get_attr('clip.start')),
                   '-t', str(asset.get_attr('clip.length')),
                   '-c:v', 'libx264',
                   '-preset', 'ultrafast',
                   '-vf', 'scale=%s:%s' % (min(1024, asset.get_attr("media.width")), -2),
                   '-movflags', '+faststart',
                   '-pix_fmt', 'yuv420p',
                   tf.name]

            self.logger.info(cmd)
            subprocess.check_call(cmd)
            store_video_proxy(asset, tf.name)


def store_video_proxy(asset, path):
    """
    Store a video proxy with some arbitrary properties useful
    for playback.

    Args:
        asset (Asset): the asset
        path (str): The path to the video

    Returns:
        dict: a file storage dict
    """
    props = get_video_metadata(path)
    attrs = {"frames": props['frames'], 'frameRate': props['frameRate']}
    size = (props['width'], props['height'])
    return store_asset_proxy(asset, path, size, 'video', attrs)
