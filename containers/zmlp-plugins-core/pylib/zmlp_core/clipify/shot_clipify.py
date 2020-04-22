import zmlpsdk.video as video
from zmlpsdk import AssetProcessor, Argument
from zmlpsdk.proxy import get_proxy_level_path


class ShotDetectionVideoClipifier(AssetProcessor):

    def __init__(self):
        super(ShotDetectionVideoClipifier, self).__init__()
        self.add_arg(Argument('min_clip_length', 'float', default=1.0, required=True))

    def process(self, frame):
        asset = frame.asset
        # Bail out if we fail preconditions
        if not video.check_video_clip_preconditions(asset):
            return -1

        video_proxy = get_proxy_level_path(asset, 0, "video/")
        self._generate_clips(frame, video_proxy)

    def _generate_clips(self, frame, movie_file):
        clip_gen = video.ShotBasedClipGenerator(movie_file, self.arg_value('min_clip_length'))
        for clip in clip_gen:
            self._create_clip(frame, clip)

    def _create_clip(self, frame, clip):
        self.logger.info("Creating scene clip {} from asset {}".format(clip, frame.asset.id))
        self.expand(frame, video.make_video_clip_expand_frame(
            frame.asset, clip.time_in, clip.time_out, 'shot'))
