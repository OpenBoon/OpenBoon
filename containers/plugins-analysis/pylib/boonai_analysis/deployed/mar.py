import logging
import os
import tempfile

import backoff
import numpy as np
import requests
from PIL import Image, ImageColor

from boonai_analysis.utils.prechecks import Prechecks
from boonflow import Argument, file_storage, proxy, clips, video, Prediction, FileTypes
from boonflow.analysis import LabelDetectionAnalysis
from boonflow.base import ImageInputStream
from ..custom.base import CustomModelProcessor

logger = logging.getLogger(__name__)


@backoff.on_exception(backoff.expo,
                      requests.exceptions.RequestException,
                      max_time=20)
def make_request(endpoint, stream):
    """
    Make request to the the hosted model endpoint.

    Args:
        endpoint (str): The request endpoinnt
        stream (IOBase): An IO stream to

    Returns:
        mixed: The results of the inference.

    """
    logger.info(f'Making request to {endpoint}')
    rsp = requests.post(endpoint, data=stream)
    rsp.raise_for_status()
    return rsp.json()


class TorchModelBase(CustomModelProcessor):

    def __init__(self):
        super(TorchModelBase, self).__init__()
        self.add_arg(Argument("endpoint", "str", required=True))
        self.add_arg(Argument("endpoint_path", "str", required=False))
        self.endpoint = None
        self.endpoint_path = None

    def init(self):
        """Init constructor """
        # get model by model id
        self.load_app_model()
        self.endpoint_path = self.arg_value("endpoint_path") or '/predictions/model1'
        self.endpoint = self.arg_value('endpoint') + self.endpoint_path

    def process_image(self, frame):
        input_image = self.load_proxy_image(frame, 1)

        predictions = self.load_predictions(input_image, frame.asset)
        analysis = LabelDetectionAnalysis(min_score=self.min_score)
        analysis.add_predictions(predictions)

        frame.asset.add_analysis(self.app_model.module_name, analysis)

    def process(self, frame):
        pass

    def load_predictions(self, input_image, asset=None):
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

    def load_predictions(self, input_file, asset=None):
        """
            Run prediction methods and returns a list of Prediction objects
        Args:
            input_file: An object with a read() method that returns bytes.
            asset: Asset that can be used for further processing
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
        result = make_request(self.endpoint, stream)
        return [(k, v) for k, v in result.items()]


class TorchModelArchiveDetector(TorchModelBase):

    def __init__(self):
        super(TorchModelArchiveDetector, self).__init__()

    def process(self, frame):
        asset = frame.asset
        if asset.get_attr('media.type') == "video":
            self.process_video(asset)
        else:
            self.process_image(frame)

    def load_predictions(self, input_image, asset=None):
        """
            Run prediction methods and returns a list of Prediction objects
        Args:
            input_image: An object with a read() method that returns bytes.
            asset: Asset that can be used for further processing
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
        result = make_request(self.endpoint, stream)

        preds = []
        for pred in result:
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
        self.field = None

    def init(self):
        super(TorchModelTextClassifier, self).init()
        train_args = self.app.models.get_training_args(self.app_model)
        self.field = train_args.get("field") or "media.content"

    def process(self, frame):
        self.process_text(frame)

    def load_predictions(self, text, asset=None):
        """
            Run prediction methods and returns a list of Prediction objects
        Args:
            text: A String that will have the content predicted.
            asset: Asset that can be used for further processing

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
        text = asset.get_attr(self.field)
        if not text:
            return

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
        result = make_request(self.endpoint, stream)
        return [(k, v) for k, v in result.items()]


class TorchModelImageSegmenter(CustomModelProcessor):

    # Not supporting video.
    file_types = FileTypes.images | FileTypes.documents

    def __init__(self):
        super(TorchModelImageSegmenter, self).__init__()
        self.endpoint = None
        """The Torch Serve endpoint"""
        self.labels = None
        """An array of labels if set on the model."""
        self.colors = None
        """The colors for the segmentation, is either supplied by args or"""
        self.endpoint_path = '/predictions/model1'
        """The base edpoint path"""

    def init(self):
        self.load_app_model()
        self.endpoint = self.arg_value('endpoint') + self.endpoint_path

        train_args = self.app.models.get_training_args(self.app_model)
        self.labels = train_args.get('labels')
        self.colors = train_args.get('colors') or self.load_color_file()

    def process(self, frame):
        img = self.load_proxy_image(frame, 1)
        seg_img = self.make_torch_serve_request(img)

        analysis = LabelDetectionAnalysis(min_score=self.min_score)
        analysis.add_predictions(self.segmented_image_to_predictions(seg_img))
        frame.asset.add_analysis(self.app_model.module_name, analysis)

        self.save_segmented_proxy(frame.asset, img, seg_img)

    def segmented_image_to_predictions(self, seg_img):
        """
            Run prediction methods and returns a list of Prediction objects
        Args:
            seg_img: The image that will be segmented
        Returns:
            list[Prediction]: A list of Prediction Labels containing the colors present in the image

        """
        trim = np.delete(np.array(seg_img), 1, 2)
        color_label_pairs = [(self.colors[x % 500], self.get_label(x)) for x in
                             np.unique(trim).astype(np.uint8)]

        results = []
        for color, label in color_label_pairs:
            results.append(Prediction(label, 1, color=color))
        return results

    def make_torch_serve_request(self, image):
        """
        Call the model to make predictions.

        Args:
            image (IOBase): An object with a read() method that returns bytes.
        Returns:
            list: A list of tuples containing predictions

        """
        return make_request(self.endpoint, image.read())

    def get_label(self, idx):
        """
        Get a label for a particular class index.  If no labels were defined
        then return a stringified index.

        Args:
            idx (int): The class index

        Returns:
            str: The converted label.
        """
        if not self.labels:
            return str(idx)
        else:
            return self.labels[idx]

    def save_segmented_proxy(self, asset, src_img, seg_img):
        """
        Save the segmented image proxy.

        Args:
            asset (Asset): The Asset
            src_img (IOBase): The original processed image.
            seg_img (nparay): The pixesl with predictions.

        """
        response_np = np.delete(np.array(seg_img), 1, 2)
        response_shape = list(response_np.shape)
        response_shape[-1] = 3

        np_colored_image = np.array(
            [ImageColor.getcolor(self.colors[x], "RGB")
             for x in response_np.astype(int).flatten()]).reshape(response_shape).astype(np.uint8)

        src_img_size = src_img.pil_img().size
        colored_image = Image.fromarray(np_colored_image, 'RGB').resize(src_img_size)

        # Save to File
        _, img_path = tempfile.mkstemp(suffix='.jpg')
        colored_image.save(img_path)

        # Save proxy
        self.create_proxy_image(img_path, src_img_size, asset)

    def create_proxy_image(self, seg_img_path, size, asset):
        """
        Update an image segmentation proxy image.

        Args:
            seg_img_path (str): Path to the file.
            size (dict): A size dict
            asset (Asset): The Asset.

        Returns:
            StoredFile: The stored file reference
        """
        attrs = {"width": size[0], "height": size[1]}
        return file_storage.assets.store_file(seg_img_path, asset, "segment-proxy",
                                              "segment-proxy.jpg", attrs)

    def load_color_file(self):
        """
        Loads the colors from the pre-cached color file.

        Returns:
            list: A list of tuples with int RGB values
        """
        color_array = []
        path = os.path.dirname(__file__) + "/colors.txt"
        with open(path) as fp:
            for line in fp.readlines():
                line = line.strip()
                if not line:
                    continue
                color_array.append(line)
        return color_array
