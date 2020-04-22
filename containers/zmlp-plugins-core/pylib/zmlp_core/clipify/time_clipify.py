from zmlpsdk import AssetProcessor, Argument
import zmlpsdk.video as video
from zmlpsdk.proxy import get_proxy_level_path


class TimeBasedVideoClipifier(AssetProcessor):

    def __init__(self):
        super(TimeBasedVideoClipifier, self).__init__()
        self.add_arg(Argument('clip_length', 'float', default=5.0, required=True))

    def process(self, frame):
        asset = frame.asset
        # Bail out if we fail preconditions
        if not video.check_video_clip_preconditions(asset):
            return -1
        self._generate_clips(frame)

    def _generate_clips(self, frame):
        asset = frame.asset

        video_proxy = get_proxy_level_path(asset, 0, "video/")
        clip_gen = video.TimeBasedClipGenerator(video_proxy, self.arg_value('clip_length'))

        for clip in clip_gen:
            scene_clip = video.make_video_clip_expand_frame(
                asset, clip.time_in, clip.time_out, 'time{}'.format(clip.length))
            self.expand(frame, scene_clip)
