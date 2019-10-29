import subprocess

from pathlib2 import Path

from zsdk.processor import ProcessorHelper


class AbstractProxyTranscoder(ProcessorHelper):
    """Abstract class all Transcoders should inherit from. Transcoders are used by the
     video ingestor to create web proxy material. When implementing a Transcoder the
     _transcode method should be overridden.

     """
    def transcode(self, source_path, destination_path, width=None, height=None):
        """Transcodes the source path into a web-optimized video and stores it in the
        destination path. Width and/or height can be given, if only one is given then
        the other will be calculated to maintain the aspect ratio.

        Args:
            source_path (str or Path): Path to the source video.
            destination_path (str or Path): Path to where the transcoded video will be
             saved.
            width (int): Width of the proxy video in pixels.
            height (int): Height of the proxy video in pixels.

        Raises:
             IOError: If the transcode was unsuccessful.
        """
        self._transcode(source_path, destination_path, width=width, height=height)

        # Validates the overridden method in concrete classes.
        if not Path(destination_path).exists():
            raise IOError('Transcoding failed for %s.' % source_path)

    def _transcode(self, source_path, destination_path, width=None, height=None):
        """This method is wrapped by the transcode method and must be overridden in
        a concrete class. This method is expected to generate web-optimized proxy from
        the source path and store it in the destination path. This method is also
        responsible for determining the proxy resolution based on the width and/or height
        given. If only one is given then the other measurement must be calculated to
        match the source aspect ratio.

        """
        raise NotImplementedError


class FFMpegProxyTranscoder(AbstractProxyTranscoder):
    """Uses FFMpeg to create web-optimized proxies."""
    def _transcode(self, source_path, destination_path, width=None, height=None):
        if width is None:
            width = -2
        if height is None:
            height = -2
        ffmpeg_command = ['ffmpeg', '-y',
                          '-i', str(source_path),
                          '-strict', '-2',
                          '-v', 'debug',
                          '-threads', '0',
                          '-c:v', 'libx264',
                          '-c:a', 'aac',
                          '-preset', 'medium',
                          '-vf', 'scale=%s:%s' % (width, height),
                          '-b:v', '3m',
                          '-movflags', '+faststart',
                          '-profile:v', 'high', '-level', '4.0',
                          '-pix_fmt', 'yuv420p',
                          '-ac', '2',
                          str(destination_path)]
        subprocess.check_call(ffmpeg_command)
