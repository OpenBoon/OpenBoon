"""Tools for building clips"""
from zmlp.entity import TimelineBuilder


class ClipTracker:
    """
    ClipTracker keeps track of start/stop times from predictions that come
    ML predictions on individual frames of a video in order to create a
    TimelineBuilder with tracks and clips.

    When all the clips are loaded call build_timeline() with the final time
    to close all open clips.

    """

    def __init__(self, asset, timeline_name):
        self.clips = {}
        self.timeline = TimelineBuilder(asset, timeline_name)

    def append(self, time, predictions):
        """
        Append the given labels and time to the ClipTracker
        Args:
            time (float): The video timecode time.
            predictions: A dictionary (label, score) or a list containing predictions info

        """

        if isinstance(predictions, list):
            # Setting default score in case of list
            predictions = {pred: 1 for pred in predictions}

        for label, score in predictions.items():
            current = self.clips.get(label)
            if not current:
                self.clips[label] = {
                    'start': time,
                    'stop': time,
                    'score': score
                }
            else:
                current['stop'] = time
                current['score'] = score

        to_remove = []
        for label, clip in self.clips.items():
            if clip['stop'] != time:
                self.timeline.add_clip(label, clip['start'], time, label, clip['score'])
                to_remove.append(label)

        for label in to_remove:
            del self.clips[label]

    def build_timeline(self, final_time):
        """
        Build and return a TimelineBuilder from the tracked clips.

        Args:
            final_time (float): The duration of the video.

        Returns:
            TimelineBuilder

        """
        self.append(final_time, {})
        return self.timeline
