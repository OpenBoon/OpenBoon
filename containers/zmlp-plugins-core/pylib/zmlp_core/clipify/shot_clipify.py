import subprocess

from zmlp.analysis import AssetBuilder, Argument
from zmlp.analysis.proxy import get_proxy_level
from .util import check_video_clip_preconditions, make_video_clip_file_import


class ShotDetectionVideoClipifier(AssetBuilder):

    def __init__(self):
        super(ShotDetectionVideoClipifier, self).__init__()
        self.add_arg(Argument('min_clip_length', 'float', default=1.0, required=True))

    def process(self, frame):
        asset = frame.asset
        # Bail out if we fail preconditions
        if not check_video_clip_preconditions(asset):
            return

        self._generate_clips(frame, get_proxy_level(asset, 3, "video/"))

    def _generate_clips(self, frame, movie_file):
        min_clip_length = float(self.arg_value('min_clip_length'))

        # Get the duration.
        duration_command = ['ffprobe', '-v', 'error', '-show_entries', 'format=duration',
                            '-of', 'default=noprint_wrappers=1:nokey=1', movie_file]
        duration = subprocess.check_output(duration_command)
        duration = round(float(duration), 3)

        # Get the keyframes.
        keyframe_command = ('ffprobe -show_frames -of compact=p=0 -show_entries '
                            'frame=pkt_pts_time,pict_type -f lavfi '
                            'movie=' + movie_file + ',select=gt(scene\\,0.1)')

        self.logger.debug('FFPROBE COMMAND: %s' % keyframe_command)
        p = subprocess.Popen(keyframe_command.split(' '),
                             stderr=subprocess.PIPE,
                             stdout=subprocess.PIPE)
        previous_seconds = 0.0

        while True:
            line = p.stdout.readline().decode()
            print(line)
            if not line:
                break
            line = line.strip()
            if not line.startswith('pkt_pts_time'):
                continue
            self.logger.debug('PROCESSING LINE: %s' % line)
            current_seconds = round(float(line.split('|')[0].split('=')[1]), 3)

            # Skip keyframe if the clip created will be too short
            if current_seconds - previous_seconds < min_clip_length:
                continue

            self._create_clip(frame, previous_seconds, current_seconds)
            previous_seconds = current_seconds

        # Don't make a clip if there are no previous clips.
        # This would just be a dup
        if previous_seconds != 0.0:
            self._create_clip(frame, previous_seconds, duration)
        try:
            p.wait()
        except OSError:
            self.logger.warning('Exception thrown waiting on process to complete.')

    def _create_clip(self, frame, cut_in, cut_out):
        self.logger.info("Creating scene clip {}/{} from asset {}".format(
            cut_in, cut_out, frame.asset.id))
        self.expand(frame, make_video_clip_file_import(frame.asset, cut_in, cut_out, 'shot'))
