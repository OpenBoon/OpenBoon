import logging
import os
import subprocess


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
