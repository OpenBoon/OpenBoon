import logging
import os
import subprocess
import tempfile
import shlex
import hashlib
import json
import shutil

from datetime import datetime

from .base import app_instance

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


def extract_thumbnail_from_video(video_path, thumbnail_path, seconds, size=None):
    """Creates a thumbnail image from the video at the specified seconds.

    Args:
        video_path (str or Path): Path to the source video.
        thumbnail_path (str or Path): Path where the thumbnail should be
            created.
        seconds (float): The time in the video where the thumbnail should be
            taken from.
        size (tuple): The size the frame should be resized to, if any.
    Raises:
        (IOError): If the thumbnail could not be created.

    """
    cmd = ["ffmpeg",
           "-v",
           "error",
           "-y",
           "-ss",
           str(seconds),
           "-i",
           str(video_path),
           "-qscale:v", "4",
           "-vframes",
           "1",
           str(thumbnail_path)]

    if size:
        cmd.insert(8, "-s")
        cmd.insert(9, "%dx%d" % size)

    logger.info(f"Extracting thumbnail at time {seconds}")
    logger.debug("running command: %s" % cmd)
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

    def __init__(self, video_file, sensitivity=0.025, min_length=1.0):
        """
        Create a new ShotBasedClipGenerator.  For best results, it's better to have
        more sensitive shot detection and a min clip length.

        Args:
            video_file (str): The video file path.
            sensitivity: (float): A lower value creates more shots.
            min_length: (float): The minimum clip length.
        """
        super(ShotBasedFrameExtractor, self).__init__(video_file)
        self.output_dir = self.make_output_dir()
        self.catalog_path = f'{self.output_dir}/catalog.json'
        self.sensitivity = sensitivity
        self.min_length = min_length

    def make_output_dir(self):
        """
        Create a temp dir for holding our video frqmes and catalog.

        Returns:
            str: The path to the temp dir.
        """
        tdir = tempfile.gettempdir()
        hashval = hashlib.sha1(self.video_file.encode()).hexdigest()
        out_dir = f'{tdir}/{hashval}'
        os.makedirs(out_dir, exist_ok=True)
        return out_dir

    def clean(self):
        try:
            shutil.rmtree(self.output_dir)
        except Exception:
            logger.exception(f'Unable to delete {self.output_dir}')
        finally:
            self.make_output_dir()

    def __iter__(self):
        return self._generate()

    def _generate(self):
        if not os.path.exists(f'{self.output_dir}/catalog.json'):
            self._generate_catalog_file()

        with open(self.catalog_path, "r") as fp:
            catalog = json.load(fp)

        for shot_time, file_path in catalog:
            yield shot_time, file_path

    def _generate_catalog_file(self):
        logger.info(f'Creating video frame catalog ${self.catalog_path}')

        catalog = []
        for idx, shot_time in enumerate(self._get_shot_times()):
            file_name = f'{self.output_dir}/frame_{idx}.jpg'
            try:
                extract_thumbnail_from_video(self.video_file, file_name, shot_time)
                catalog.append((shot_time, file_name))
            except IOError:
                logger.warning(f'Failed to extract frame at {shot_time}')

        with open(self.catalog_path, "w") as fp:
            json.dump(catalog, fp)

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
        clip_time = 0.0
        while True:
            line = p.stdout.readline().decode()
            if not line:
                break
            line = line.strip()
            if not line.startswith('pkt_pts_time'):
                continue
            point = round(float(line.split('|')[0].split('=')[1]), 3)
            if point - clip_time < self.min_length:
                continue
            clip_time = point
            shot_times.append(point)

        try:
            p.wait()
        except OSError:
            logger.warning('Exception thrown waiting on process to complete.')

        return shot_times


def save_timeline(asset, timeline):
    """
    Save the given timeline as Clips.

    Args:
        asset (Asset): The asset to save the timeline for.
        timeline (TimelineBuilder): The timeline

    Returns:
        dict: A status object.

    """
    # Disable thumbs when creating timelines from processing
    # These are processed later.
    timeline.deep_analysis = False

    new_timelines = asset.get_attr('tmp.timelines')
    if not new_timelines:
        new_timelines = []
    new_timelines.append(timeline.name)
    asset.set_attr('tmp.timelines', new_timelines)

    app = app_instance()
    return app.clips.create_clips(timeline)
