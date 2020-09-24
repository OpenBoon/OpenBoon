"""
Classes and functions for building timelines.
"""
from ..util import as_id, as_collection

__all__ = [
    'TimelineBuilder'
]


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
