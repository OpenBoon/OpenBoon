import os
import pickle
import tempfile
import zipfile

import numpy as np

from zmlpsdk import AssetProcessor, Argument
from zmlpsdk.storage import file_storage
from zmlpsdk.analysis import LabelDetectionAnalysis


class KnnLabelDetectionClassifier(AssetProcessor):
    def __init__(self):
        super(KnnLabelDetectionClassifier, self).__init__()

        self.add_arg(Argument("model_id", "str", required=True, toolTip="The model Id"))
        self.add_arg(Argument("sensitivity", "int", default=1000,
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

        analysis = LabelDetectionAnalysis()
        min_distance = self.arg_value('sensitivity')
        if dist[0][0] < min_distance:
            analysis.add_label_and_score(prediction[0], dist[0][0])
        else:
            analysis.add_label_and_score('Unrecognized', dist[0][0])

        asset.add_analysis(self.app_model.name, analysis)

    def load_model(self):
        """
        Load the model.

        Returns:
            KNeighborsClassifier: The model.
        """
        model_zip = file_storage.projects.localize_file(self.app_model.file_id)
        with zipfile.ZipFile(model_zip) as zfp:
            zfp.extractall(path=tempfile.tempdir)
        with open(os.path.join(tempfile.tempdir, 'model', 'knn_classifier.pickle'), 'rb') as fp:
            classifier = pickle.load(fp)
        return classifier

    @staticmethod
    def hashes_as_nparray(hash):
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
