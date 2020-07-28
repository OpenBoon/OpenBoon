"""
Classes and functions for building timelines.
"""
import decimal
import time

class Clip:
    """
    A clip is a section of a video timeline on a certain Track.  Clips have unique start
    and stop points and cannot overlap.
    """

    def __init__(self, start, stop, metadata=None):
        """
        Create a new Clip instance.

        Args:
            start (float): The start offset time of the clip.
            stop (float): The stop offset time of the clip.
            metadata (dict): A dictionary of arbitrary metadata.
        """
        self.start = truncate(start, 3)
        self.stop = truncate(stop, 3)
        self.metadata = metadata or {}

    def extend_to(self, position):
        """
        Move the end of the clip to the given position.

        Args:
            position (float): The position to extend the clip to.
        """
        position = truncate(position, 3)
        if position < self.stop:
            raise ValueError('The new position is less than the current end')
        self.stop = position

    def intersects(self, position):
        position = truncate(position, 3)
        return self.start <= position <= self.stop

    def for_json(self):
        return {
            'start': self.start,
            'stop': self.stop,
            'metadata': self.metadata
        }


class Track:
    """
    A Track is a collection of Clips.

    """
    def __init__(self, name, metadata=None, sort=None):
        """

        Args:
            name (str): The name of the Track.
            metadata (dict): A dict of arbitrary metadata.
        """
        self.name = name
        self.metadata = metadata or {}
        self._clips = {}
        self._sort = sort

    def add_clip(self, start, stop, metadata=None):
        """
        Add and return a Clip to the given track.
        Args:
            start (float): The start time of the clip.
            stop (float): The stop time of the clip.
            metadata (dict): A dict of arbitrary metadata.
        """
        if stop < start:
            raise ValueError("The stop time cannot be smaller than the start time.")

        key = str(start)
        clip = self._clips.get(key)
        if not clip:
            clip = Clip(start, stop, metadata)
            self._clips[key] = clip
        elif metadata:
            clip.metadata.update(metadata)
        return clip

    @property
    def sort(self):
        if self._sort is not None:
            return self._sort
        else:
            return self.name

    @property
    def clips(self):
        return sorted(self._clips.values(), key=lambda c: c.start)

    def for_json(self):
        return {
            'name': self.name,
            'metadata': self.metadata,
            'clips': self.clips
        }

    def for_webvtt(self):
        count = 0

        webvtt = "WEBVTT - {}\n\n".format(self.name)
        for clip in self.clips:
            start = clip["start"]
            stop = clip["stop"]
            content = clip['metadata']

            if content:
                start = time.strftime("%H:%M:%S.000", time.gmtime(float(start)))
                stop = time.strftime("%H:%M:%S.000", time.gmtime(float(stop)))
                
                line = "{}\n{} --> {}\n{}\n\n".format(count, start, stop, content)
                webvtt += line
                count += 1

        return webvtt


class Timeline:
    """
    The Timeline class is used for building a ZMLP timeline structure from video analysis
    such as Google Video Intelligence.  Thee data is saved as a timeline proxy against an Asset
    and is itended to be loaded by a video player.

    Timelines contain an array of Tracks, and Tracks contain an array of clips.
    """

    def __init__(self, name, metadata=None):
        """
        Create a new timeline instance.
        Args:
            name (str): The name of the Timeline.
            metadata (dict): Arbitrary timeline metadata.
        """
        self.name = name
        self.metadata = metadata or {}
        self._tracks = {}

    def get_track(self, name):
        """
        Return the track with the given name or None.

        Args:
            name (str): The track name.

        Returns:
            Track: The track
        """
        return self._tracks.get(name)

    def add_track(self, name, metadata=None, sort=None):
        """
        Add and return a Track to the Timeline.  If a Track with the same name
        exists then it will be returned.

        Args:
            name (str): The Track name.
            metadata (dit): A dictionary of metadata.
            sort (mixed): A value to sort the track by, defaults to the name.
        Returns:
            Track: The new or existing Track.

        """
        track = self._tracks.get(name)
        if not track:
            track = Track(name, metadata, sort)
            self._tracks[name] = track
        elif metadata:
            track.metadata.update(metadata)
        return track

    @property
    def tracks(self):
        """
        A sorted list of Tracks.

        Returns:
            list[Track] a sorted list of Tracks
        """
        return sorted(self._tracks.values(), key=lambda c: c.sort)

    def for_json(self):
        return {
            'name': self.name,
            'metadata': self.metadata,
            'tracks': [track for track in self.tracks if len(track.clips) > 0]
        }


def truncate(number, places):
    """
    Truncate a float to the given number of places.

    Args:
        number (float): The number to truncate.
        places (int): The number of plaes to preserve.

    Returns:
        float: The truncated float.
    """
    if not isinstance(places, int):
        raise ValueError('Decimal places must be an integer.')
    if places < 1:
        raise ValueError('Decimal places must be at least 1.')

    with decimal.localcontext() as context:
        context.rounding = decimal.ROUND_DOWN
        exponent = decimal.Decimal(str(10 ** - places))
        return decimal.Decimal(str(number)).quantize(exponent).to_eng_string()
