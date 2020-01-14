from zmlpsdk import AssetProcessor, Argument
from .util import check_video_clip_preconditions, make_video_clip_file_import


class TimeBasedVideoClipifier(AssetProcessor):

    def __init__(self):
        super(TimeBasedVideoClipifier, self).__init__()
        self.add_arg(Argument('clip_length', 'float', default=5.0, required=True))

    def process(self, frame):
        asset = frame.asset
        # Bail out if we fail preconditions
        if not check_video_clip_preconditions(asset):
            return

        self._generate_clips(frame)

    def _generate_clips(self, frame):
        asset = frame.asset
        duration = asset.get_attr('clip.stop')
        length = self.arg_value('clip_length')

        scrubber = 0
        while scrubber < duration:
            cut_in = scrubber
            cut_out = min(scrubber + length, duration)
            scrubber = cut_out

            self.logger.info("Creating scene clip {}/{} from asset {}".format(
                cut_in, cut_out, frame.asset.id))

            scene_clip = make_video_clip_file_import(
                asset, cut_in, cut_out, 'time{}'.format(length))
            self.expand(frame, scene_clip)
