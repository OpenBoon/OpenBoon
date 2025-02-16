import importlib

from boonai_analysis.utils.prechecks import Prechecks
from boonflow import Prediction
from boonflow import file_storage, proxy, clips, video
from boonflow.analysis import LabelDetectionAnalysis
from boonflow.proxy import get_proxy_level_path
from .base import CustomModelProcessor
from ..utils.keras import load_keras_image, load_keras_model


class TensorflowTransferLearningClassifier(CustomModelProcessor):
    """Classifier for retrained saved model """

    def __init__(self):
        super(TensorflowTransferLearningClassifier, self).__init__()

        self.trained_model = None
        self.train_args = {}
        self.labels = None
        self.preprocess_func = None

    def init(self):
        """Init constructor """
        # get model by model id
        self.load_app_model()

        # unzip and extract needed files for trained model and labels
        model_path = self.get_model_path()
        self.trained_model, self.labels = load_keras_model(model_path)
        self.train_args = self.app.models.get_training_args(self.app_model)
        self.import_preprocess_func()

    def import_preprocess_func(self):
        if 'efficientnet' in self.train_args['base_model']:
            func = getattr(importlib.import_module(
                'tensorflow.keras.applications.efficientnet'), 'preprocess_input')
        else:
            func = getattr(importlib.import_module(
                'tensorflow.keras.applications.resnet_v2'), 'preprocess_input')
        self.preprocess_func = func

    def process(self, frame):
        asset = frame.asset
        if asset.get_attr('media.type') == "video":
            self.process_video(frame.asset)
        else:
            self.process_image(frame.asset)

    def process_image(self, asset):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        Returns:
            None
        """
        # This is set for tests only
        max_preds = self.context.settings.get('maxPredictions')
        proxy_path = get_proxy_level_path(asset, 1)
        predictions = self.predict(proxy_path)

        analysis = LabelDetectionAnalysis(min_score=0.15, max_predictions=max_preds)
        for label in predictions:
            analysis.add_label_and_score(label[0], label[1])

        asset.add_analysis(self.app_model.module_name, analysis)

    def predict(self, path):
        """ Make a prediction for an image path

        Args:
            path (str): image path

        Returns:
            List[tuple]: result is list of tuples in format [(label, score),
            (label, score)]
        """
        img = load_keras_image(path)
        # get predictions
        proba = self.trained_model.predict(self.preprocess_func(img))[0]
        # create list of tuples for labels and prob scores
        result = [*zip(self.labels, proba)]
        return result

    def process_video(self, asset):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        Returns:
            None
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
        """Set up ClipTracker and Asset Detection Analysis

        Args:
            extractor: ShotBasedFrameExtractor
            clip_tracker: ClipTracker

        Returns:
            (tuple): asset detection analysis, clip_tracker
        """
        analysis = LabelDetectionAnalysis(collapse_labels=True,
                                          min_score=0.15,
                                          save_pred_attrs=False)

        for time_ms, path in extractor:
            results = [Prediction(r[0], r[1]) for r in self.predict(path)]
            clip_tracker.append_predictions(time_ms, results)
            for p in results:
                analysis.add_prediction(p)

        return analysis, clip_tracker
