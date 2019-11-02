import subprocess as subprocess
from zorroa.zsdk.helpers import AbstractClipifier


class FFProbeKeyframeClipifier(AbstractClipifier):
    """Uses FFProbe to create clips based on keyframes."""
    def _get_clips(self, asset):
        movie_file = asset.get_local_source_path()
        minimum_clip_length = float(self.minimum_clip_length)

        # Get the duration.
        duration_command = ['ffprobe', '-v', 'error', '-show_entries', 'format=duration',
                            '-of', 'default=noprint_wrappers=1:nokey=1', movie_file]
        duration = subprocess.check_output(duration_command)
        duration = round(float(duration), 3)

        # Get the keyframes.
        keyframe_command = ('ffprobe -of compact=p=0 -show_entries '
                            'frame=pkt_pts_time,pict_type -f lavfi '
                            'movie=' + movie_file + ',select=gt(scene\\,0.1)')
        self.logger.info('FFPROBE COMMAND: %s' % keyframe_command)
        p = subprocess.Popen(keyframe_command.split(), stderr=subprocess.PIPE,
                             stdout=subprocess.PIPE)
        previous_seconds = 0.0
        clips = []
        while True:
            line = p.stdout.readline().decode("utf-8")
            if not line:
                break
            line = line.strip()
            if not line.startswith('pkt_pts_time'):
                continue
            self.logger.info('PROCESSING LINE: %s' % line)
            current_seconds = round(float(line.split('|')[0].split('=')[1]), 3)

            # Skip keyframe if the clip created will be too short
            if current_seconds - previous_seconds < minimum_clip_length:
                continue

            clips.append((previous_seconds, current_seconds))
            self.logger.info(current_seconds)
            previous_seconds = current_seconds
        clips.append((previous_seconds, duration))
        try:
            p.wait()
        except OSError:
            self.logger.warning('Exception thrown waiting on process to complete.')
        return clips

