import os

import requests
import tempfile

from boonai_analysis.utils.prechecks import Prechecks
from boonflow import Argument, file_storage, proxy, clips, video, Prediction
from boonflow.analysis import LabelDetectionAnalysis
from boonflow.base import ImageInputStream
from ..custom.base import CustomModelProcessor

from PIL import Image
import numpy as np


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

    def process_image(self, frame):
        input_image = self.load_proxy_image(frame, 1)
        predictions = self.load_predictions(input_image)
        analysis = LabelDetectionAnalysis(min_score=self.min_score)

        analysis.add_predictions(predictions)
        frame.asset.add_analysis(self.app_model.module_name, analysis)

    def process(self, frame):
        pass

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

    def process(self, frame):
        asset = frame.asset
        if asset.get_attr('media.type') == "video":
            self.process_video(asset)
        else:
            self.process_image(frame)

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

    def process(self, frame):
        asset = frame.asset
        if asset.get_attr('media.type') == "video":
            self.process_video(asset)
        else:
            self.process_image(frame)

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

    def process(self, frame):
        asset = frame.asset
        if self.arg_value('text_content_field') or asset.get_attr('media.content'):
            self.process_text(frame)

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


class TorchModelImageSegmenter(TorchModelBase):

    def __init__(self):
        super(TorchModelImageSegmenter, self).__init__()
        self.image = None
        self.response_image = None
        self.asset = None
        self.label_index = self._load_label_file()
        self.colors = self._load_color_array()

    def _load_label_file(self):
        raise NotImplemented("load file from cloud storage and return a dict")

    def _load_color_array(self):
        color_array = []
        with open('../resources/colors.txt') as f:
            for line in f.read().splitlines():
                rgb = line.split(' ')
                color_array.append((int(rgb[0]), int(rgb[1]), int(rgb[2])))

        return color_array

    def _rgb_to_hex(self, rgb):
        return '#%02x%02x%02x' % rgb

    def process(self, frame):
        self.asset = frame.asset
        if frame.asset.get_attr('media.type') == "video":
            self.process_video(frame.asset)
        else:
            self.process_image(frame)

    def load_predictions(self, image):
        """
            Run prediction methods and returns a list of Prediction objects
        Args:
            image: The image that will be segmented

        Returns:
            list[Prediction]: A list of Prediction Labels containing the colors present in the image

        """
        self.image = image
        raw_predictions = self.predict(image)
        predictions = []
        for label in raw_predictions:
            predictions.append(Prediction(label[1], 1,
                                          kwargs={'color': self._rgb_to_hex(label[0])}))

        return predictions

    def predict(self, image):
        """
        Call the model to make predictions.

        Args:
            stream (IOBase): An object with a read() method that returns bytes.

        Returns:
            list: A list of tuples containing predictions

        """
        rsp = requests.post(self.endpoint, data=image.read())

        rsp.raise_for_status()

        self.response_image = rsp.json()

        return self._get_labels(rsp.json())

    def process_image(self, frame):
        super(TorchModelImageSegmenter, self).process_image(frame)
        self._segment_image(original_image=self.image, response_image=self.response_image)

    def _get_labels(self, response_image):
        response_np = np.delete(np.array(response_image), 1, 2)

        return [[self.colors[x], self.label_index[str(x)]] for x in
                np.unique(response_np).astype(np.uint8)]

    def _segment_image(self, original_image, response_image):

        response_np = np.delete(np.array(response_image), 1, 2)
        response_shape = list(response_np.shape)
        response_shape[-1] = 3

        np_colored_image = np.array(
            [self.colors[x] for x in response_np.astype(int).flatten()]) \
            .reshape(response_shape).astype(np.uint8)

        original_image_size = original_image.pil_img().size
        colored_image = Image.fromarray(np_colored_image, 'RGB').resize(original_image_size)
        colored_image_file = self._save_to_file(colored_image)

        # Create Proxy image
        self._create_proxy_image(colored_image_file, original_image_size)

    def _save_to_file(self, colored_image):
        _, path = tempfile.mkstemp(suffix='.jpg')
        colored_image.save(path)
        return path

    def _create_proxy_image(self, colored_image_file, original_shape):
        attrs = {"width": original_shape[0], "height": original_shape[1]}
        return file_storage.assets.store_file(colored_image_file, self.asset, "web-proxy",
                                              "web-proxy.jpg", attrs)
