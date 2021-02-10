from tensorflow.keras.applications.resnet_v2 import preprocess_input

from zmlp_analysis.utils.prechecks import Prechecks
from zmlpsdk import AssetProcessor, Argument, file_storage
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path
from zmlpsdk.video import ShotBasedFrameExtractor, save_timeline
from zmlpsdk.clips import ClipTracker
from zmlpsdk.proxy import get_video_proxy
from ..utils.keras import load_keras_image, load_keras_model


class TensorflowImageClassifier(AssetProcessor):
    """A processor for loading and executing a uploaded Tensorflow image classifier"""

    def __init__(self):
        super(TensorflowImageClassifier, self).__init__()

        self.add_arg(Argument("model_id", "str", required=True, toolTip="The model Id"))
        self.add_arg(Argument("input_size", "list", required=True,
                              toolTip="The input size", default=(224, 224)))

        self.app_model = None
        self.trained_model = None
        self.labels = None
        self.extract_type = "shot"

    def init(self):
        """Init constructor """
        # get model by model id
        self.app_model = self.app.models.get_model(self.arg_value("model_id"))

        # unzip and extract needed files for trained model and labels
        self.trained_model, self.labels = load_keras_model(self.app_model)

    def process(self, frame):
        asset = frame.asset
        if asset.get_attr('media.type') == "video":
            self.process_video(frame.asset)
        else:
            self.process_image(frame.asset)

    def process_image(self, asset):
        """Process an image/document.

        Args:
            frame (Frame): Frame to be processed

        Returns:
            None
        """
        proxy_path = get_proxy_level_path(asset, 1)
        predictions = self.predict(proxy_path)

        analysis = LabelDetectionAnalysis(min_score=0.01)
        for label in predictions:
            analysis.add_label_and_score(label[0], label[1])

        asset.add_analysis(self.app_model.module_name, analysis)

    def process_video(self, asset):
        """Process a video file.

        Args:
            frame (Frame): Frame to be processed
        """
        asset_id = asset.id
        final_time = asset.get_attr('media.length')

        if not Prechecks.is_valid_video_length(asset):
            return

        video_proxy = get_video_proxy(asset)
        if not video_proxy:
            self.logger.warning(f'No video could be found for {asset_id}')
            return

        local_path = file_storage.localize_file(video_proxy)

        extractor = ShotBasedFrameExtractor(local_path)
        clip_tracker = ClipTracker(asset, self.app_model.module_name)
        analysis, clip_tracker = self.build_analysis(extractor, clip_tracker)
        asset.add_analysis(self.app_model.module_name, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(asset, timeline)

    def predict(self, path):
        """ Make a prediction for an image path

        Args:
            path (str): image path

        Returns:
            List[tuple]: result is list of tuples in format [(label, score),
            (label, score)]
        """
        size = self.arg_value("input_size") or (224, 224)
        img = load_keras_image(path, size=size)
        proba = self.trained_model.predict(preprocess_input(img))[0]
        # create list of tuples for labels and prob scores
        result = [*zip(self.labels, proba)]
        return result

    def build_analysis(self, extractor, clip_tracker):
        """ Set up ClipTracker and Asset Detection Analysis

        Args:
            extractor: ShotBasedFrameExtractor
            clip_tracker: ClipTracker

        Returns:
            (tuple): asset detection analysis, clip_tracker
        """
        analysis = LabelDetectionAnalysis(collapse_labels=True, min_score=0.01)

        for time_ms, path in extractor:
            clip_tracker.append(time_ms, self.labels)
            results = self.predict(path)
            [analysis.add_label_and_score(r[0], r[1]) for r in results]

        return analysis, clip_tracker
