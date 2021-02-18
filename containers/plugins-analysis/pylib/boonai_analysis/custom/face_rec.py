import os
import pickle

import numpy as np

from boonflow import AssetProcessor, Argument
from boonflow.storage import file_storage
from boonflow.analysis import LabelDetectionAnalysis


class KnnFaceRecognitionClassifier(AssetProcessor):
    def __init__(self):
        super(KnnFaceRecognitionClassifier, self).__init__()

        self.add_arg(Argument("model_id", "str", required=True, toolTip="The model Id"))
        self.add_arg(Argument("sensitivity", "int", default=1200,
                              toolTip="How sensitive the model is to differences."))

        self.app_model = None
        self.face_classifier = None
        self.labels = None

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))
        self.face_classifier = self.load_model()

    def process(self, frame):
        asset = frame.asset

        faces = asset.get_attr('analysis.boonai-face-detection.predictions')
        if not faces:
            return

        analysis = LabelDetectionAnalysis(min_score=0.0)

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
                score = 0.0

            analysis.add_label_and_score(label, score, bbox=face["bbox"])

        asset.add_analysis(self.app_model.module_name, analysis)

    def load_model(self):
        """
        Load the model.

        Returns:
            KNeighborsClassifier: The model.
        """
        model_path = file_storage.models.install_model(self.app_model)
        with open(os.path.join(model_path, 'face_classifier.pickle'), 'rb') as fp:
            face_classifier = pickle.load(fp)
        return face_classifier

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
            num_hash = []
            hash = f['simhash']
            for char in hash:
                num_hash.append(ord(char))
            data.append(num_hash)
            i += 1

        return np.asarray(data, dtype=np.float64)
