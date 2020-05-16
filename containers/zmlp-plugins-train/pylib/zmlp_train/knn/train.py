import os
import tempfile
import pickle

import zmlp
from zmlpsdk import AssetProcessor, Argument, ZmlpFatalProcessorException
from ..utils.models import upload_model_directory, download_dataset

import numpy as np
from sklearn.neighbors import KNeighborsClassifier


class KnnFaceRecognitionTrainer(AssetProcessor):
    file_types = None

    def __init__(self):
        super(KnnFaceRecognitionTrainer, self).__init__()
        
        # These are the base args
        self.add_arg(Argument("model_id", "str", required=True,
                              toolTip="The model Id"))

        self.app_model = None
        self.base_dir = None

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))
        self.base_dir = tempfile.mkdtemp('knn_face_recognition')

    @staticmethod
    def num_hashes(detections):
        # Take a list of detections, return a numpy array with the hashes
        data = []
        labels = []
        i = 0
        for f in detections:
            num_hash = []
            hash = f['simhash']
            for char in hash:
                num_hash.append(ord(char))
            data.append(num_hash)
            labels.append(f['label'])
            i += 1

        x = np.asarray(data, dtype=np.float64)
        y = np.asarray(labels)

        return x, y

    def process(self, frame):
        self.reactor.write_event("status", {
            "status": "Searching Dataset Labels"
        })
        query = {
            'size': 100,
            'query': {
                'nested': {
                    'path': 'labels',
                    'query': {
                        'term': {'labels.dataSetId': self.app_model.dataset_id}
                    }
                }
            }
        }

        face_model = []
        for num, asset in enumerate(self.app.assets.scroll_search(query, timeout='5m')):
            print(num)
            for label in asset['labels']:
                if label['dataSetId'] == self.app_model.dataset_id:
                    del (label['bbox'])
                    del (label['dataSetId'])
                    face_model.append(label)

        self.reactor.write_event("status", {
            "status": "Training model{}".format(self.app_model.file_id)
        })

        if face_model:
            x_train, y_train = self.num_hashes(face_model)
            classifier = KNeighborsClassifier(n_neighbors=1, p=1, weights='distance', metric='manhattan')
            classifier.fit(x_train, y_train)
        else:
            classifier = None

        self.publish_model(classifier)

    def publish_model(self, classifier):
        """
        Publishes the trained model and a Pipeline Module which uses it.

        Args:
            labels (list): An array of labels in the correct order.

        """
        self.logger.info('publishing model')
        model_dir = tempfile.mkdtemp() + '/' + self.app_model.name
        os.makedirs(model_dir)

        self.logger.info('saving model : {}'.format(model_dir))

        with open(model_dir + '/face_classifier.pickle', 'wb') as fp:
            pickle.dump(classifier, fp)

        # Upload the zipped model to project storage.
        self.logger.info('uploading model')

        self.reactor.write_event("status", {
            "status": "Uploading model{}".format(self.app_model.file_id)
        })

        upload_model_directory(model_dir, self.app_model.file_id)

        self.app.models.publish_model(self.app_model)
        self.reactor.write_event("status", {
            "status": "Published model {}".format(self.app_model.file_id)
        })
