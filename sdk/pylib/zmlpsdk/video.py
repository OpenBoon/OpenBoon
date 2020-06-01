import logging
import os
import shlex
import subprocess
import tempfile

from zmlp import Clip, FileImport
from zmlpsdk.base import ExpandFrame
from zmlpsdk.proxy import get_proxy_level_path

logger = logging.getLogger(__name__)


class VideoClipGenerator:
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


class TimeBasedClipGenerator(VideoClipGenerator):
    """
    A VideoClip generator which generates a clip for every N seconds
    of a video.
    """
    def __init__(self, video_file, clip_length=5):
        """
        Create a new TimeBasedClipGenerator.

        Args:
            video_file (str): The path to the videp file.
            clip_length (float): The length of each clip.
        """
        super(TimeBasedClipGenerator, self).__init__(video_file)
        self.clip_length = clip_length

    def __iter__(self):
        return self._generate()

    def _generate(self):
        duration = get_video_duration(self.video_file)

        scrubber = 0
        while scrubber < duration:
            cut_in = scrubber
            cut_out = min(scrubber + self.clip_length, duration)
            scrubber = cut_out
            yield VideoClip(cut_in, cut_out)


class ShotBasedClipGenerator(VideoClipGenerator):
    """
    A VideoClip generator which generates a video clip for every
    iframe in a video.
    """

    def __init__(self, video_file, min_clip_length=5):
        """
        Create a new ShotBasedClipGenerator.

        Args:
            video_file (str): The video file path.
            min_clip_length (float): The minimum clip size.

        """
        super(ShotBasedClipGenerator, self).__init__(video_file)
        self.min_clip_length = float(min_clip_length)

    def __iter__(self):
        return self._generate()

    def _generate(self):
        duration = get_video_duration(self.video_file)
        keyframe_command = ('ffprobe -show_frames -of compact=p=0 -show_entries '
                            'frame=pkt_pts_time,pict_type -f lavfi '
                            'movie=' + self.video_file + ',select=gt(scene\\\\,0.1)')

        p = subprocess.Popen(shlex.split(keyframe_command),
                             stderr=subprocess.PIPE,
                             stdout=subprocess.PIPE,
                             shell=False)

        previous_seconds = 0.0
        while True:
            line = p.stdout.readline().decode()
            if not line:
                break
            line = line.strip()
            if not line.startswith('pkt_pts_time'):
                continue
            current_seconds = round(float(line.split('|')[0].split('=')[1]), 3)

            # Skip keyframe if the clip created will be too short
            if current_seconds - previous_seconds < self.min_clip_length:
                continue

            yield VideoClip(previous_seconds, current_seconds)
            previous_seconds = current_seconds

        # Don't make a clip if there are no previous clips.
        # This would just be a dup
        if previous_seconds != 0.0:
            yield VideoClip(previous_seconds, duration)

        try:
            p.wait()
        except OSError:
            self.logger.warning('Exception thrown waiting on process to complete.')


class VideoClip:
    """
    A VideoClip describes a section of a video track.

    Attributes:
        length (float): The length of the clip.
        midpoint (float): The mid point time of the clip.
    """
    def __init__(self, time_in, time_out):
        """
        Create a new Video Clip.

        Args:
            time_in (float): The clip start time in seconds.
            time_out (float): The clip stop time in seconds.

        """
        self.time_in = round(time_in, 3)
        self.time_out = round(time_out, 3)
        self.length = round(time_out - time_in, 3)
        self.midpoint = round(time_in + (self.length / 2.0), 3)

    def __str__(self):
        return "<VideoClip in={} out={}>".format(self.time_in, self.time_out)


class VideoFrameIterator:
    """
    A class for iterating images from within a video.  Iterating
    an instance of this class will return a tuple of VideoClip, file path.

    Examples:

        for clip, path in VideoFrameIterator(TimeBasedClipGenerator(movie)):
            print(clip)
            print(path)

    """
    def __init__(self, clip_gen):
        """
        Create a new VideoFrameIterator.

        Args:
            clip_gen (VideoClipGenerator): A VideoClipGenerator it generate clips with.
        """
        self.clip_gen = clip_gen
        self.image_path = tempfile.gettempdir() + "/video_frame_output.jpg"

    def __iter__(self):
        return self._generate()

    def _generate(self):
        for clip in self.clip_gen:
            extract_thumbnail_from_video(self.clip_gen.video_file,
                                         self.image_path,
                                         clip.midpoint)
            yield clip, self.image_path


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
           "1",
           "-y",
           "-ss",
           str(seconds),
           "-i",
           str(video_path),
           "-vframes",
           "1",
           str(thumbnail_path)]

    logger.info("running command: %s" % cmd)
    try:
        subprocess.check_call(cmd, shell=False, stderr=subprocess.DEVNULL)
    except subprocess.CalledProcessError:
        # Don't let CalledProcessError bubble out
        # we're only sending IOError
        pass

    if not os.path.exists(thumbnail_path):
        # Don't let the CalledProcessError impl detail leak out
        raise IOError('FFMpeg failed to create a thumbnail, command failed: {}'.format(cmd))


def check_video_clip_preconditions(asset):
    """
    Return true if the given asset can be clipified.

    Args:
        asset (Asset): The asset which might be clipified.

    Returns:
        bool: True if all preconditions are met

    """
    # Only video can be clipped
    if asset.get_attr('media.type') != 'video':
        return False

    # Only full videos can be clipped
    if asset.get_attr('clip.track') != 'full':
        return False

    if not get_proxy_level_path(asset, 3, "video/"):
        return False

    return True


def make_video_clip_expand_frame(asset, time_in, time_out, track):
    """
    Make an FileImport wrapped in an ExpandFrame that can be
    emitted back to the archivist using the parent asset and given clip
    args.

    Args:
        asset (Asset): The parent asset.
        time_in (float): The start time of the clip.
        time_out (float): The stop time of the clip.
        track (str): The name of the track.

    Returns:

    """
    clip = Clip.scene(time_in, time_out, track)
    file_import = FileImport("asset:{}".format(asset.id), clip=clip)
    # Copy media onto the new asset since it's going to be
    # exactly the same.
    file_import.attrs["media"] = asset.get_attr('media')
    return ExpandFrame(file_import)
