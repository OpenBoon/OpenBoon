import logging
import os
import subprocess
import tempfile
import shlex

from datetime import datetime

logger = logging.getLogger(__name__)


def get_video_duration(video_path):
    """
    Return the duration of the given video file path.

    Args:
        video_path (str): A path to a video file/

    Returns:
        float: the duration in seconds.

    """
    duration_command = ['ffprobe', '-v', 'error', '-show_entries', 'format=duration',
                        '-of', 'default=noprint_wrappers=1:nokey=1', video_path]

    duration = subprocess.check_output(duration_command, shell=False)
    return round(float(duration), 3)


def extract_thumbnail_from_video(video_path, thumbnail_path, seconds):
    """Creates a thumbnail image from the video at the specified seconds.

    Args:
        video_path (str or Path): Path to the source video.
        thumbnail_path (str or Path): Path where the thumbnail should be
            created.
        seconds (float): The time in the video where the thumbnail should be
            taken from.

    Raises:
        (IOError): If the thumbnail could not be created.

    """
    cmd = ["ffmpeg",
           "-v",
           "warning",
           "-y",
           "-ss",
           str(seconds),
           "-i",
           str(video_path),
           "-qscale:v", "4",
           "-vframes",
           "1",
           str(thumbnail_path)]

    logger.info("running command: %s" % cmd)
    try:
        subprocess.check_call(cmd, shell=False)
    except subprocess.CalledProcessError:
        # Don't let CalledProcessError bubble out
        # we're only sending IOError
        pass

    if not os.path.exists(thumbnail_path):
        # Don't let the CalledProcessError impl detail leak out
        raise IOError('FFMpeg failed to create a thumbnail, command failed: {}'.format(cmd))


class WebvttBuilder:
    """
    A simple class for building a webvtt file.  This class is meant to
    be used with a 'with' statement.
    """
    def __init__(self, path=None):
        """
        Create a new WebvttBuilder.

        Args:
            path (str): An optional file path, otherwise a temp file.
        """
        self.path = path or tempfile.mkstemp(".vtt")[1]
        self.fp = None

    def append(self, time_in, time_out, content):
        start = datetime.utcfromtimestamp(float(time_in)).strftime('%H:%M:%S.%f')[:-3]
        stop = datetime.utcfromtimestamp(float(time_out)).strftime('%H:%M:%S.%f')[:-3]
        self.fp.write("{} --> {}\n{}\n\n".format(start, stop, content))

    def __enter__(self):
        self.fp = open(self.path, 'a')
        self.fp.write("WEBVTT\n\n")
        return self

    def __exit__(self, *args):
        self.fp.close()


class VideoFrameExtractor:
    """
    A simple base class for clip generators.
    """
    def __init__(self, video_file):
        """
        Create a new VideoClipGenerator with the given movie file.

        Args:
            video_file (str): The movie file path.
        """
        self.video_file = video_file

    def __iter__(self):
        return self._generate()

    def _generate(self):
        """
        Yields VideoClip instances to the caller.

        Returns:
            generator
        """
        pass


class TimeBasedFrameExtractor(VideoFrameExtractor):
    """
    A VideoFrameExtractor which generates an image for every N seconds
    of a video.  Only 1 frame is on disk at any given time, each
    iteration writes over the previous frame.
    """
    def __init__(self, video_file, seconds=1):
        """
        Create a new TimeBasedFrameExtractor.

        Args:
            video_file (str): The path to the videp file.
            seconds (float): The number of second between each image.
        """
        super(TimeBasedFrameExtractor, self).__init__(video_file)
        self.seconds = seconds
        self.duration = get_video_duration(self.video_file)
        self.output_path = tempfile.mkstemp(".jpg")[1]

    def __iter__(self):
        return self._generate()

    def _generate(self):
        duration = get_video_duration(self.video_file)
        scrubber = self.seconds * -1
        while scrubber < duration:
            scrubber = min(scrubber + self.seconds, duration)
            extract_thumbnail_from_video(self.video_file, self.output_path, scrubber)
            yield scrubber, self.output_path


class ShotBasedFrameExtractor(VideoFrameExtractor):
    """
    ShotBasedFrameExtractor uses ffmpeg lavfi shot detection filter to detect frame
    a high level of difference.  Only 1 frame is on disk at any given time, each
    iteration writes over the previous frame.
    """

    sensitivity = 0.12
    """How sensitive shot detection is, higher is less shots."""

    def __init__(self, video_file):
        """
        Create a new ShotBasedClipGenerator.

        Args:
            video_file (str): The video file path.
        """
        super(ShotBasedFrameExtractor, self).__init__(video_file)
        self.output_path = tempfile.mkstemp(".jpg")[1]

    def __iter__(self):
        return self._generate()

    def _generate(self):
        for shot_time in self._get_shot_times():
            extract_thumbnail_from_video(self.video_file, self.output_path, shot_time)
            yield shot_time, self.output_path

    def _get_shot_times(self):
        keyframe_command = ('ffprobe -show_frames -of compact=p=0 -show_entries '
                            'frame=pkt_pts_time,pict_type -f lavfi '
                            'movie={},select=gt(scene\\\\,{})'
                            .format(self.video_file, self.sensitivity))

        p = subprocess.Popen(shlex.split(keyframe_command),
                             stderr=subprocess.PIPE,
                             stdout=subprocess.PIPE,
                             shell=False)

        shot_times = [0.0]
        while True:
            line = p.stdout.readline().decode()
            if not line:
                break
            line = line.strip()
            if not line.startswith('pkt_pts_time'):
                continue
            current_seconds = round(float(line.split('|')[0].split('=')[1]), 3)
            shot_times.append(current_seconds)

        try:
            p.wait()
        except OSError:
            logger.warning('Exception thrown waiting on process to complete.')

        return shot_times
