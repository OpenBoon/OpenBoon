"""Tools for building clips"""
from boonflow import Prediction
from boonsdk.entity import TimelineBuilder
from boonsdk.util import as_collection


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
        # Make a timeline but disable deep analysis.
        self.timeline = TimelineBuilder(asset, timeline_name, deep_analysis=False)

    def append(self, time, predictions):
        """
        Append the given labels and time to the ClipTracker
        Args:
            time (float): The video timecode time.
            predictions: A dictionary (label, score) or a list containing predictions info

        """
        if isinstance(predictions, list):
            # Setting default score in case of list
            predictions = [Prediction(pred, 1) for pred in predictions]
        elif isinstance(predictions, dict):
            predictions = [Prediction(k, v) for k, v in predictions.items()]

        self.append_predictions(time, predictions)

    def append_predictions(self, time, preds):
        """
        Append a list of predictions to the ClipTacker.

        Args:
            time (float): Time in seconds.
            preds (list): A list of predictions.
        """

        for pred in as_collection(preds):
            label = pred.label
            score = pred.score
            current = self.clips.get(pred.label)
            if not current:
                self.clips[label] = {
                    'start': time,
                    'stop': time,
                    'score': score,
                    'bbox': pred.attrs.get('bbox')
                }
            else:
                current['stop'] = time
                current['score'] = max(current['score'], score)

        to_remove = []
        for label, clip in self.clips.items():
            if clip['stop'] != time:
                self.timeline.add_clip(
                    label, clip['start'], time, label, clip['score'], bbox=clip['bbox'])
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
