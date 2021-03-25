import os
import pickle

import numpy as np

from boonflow import Argument, Prediction
from boonflow.analysis import LabelDetectionAnalysis
from boonai_analysis.utils.prechecks import Prechecks
from boonflow import file_storage, proxy, clips, video
from boonai_analysis.boonai.faces import MtCnnFaceDetectionEngine
from .base import CustomModelProcessor


class KnnFaceRecognitionClassifier(CustomModelProcessor):

    def __init__(self):
        super(KnnFaceRecognitionClassifier, self).__init__()
        self.add_arg(Argument("sensitivity", "int", default=1200,
                              toolTip="How sensitive the model is to differences."))

        self.face_classifier = None
        self.labels = None
        self.detect_engine = None

    def init(self):
        self.load_app_model()
        self.face_classifier = self.load_model()
        self.detect_engine = MtCnnFaceDetectionEngine()

    def process(self, frame):
        asset = frame.asset
        if asset.get_attr('media.type') == "video":
            self.process_video(frame.asset)
        else:
            self.process_image(frame.asset)

    def process_image(self, asset):
        faces = asset.get_attr('analysis.boonai-face-detection.predictions')
        if not faces:
            return

        analysis = LabelDetectionAnalysis(min_score=0)
        for pred in self.get_rec_predictions(faces):
            analysis.add_prediction(pred)
        asset.add_analysis(self.app_model.module_name, analysis)

    def load_model(self):
        """
        Load the model.

        Returns:
            KNeighborsClassifier: The model.
        """
        model_path = self.get_model_path()
        with open(os.path.join(model_path, 'face_classifier.pickle'), 'rb') as fp:
            face_classifier = pickle.load(fp)
        return face_classifier

    def get_rec_predictions(self, faces):
        """
        Get a list or recognition predictions from a list of faces.

        Args:
            faces (list): A list of known face detections.

        Returns:
            list: A list of Prediction instances.
        """
        result = []
        x = self.hashes_as_nparray(faces)
        predictions = self.face_classifier.predict(x)
        dist, ind = self.face_classifier.kneighbors(x, n_neighbors=1, return_distance=True)

        min_distance = self.arg_value('sensitivity')
        for i, face in enumerate(faces):
            if dist[i][0] < min_distance:
                label = predictions[i]
                score = 1.0 - max(0, min(1, (dist[i][0] - 200) / (min_distance - 200)))
            else:
                label = 'Unrecognized'
                score = 0
            result.append(Prediction(label, score, bbox=face['bbox']))
        return result

    @staticmethod
    def hashes_as_nparray(detections):
        """
        Convert the face hashes into a NP array so they can be compared
        to the ones in the model.

        Args:
            detections (list): List of face detection.

        Returns:
            nparray: Array of simhashes as a NP array.
        """
        data = []
        i = 0
        for f in detections:
            sim = f['simhash']
            num_hash = [ord(char) for char in sim]
            data.append(num_hash)
            i += 1

        return np.asarray(data, dtype=np.float64)

    def process_video(self, asset):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            asset (Asset): Asset to be processed

        """
        if not Prechecks.is_valid_video_length(asset):
            return

        final_time = asset.get_attr('media.length')
        video_proxy = proxy.get_video_proxy(asset)
        if not video_proxy:
            self.logger.warning(f'No video could be found for {asset.id}')
            return

        analysis = LabelDetectionAnalysis(
            min_score=0, collapse_labels=True, save_pred_attrs=False)

        local_path = file_storage.localize_file(video_proxy)
        # Extract a frame every 1 second.
        extractor = video.TimeBasedFrameExtractor(local_path, 1)
        clip_tracker = clips.ClipTracker(asset, self.app_model.module_name)

        for time_ms, path in extractor:
            detected_faces = self.detect_engine.detect(path)
            if detected_faces:
                recs = self.get_rec_predictions(detected_faces)
                clip_tracker.append_predictions(time_ms, recs)
                analysis.add_predictions(recs)
            else:
                clip_tracker.append_predictions(time_ms, [])

        asset.add_analysis(self.app_model.module_name, analysis)
        timeline = clip_tracker.build_timeline(final_time)
        video.save_timeline(asset, timeline)
