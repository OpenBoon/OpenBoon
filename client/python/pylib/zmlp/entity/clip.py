"""
Classes and functions for building timelines.
"""
from ..entity.asset import StoredFile
from ..util import as_id, as_collection

__all__ = [
    'TimelineBuilder',
    'VideoClip'
]


class VideoClip:
    """
    Clips represent a prediction for a section of video.
    """
    def __init__(self, data):
        self._data = data

    @property
    def id(self):
        """The Asset id the clip is associated with."""
        return self._data['id']

    @property
    def asset_id(self):
        """The Asset id the clip is associated with."""
        return self._data['assetId']

    @property
    def timeline(self):
        """The name of the timeline, this is the same as the pipeline module."""
        return self._data['timeline']

    @property
    def track(self):
        """The track name"""
        return self._data['track']

    @property
    def content(self):
        """The content of the clip. This is the prediction"""
        return self._data['content']

    @property
    def length(self):
        """The length of the clip"""
        return self._data['length']

    @property
    def start(self):
        """The start time of the clip"""
        return self._data['start']

    @property
    def stop(self):
        """The stop time of the clip"""
        return self._data['stop']

    @property
    def score(self):
        """The prediction score"""
        return self._data['score']

    @property
    def simhash(self):
        """A similarity hash, if any"""
        return self._data.get('simhash')

    @property
    def files(self):
        """The array of associated files."""
        return [StoredFile(f) for f in self._data.get('files', [])]

    @staticmethod
    def from_hit(hit):
        """
        Converts an ElasticSearch hit into an VideoClip.

        Args:
            hit (dict): An raw ES document

        Returns:
            Asset: The Clip.
        """
        data = {
            'id': hit['_id'],
        }
        data.update(hit.get('_source', {}).get('clip', {}))
        return VideoClip(data)

    def __len__(self):
        return self.length

    def __str__(self):
        return "<VideoClip id='{}'/>".format(self.id)

    def __repr__(self):
        return "<VideoClip id='{}' at {}/>".format(self.id, hex(id(self)))

    def __eq__(self, other):
        return other.id == self.id

    def __hash__(self):
        return hash(self.id)


class TimelineBuilder:
    """
    The TimelineBuilder class is used for batch creation of video clips.  Clips within a track
    can be overlapping.  Duplicate clips are automatically compacted to the highest score.
    """

    def __init__(self, asset, name):
        """
        Create a new timeline instance.
        Args:
            name (str): The name of the Timeline.
        """
        self.asset = as_id(asset)
        self.name = name
        self.tracks = {}

    def add_clip(self, track_name, start, stop, content, score=1, tags=None):
        """
        Add a clip to the timeline.

        Args:
            track_name (str): The Track name.
            start (float): The starting time.
            stop (float): The end time.
            content (str): The content.
            score: (float): The score if any.
            tags: (list): A list of tags that describes the content.

        Returns:
            (dict): A clip entry.

        """
        if stop < start:
            raise ValueError("The stop time cannot be smaller than the start time.")

        track = self.tracks.get(track_name)
        if not track:
            track = {'name': track_name, 'clips': []}
            self.tracks[track_name] = track

        clip = {
            "start": start,
            "stop":  stop,
            "content": [c.replace("\n", " ").strip() for c in as_collection(content)],
            "score": score,
            "tags": as_collection(tags)
        }

        track['clips'].append(clip)
        return clip

    def for_json(self):
        return {
            'name': self.name,
            'assetId': self.asset,
            'tracks': [track for track in self.tracks.values() if track['clips']]
        }
