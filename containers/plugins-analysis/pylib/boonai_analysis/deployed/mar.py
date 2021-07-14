import os

import requests

from boonai_analysis.utils.prechecks import Prechecks
from boonflow import Argument, file_storage, proxy, clips, video, Prediction
from boonflow.analysis import LabelDetectionAnalysis
from boonsdk.client import FileInputStream
from ..custom.base import CustomModelProcessor


class TorchModelArchiveClassifier(CustomModelProcessor):

    def __init__(self):
        super(TorchModelArchiveClassifier, self).__init__()
        self.add_arg(Argument("endpoint", "str", required=True))
        self.add_arg(Argument("model", "str", default="model1"))
        self.endpoint = None

    def init(self):
        """Init constructor """
        # get model by model id
        self.load_app_model()
        self.endpoint = os.path.join(
            self.arg_value('endpoint'), 'predictions', self.arg_value('model'))

    def process(self, frame):
        asset = frame.asset
        if asset.get_attr('media.type') == "video":
            self.process_video(asset)
        else:
            self.process_image(frame)

    def process_image(self, frame):
        asset = frame.asset

        input_image = self.load_proxy_image(frame, 1)
        predictions = self.predict(input_image)

        analysis = LabelDetectionAnalysis(min_score=self.min_score)
        for label in predictions:
            analysis.add_label_and_score(label[0], label[1])

        asset.add_analysis(self.app_model.module_name, analysis)

    def predict(self, stream):
        """
        Call the model to make predictions.

        Args:
            stream (IOBase): An object with a read() method that returns bytes.

        Returns:
            list: A list of tuples containing predictions

        """
        rsp = requests.post(self.endpoint, data=stream)
        rsp.raise_for_status()

        return [(k, v) for k, v in rsp.json().items()]

    def process_video(self, asset):
        """
        Process a video asset.

        Args:
            asset (Asset): An Asset instance.
        """
        asset_id = asset.id
        final_time = asset.get_attr('media.length')

        if not Prechecks.is_valid_video_length(asset):
            return

        video_proxy = proxy.get_video_proxy(asset)
        if not video_proxy:
            self.logger.warning(f'No video could be found for {asset_id}')
            return

        local_path = file_storage.localize_file(video_proxy)

        extractor = video.ShotBasedFrameExtractor(local_path)
        clip_tracker = clips.ClipTracker(asset, self.app_model.module_name)
        analysis, clip_tracker = self.set_analysis(extractor, clip_tracker)
        asset.add_analysis(self.app_model.module_name, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        video.save_timeline(asset, timeline)

    def set_analysis(self, extractor, clip_tracker):
        """ Set up ClipTracker and Asset Detection Analysis

        Args:
            extractor: ShotBasedFrameExtractor
            clip_tracker: ClipTracker

        Returns:
            (tuple): asset detection analysis, clip_tracker
        """
        analysis = LabelDetectionAnalysis(collapse_labels=True, min_score=self.min_score)

        for time_ms, path in extractor:
            results = [Prediction(r[0], r[1]) for r in self.predict(FileInputStream(path, 'rb'))]
            clip_tracker.append_predictions(time_ms, results)
            analysis.add_predictions(results)
        return analysis, clip_tracker


class TorchModelObjectDetection(TorchModelArchiveClassifier):

    def __init__(self):
        super(TorchModelObjectDetection, self).__init__()

    def process_image(self, frame):
        input_image = self.load_proxy_image(frame, 1)
        predictions = self.predict(input_image)
        analysis = LabelDetectionAnalysis(min_score=self.min_score)
        for label in predictions:
            analysis.add_prediction(Prediction(label[0], label[1], bbox=label[2]))

        frame.asset.add_analysis(self.app_model.module_name, analysis)

    def predict(self, stream):
        """
        Call the model to make predictions.

        Args:
            stream (IOBase): An object with a read() method that returns bytes.

        Returns:
            list: A list of tuples containing predictions

        """
        rsp = requests.post(self.endpoint, data=stream)
        rsp.raise_for_status()

        preds = []
        for pred in rsp.json():
            label = list(pred.keys())[0]
            score = pred['score']
            bbox = pred[label]
            preds.append((label, score, bbox))

        return preds
