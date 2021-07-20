import os

import requests

from boonai_analysis.utils.prechecks import Prechecks
from boonflow import Argument, file_storage, proxy, clips, video, Prediction
from boonflow.analysis import LabelDetectionAnalysis
from boonflow.base import ImageInputStream
from ..custom.base import CustomModelProcessor


class TorchModelBase(CustomModelProcessor):

    def __init__(self):
        super(TorchModelBase, self).__init__()
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
        elif self.arg_value('text_content_field') or asset.get_attr('media.content'):
            self.process_text(frame)
        else:
            self.process_image(frame)

    def process_text(self, frame):
        asset = frame.asset
        arg_name = 'text_content_field'

        if self.arg_value(arg_name) and asset.attr_exists(self.arg_value(arg_name)):
            text = asset.get_attr(self.arg_value(arg_name))
        else:
            text = asset.get_attr('media.content')

        if text:
            predictions = self.load_predictions(text)
            analysis = LabelDetectionAnalysis(min_score=self.min_score)

            analysis.add_predictions(predictions)
            frame.asset.add_analysis(self.app_model.module_name, analysis)

    def process_image(self, frame):
        input_image = self.load_proxy_image(frame, 1)
        predictions = self.load_predictions(input_image)
        analysis = LabelDetectionAnalysis(min_score=self.min_score)

        analysis.add_predictions(predictions)
        frame.asset.add_analysis(self.app_model.module_name, analysis)

    def load_predictions(self, input_image):
        pass

    def predict(self, stream):
        pass

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
            results = self.load_predictions(ImageInputStream.from_path(path))
            clip_tracker.append_predictions(time_ms, results)
            analysis.add_predictions(results)

        return analysis, clip_tracker


class TorchModelArchiveClassifier(TorchModelBase):
    def __init__(self):
        super(TorchModelArchiveClassifier, self).__init__()

    def load_predictions(self, input_file):
        """
            Run prediction methods and returns a list of Prediction objects
        Args:
            input_file: An object with a read() method that returns bytes.

        Returns:
            list[Prediction]: A list of Prediction objects

        """
        raw_predictions = self.predict(input_file)
        predictions = []
        for label in raw_predictions:
            predictions.append(Prediction(label[0], label[1]))

        return predictions

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


class TorchModelArchiveDetector(TorchModelBase):

    def __init__(self):
        super(TorchModelArchiveDetector, self).__init__()

    def load_predictions(self, input_image):
        """
            Run prediction methods and returns a list of Prediction objects
        Args:
            input_image: An object with a read() method that returns bytes.

        Returns:
            list[Prediction]: A list of Prediction objects

        """
        raw_predictions = self.predict(input_image)
        predictions = []
        for label in raw_predictions:
            predictions.append(Prediction(label[0], label[1], bbox=label[2]))

        return predictions

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
            keys = list(pred.keys())
            keys.remove("score")
            label = keys[0]
            score = pred['score']
            bbox = pred[label]
            preds.append((label, score, bbox))

        return preds


class TorchModelTextClassifier(TorchModelBase):

    def __init__(self):
        super(TorchModelTextClassifier, self).__init__()

    def load_predictions(self, text):
        """
            Run prediction methods and returns a list of Prediction objects
        Args:
            text: A String that will have the content predicted.

        Returns:
            list[Prediction]: A list of Prediction objects

        """
        stream = bytearray(text.encode())
        raw_predictions = self.predict(stream)
        predictions = []
        for label in raw_predictions:
            predictions.append(Prediction(label[0], label[1]))

        return predictions

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
