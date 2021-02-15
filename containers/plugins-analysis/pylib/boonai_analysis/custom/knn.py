import os
import pickle
import numpy as np

from boonflow import AssetProcessor, Argument
from boonflow.storage import file_storage
from boonflow.analysis import SingleLabelAnalysis


class KnnLabelDetectionClassifier(AssetProcessor):
    def __init__(self):
        super(KnnLabelDetectionClassifier, self).__init__()

        self.add_arg(Argument("model_id", "str", required=True, toolTip="The model Id"))
        self.add_arg(Argument("sensitivity", "int", default=10000,
                              toolTip="How sensitive the model is to differences."))

        self.app_model = None
        self.classifier = None
        self.labels = None

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))
        self.classifier = self.load_model()

    def process(self, frame):
        asset = frame.asset

        simhash = asset.get_attr('analysis.zvi-image-similarity.simhash')
        if not simhash:
            return

        x = self.hash_as_nparray(simhash)
        prediction = self.classifier.predict([x])
        dist, ind = self.classifier.kneighbors([x], n_neighbors=1, return_distance=True)

        min_distance = self.arg_value('sensitivity')
        dist_result = dist[0][0]
        if dist_result < min_distance:
            analysis = SingleLabelAnalysis(prediction[0], 1 - dist_result / min_distance)
        else:
            analysis = SingleLabelAnalysis('Unrecognized', 0.0)

        asset.add_analysis(self.app_model.module_name, analysis)

    def load_model(self):
        """
        Load the model.

        Returns:
            KNeighborsClassifier: The model.
        """

        model_path = file_storage.models.install_model(self.app_model)
        with open(os.path.join(model_path, 'knn_classifier.pickle'), 'rb') as fp:
            classifier = pickle.load(fp)
        return classifier

    @staticmethod
    def hash_as_nparray(hash):
        """
        Convert a sim hash into a NP array so they can be compared
        to the ones in the model.

        Args:
            hash (list): sim hash.

        Returns:
            nparray: simhash as a NP array.
        """

        num_hash = []
        for char in hash:
            num_hash.append(ord(char))

        return np.asarray(num_hash, dtype=np.float64)
