import os
import tempfile
import pickle

from zmlpsdk import AssetProcessor, Argument
from ..utils.models import upload_model_directory

import numpy as np
from sklearn.neighbors import KNeighborsClassifier


class KnnLabelDetectionTrainer(AssetProcessor):
    def __init__(self):
        super(KnnLabelDetectionTrainer, self).__init__()
        self.add_arg(Argument("model_id", "str", required=True, toolTip="The model Id"))
        self.app_model = None

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))

    def process(self, frame):
        self.reactor.emit_status("Searching DataSet Labels")
        query = {
            '_source': 'labels.*',
            'size': 25,
            'query': {
                'nested': {
                    'path': 'labels',
                    'query': {
                        'term': {'labels.dataSetId': self.app_model.dataset_id}
                    }
                }
            }
        }

        classifier_hashes = []
        for asset in self.app.assets.scroll_search(query):
            for label in asset['labels']:
                if label['dataSetId'] == self.app_model.dataset_id:
                    classifier_hashes.append({'simhash': asset.get_attr(
                            'analysis.zvi-image-similarity.simhash'),
                            'label': label['label']})

        if not classifier_hashes:
            self.logger.warning("No labeled assets")
            return

        status = "Training knn classifier {} with {} points".format(
            self.app_model.name, len(classifier_hashes))

        self.reactor.emit_status(status)
        x_train, y_train = self.num_hashes(classifier_hashes)
        classifier = KNeighborsClassifier(
            n_neighbors=1, p=1, weights='distance', metric='manhattan')
        classifier.fit(x_train, y_train)
        self.publish_model(classifier)

    @staticmethod
    def num_hashes(hashes):
        """
        Take a list of detections, return a numpy array with the hashes

        Args:
            hashes (list): A list of hashes and labels

        Returns:
            array: a tuple of NP array containg the hashes and labels.
        """
        data = []
        labels = []
        i = 0
        for f in hashes:
            num_hash = []
            simhah = f['simhash']
            for char in simhah:
                num_hash.append(ord(char))
            data.append(num_hash)
            labels.append(f['label'])
            i += 1

        x = np.asarray(data, dtype=np.float64)
        y = np.asarray(labels)

        return x, y

    def publish_model(self, classifier):
        """
        Publish the model.

        Args:
            classifier (KNeighborsClassifier): The Kmeans classificer instance.

        Returns:
            PipelineMod: The published Pipeline Module.
        """
        self.logger.info('publishing model')
        model_dir = os.path.join(tempfile.mkdtemp(), "model_knn")
        os.makedirs(model_dir, exist_ok=True)

        with open(os.path.join(model_dir, 'knn_classifier.pickle'), 'wb') as fp:
            pickle.dump(classifier, fp)

        upload_model_directory(model_dir, self.app_model.file_id)

        pmod = self.app.models.publish_model(self.app_model)
        self.reactor.emit_status("Published model {}".format(self.app_model.name))
        return pmod
