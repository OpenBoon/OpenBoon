import os
import pickle

from boonai_analysis.utils.prechecks import Prechecks
from boonai_analysis.utils.simengine import SimilarityEngine
from boonflow import Argument, LabelDetectionAnalysis, Prediction
from boonflow.clips import ClipTracker
from boonflow.proxy import get_video_proxy
from boonflow.storage import file_storage
from boonflow.video import ShotBasedFrameExtractor, save_timeline
from .base import CustomModelProcessor


class KnnLabelDetectionClassifier(CustomModelProcessor):
    def __init__(self):
        super(KnnLabelDetectionClassifier, self).__init__()
        self.add_arg(Argument("sensitivity", "int", default=10000,
                              toolTip="How sensitive the model is to differences."))

        self.classifier = None
        self.labels = None
        self.simengine = None

    def init(self):
        self.load_app_model()
        self.classifier = self.load_model()
        self.simengine = SimilarityEngine()

    def process(self, frame):
        asset = frame.asset
        if asset.get_attr('media.type') == "video":
            self.process_video(frame.asset)
        else:
            self.process_image(frame.asset)

    def process_image(self, asset):
        simhash = asset.get_attr('analysis.boonai-image-similarity.simhash')
        if not simhash:
            return

        pred = self.predict(simhash)
        analysis = LabelDetectionAnalysis()
        analysis.add_prediction(pred)
        asset.add_analysis(self.app_model.module_name, analysis)

    def process_video(self, asset):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            asset (Asset): Asset to be processed

        """
        if not Prechecks.is_valid_video_length(asset):
            return

        final_time = asset.get_attr('media.length')
        video_proxy = get_video_proxy(asset)
        if not video_proxy:
            self.logger.warning(f'No video could be found for {asset.id}')
            return

        local_path = file_storage.localize_file(video_proxy)
        # Extract a frame every 1 second.
        extractor = ShotBasedFrameExtractor(local_path)
        clip_tracker = ClipTracker(asset, self.app_model.module_name)
        analysis = LabelDetectionAnalysis(save_pred_attrs=False, collapse_labels=True)

        for time_ms, path in extractor:
            simraw = self.simengine.calculate_nparray_hash(path)
            pred = self.predict(simraw)
            clip_tracker.append_predictions(time_ms, [pred])
            analysis.add_prediction(pred)

        timeline = clip_tracker.build_timeline(final_time)
        save_timeline(asset, timeline)
        asset.add_analysis(self.app_model.module_name, analysis)

    def predict(self, simhash):
        """
        Make a prediction using a simhash
        Args:
            simhash (str): The simhash

        Returns:
            Prediction: The prediction made.

        """
        if isinstance(simhash, str):
            inthash = self.simengine.hash_as_nparray(simhash)
        else:
            inthash = simhash

        prediction = self.classifier.predict([inthash])
        dist, ind = self.classifier.kneighbors([inthash], n_neighbors=1, return_distance=True)

        min_distance = self.arg_value('sensitivity')
        dist_result = dist[0][0]

        if dist_result < min_distance:
            prediction = Prediction(prediction[0], 1 - dist_result / min_distance)
        else:
            prediction = Prediction('Unrecognized', 0.0)
        return prediction

    def load_model(self):
        """
        Load the model.

        Returns:
            KNeighborsClassifier: The model.
        """
        model_path = self.get_model_path()
        with open(os.path.join(model_path, 'knn_classifier.pickle'), 'rb') as fp:
            classifier = pickle.load(fp)
        return classifier
