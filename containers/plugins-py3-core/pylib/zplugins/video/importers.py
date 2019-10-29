import tempfile
from uuid import uuid4

from pathlib2 import Path

from zsdk.util import file_exists
from zsdk import DocumentProcessor, Argument, ExpandFrame
from zsdk.exception import UnrecoverableProcessorException, ProcessorException
from zplugins.util.media import ffprobe, create_video_thumbnail
from zplugins.util.proxy import add_proxy_file


class VideoImporter(DocumentProcessor):
    """Processor that handles ingestion of video files. This processor will add video
    file related metadata to the the media namespace. Additionally it will handle breaking
    the video into individual clips and creating web proxies.

    Args:
        expand_batch_size (int): Override the default number of frames in an expand batch.
        enable_clipifier (bool): If True the video will be broken into clips.
        enable_transcoder (bool): If True then web-optimized proxies wil be created for the
         video.
        clipifer (helper): Describes the clipifier helper to be used. This helper must
         inherit from AbstractClipifier.
        proxy_transcoder (helper): Describes the transcoder helper to be used. This helper must
         inherit from AbstractProxyTranscoder.

    """
    tool_tips = {
        'expand_batch_size': 'Override the default number of frames in an expand batch.',
        'enable_clipifier': 'If True the video will be broken into clips.',
        'clipifier': 'Describes the clipifier helper to be used. This helper must '
                     'inherit from AbstractClipifier.',
        'enable_proxy_transcoder': 'If True then web-optimized proxies wil be created for '
                                   'the video.',
        'proxy_transcoder': 'Describes the transcoder helper to be used. This helper must '
                            'inherit from AbstractProxyTranscoder',
        'enable_extract_proxy_image': 'Extract a proxy image from the video file.'
    }

    file_types = ['mov', 'mp4', 'mpg', 'mpeg', 'm4v', 'webm', 'ogv', 'ogg', 'mxf']
    default_transcoder = {'className': 'zplugins.video.proxytranscoders.FFMpegProxyTranscoder',
                          'args': {}}
    default_clipifier = {'className': 'zplugins.video.clipifiers.FFProbeKeyframeClipifier',
                         'args': {'minimum_clip_length': 1.0,
                                  'gcp_temp_bucket_name': ''}}

    def __init__(self):
        super(VideoImporter, self).__init__()
        self.clipifier = None
        self.transcoder = None
        self.add_arg(Argument('expand_batch_size', 'int', default=None,
                              toolTip=self.tool_tips['expand_batch_size']))
        self.add_arg(Argument('enable_clipifier', 'bool', default=False,
                              toolTip=self.tool_tips['enable_clipifier']))
        self.add_arg(Argument('clipifier', 'helper', default=self.default_clipifier,
                              toolTip=self.tool_tips['clipifier']))
        self.add_arg(Argument('enable_proxy_transcoder', 'bool', default=True,
                              toolTip=self.tool_tips['enable_proxy_transcoder']))
        self.add_arg(Argument('enable_extract_proxy_image', 'bool', default=True,
                              toolTip=self.tool_tips['enable_extract_proxy_image']))
        self.add_arg(Argument('proxy_transcoder', 'helper', default=self.default_transcoder,
                              toolTip=self.tool_tips['proxy_transcoder']))

    def init(self):
        super(VideoImporter, self).init()
        self.clipifier = self.instantiate_helper(self.arg_value('clipifier'))
        self.transcoder = self.instantiate_helper(self.arg_value('proxy_transcoder'))

    def _process(self, frame):
        asset = frame.asset
        self._fix_media_type(asset)
        self._set_media_metadata(asset)
        if self.arg_value('enable_extract_proxy_image'):
            self.logger.info('Extracting a proxy thumbnail.')
            self._create_proxy_source_image(asset)
        if self.arg_value('enable_proxy_transcoder') and not asset.is_clip():
            self.logger.info('Creating web proxies.')
            self._create_web_proxies(asset)
        if self.arg_value('enable_clipifier'):
            self.logger.info('Extracting clips from video.')
            self._clipify(frame)

    def _set_media_metadata(self, asset):
        """Introspects a video file and adds metadata to the media namespace on the asset.

        Args:
            asset (Asset): Asset to add metadata to.

        """
        path = asset.get_local_source_path()
        ffprobe_info = ffprobe(path)
        media = asset.get_attr('media', {})
        frame_rate = None
        frames = None
        width = None
        height = None

        for stream in ffprobe_info.get('streams', []):
            if stream.get('codec_type') == 'video':
                # Determine the frame rate.
                frame_rate = stream.get('r_frame_rate')
                if frame_rate:
                    numerator, denominator = frame_rate.split('/')
                    frame_rate = round(float(numerator) / float(denominator), 2)
                media['frameRate'] = frame_rate
                media['frames'] = stream.get('nb_frames')

                # Set the dimensions
                width = stream.get('width')
                height = stream.get('height')
            elif stream.get('codec_type') == 'audio':
                # Set audio information.
                media['audioChannels'] = stream.get('channels')
                media['audioBitRate'] = stream.get('bit_rate')

        # Set the video duration.
        duration = ffprobe_info.get('format', {}).get('duration')
        if duration:
            duration = float(duration)
            media['duration'] = duration
            if not frames:
                media['frames'] = int(duration * frame_rate)
        elif frame_rate and frames and not duration:
            media['duration'] = float(frames) / float(frame_rate)

        # Set the title and description.
        media['description'] = ffprobe_info.get('format', {}).get('tags', {}).get('description')
        media['title'] = ffprobe_info.get('format', {}).get('tags', {}).get('title')

        asset.set_attr('media', media)
        if width and height:
            asset.set_resolution(width, height)

    def _create_proxy_source_image(self, asset):
        """Creates a source image to be used by the ProxyIngestor.

        Args:
            asset (Asset): Asset to create a thumbnail for.

        """
        # Determine the second at which to pull the thumbnail from the video.
        seconds = 0

        if asset.get_attr('media.clip.start') and asset.get_attr('media.clip.length'):
            start = asset.get_attr('media.clip.start')
            length = asset.get_attr('media.clip.length')
            seconds = start + (length * 0.5)
        elif asset.get_attr('media.duration'):
            seconds = asset.get_attr('media.duration') * 0.5

        source_path = asset.get_local_source_path()
        destination_path = Path(tempfile.mkdtemp("video_ingestor"), asset.id + '.jpg')

        # If the thumbnail fails to generate at the specified point, then
        # fallback to 0.
        final_error = None
        for try_seconds in [seconds, 0]:
            try:
                create_video_thumbnail(source_path, destination_path, try_seconds)
                break
            except IOError as e:
                final_error = e

        if not file_exists(destination_path):
            raise ProcessorException("Unable to extract video proxy image for {}, unexpected {}"
                                     .format(destination_path, final_error))

        asset.set_attr('tmp.proxy_source_image', str(destination_path))

    def _clipify(self, frame):
        """Breaks the video into a series of individual clips using the clipifier
        specified. If this frame is already a clip then it is ignored.

        Args:
            frame (Frame): Frame that should be broken into clips.

        """
        asset = frame.asset
        if asset.is_clip():
            return
        clips = self.clipifier.get_clips(asset)
        clip_count = 0
        for start, stop in clips:
            try:
                clip_asset = asset.create_clip(type='video', start=start, stop=stop)
                expand = ExpandFrame(clip_asset)
                self.expand(frame, expand, batch_size=self.arg_value("expand_batch_size"))
                clip_count += 1
                self.logger.info('Added a clip. Total clips: %d' % clip_count)
            except Exception:
                self.logger.exception('')
                raise UnrecoverableProcessorException('Error creating clip from video.')

    def _create_web_proxies(self, asset):
        """Creates proxy videos for the asset that are optimized for web streaming.

        Args:
            asset (Asset): Asset to create web proxies for.

        """
        source_path = asset.get_local_source_path()
        destination_path = Path(tempfile.mkdtemp(), '%s.mp4' % uuid4())
        self.transcoder.transcode(source_path, str(destination_path), height=1080)
        add_proxy_file(asset, destination_path)

    def _fix_media_type(self, asset):
        """
        If the asset was processed by this processor, then it's an video regardless
        of what the mimetype magic detected.

        Reset the various media type properties so the file is handled properly downstream.

        Args:
            asset (Asset): The asset to fix.

        """
        current_type = asset.get_attr("source.type")
        if current_type != "video":
            sub_type = asset.get_attr("source.extension")
            asset.set_attr("source.mediaType", "video/{}".format(sub_type))
            asset.set_attr("source.type", "video")
            asset.set_attr("source.subType", sub_type)

