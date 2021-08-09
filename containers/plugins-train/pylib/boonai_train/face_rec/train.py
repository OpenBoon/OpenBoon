import os
import pickle
import tempfile

import numpy as np
from sklearn.neighbors import KNeighborsClassifier

from boonflow import ModelTrainer, file_storage, ProcessorException


class KnnFaceRecognitionTrainer(ModelTrainer):
    file_types = None

    max_detections = 100

    label_reqs = ['label', 'simhash']

    def __init__(self):
        super(KnnFaceRecognitionTrainer, self).__init__()

    def init(self):
        self.load_app_model()

    def train(self):
        self.reactor.write_event("status", {
            "status": "Searching for labels"
        })
        query = {
            '_source': 'labels.*',
            'size': 50,
            'query': {
                'nested': {
                    'path': 'labels',
                    'query': {
                        'bool': {
                            'must': [
                                {'term': {'labels.datasetId': self.app_model.dataset_id}},
                                {'term': {'labels.scope': 'TRAIN'}}
                            ]
                        }
                    }
                }
            }
        }

        face_model = []
        for asset in self.app.assets.scroll_search(query):
            for label in asset['labels']:
                # It's possible to still get other datasets, so skip ones
                # that are not in this dataset.
                if label['datasetId'] != self.app_model.dataset_id:
                    continue
                self.check_valid_label(asset, label)
                face_model.append({'simhash': label['simhash'], 'label': label['label']})

        if not face_model:
            raise ProcessorException("No labeled faces")

        status = "Training face model {} with {} faces".format(
            self.app_model.name, len(face_model))

        self.reactor.emit_status(status)
        x_train, y_train = self.num_hashes(face_model)
        classifier = KNeighborsClassifier(
            n_neighbors=1, p=1, weights='distance', metric='manhattan')
        classifier.fit(x_train, y_train)
        self.publish_model(classifier)

    def check_valid_label(self, asset, label):
        """
        Checks to see if our label has the right properties.  Throws if it does not.

        Args:
            asset (Asset): The Asset.
            label (dict): The label
        """
        for prop in self.label_reqs:
            if prop not in label:
                raise ProcessorException(f'Invalid label on {asset.id}, '
                                         f'missing a property: {prop}')

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
            classifier (KNeighborsClassifier): The Kmeans classifier instance.

        Returns:
            AnalysisModule: The published Pipeline Module.
        """
        self.logger.info('publishing model')
        model_dir = os.path.join(tempfile.mkdtemp(), "model")
        os.makedirs(model_dir, exist_ok=True)

        with open(os.path.join(model_dir, 'face_classifier.pickle'), 'wb') as fp:
            pickle.dump(classifier, fp)

        mod = file_storage.models.save_model(model_dir, self.app_model, self.tag, self.post_action)
        self.reactor.emit_status("Published model {}".format(self.app_model.name))

        return mod
