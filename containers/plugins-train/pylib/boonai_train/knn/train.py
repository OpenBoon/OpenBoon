import os
import pickle
import tempfile

import numpy as np
from sklearn.cluster import KMeans
from sklearn.metrics import pairwise_distances_argmin_min
from sklearn.neighbors import KNeighborsClassifier

import boonsdk
from boonflow import ModelTrainer, Argument, file_storage


class KnnLabelDetectionTrainer(ModelTrainer):
    file_types = None

    def __init__(self):
        super(KnnLabelDetectionTrainer, self).__init__()
        self.add_arg(Argument("n_clusters", "int", required=False,
                              default=15, toolTip="Number of Clusters"))

    def init(self):
        self.load_app_model()

    def train(self):

        classifier_hashes = self.classifier_hashes()

        # If there's no labels for this model, we cluster, find centroids, and make labels
        if not classifier_hashes:
            # This is how many points we will cluster. The search is randomized in order
            # to get a representative sampling of the assets.
            n_clusters = self.arg_value('n_clusters')
            self.reactor.emit_status("No labeled assets - pre-clustering")
            query = {
                'size': 100,
                '_source': ['analysis.boonai-image-similarity.*'],
                'query': {
                    'function_score': {
                        'query': {'exists': {'field': 'analysis.boonai-image-similarity.simhash'}},
                        'random_score': {}
                    }
                }
            }

            assets = []
            hashes = []
            n_points = 5000
            count = 0
            for asset in self.app.assets.scroll_search(query):
                num_hash = []
                shash = asset['analysis']['boonai-image-similarity']['simhash']
                if shash is not None:
                    for char in shash:
                        num_hash.append(ord(char))
                    hashes.append(num_hash)
                    assets.append(asset)
                    count += 1
                    if count >= n_points:
                        break

            x = np.asarray(hashes, dtype=np.float64)

            if not hashes:
                self.logger.warning("No similarity hashes found. Can't pre-cluster.")
                return

            status = "Clustering {} points".format(n_points)
            self.reactor.emit_status(status)
            kmeans = KMeans(n_clusters=n_clusters, random_state=0).fit(x)
            closest, _ = pairwise_distances_argmin_min(kmeans.cluster_centers_, x)

            # Auto-make a DS if needed.
            if not self.app_model.dataset_id:
                ds = self.app.datasets.create_dataset(
                    self.app_model.name + '-dataset',
                    boonsdk.DatasetType.Classification, description='Auto-label dataset')
            else:
                ds = self.app.datasets.get_dataset(self.app_model.dataset_id)

            for n, i in enumerate(closest):
                self.app.assets.update_labels(
                    assets[i].id,
                    ds.make_label(f'auto group {n}'))

            classifier_hashes = self.classifier_hashes()

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
            simhash = f['simhash']
            for char in simhash:
                num_hash.append(ord(char))
            data.append(num_hash)
            labels.append(f['label'])
            i += 1

        x = np.asarray(data, dtype=np.float64)
        y = np.asarray(labels)

        return x, y

    def classifier_hashes(self):
        query = {
            '_source': ['labels.*', 'analysis.boonai-image-similarity.*'],
            'size': 50,
            'query': {
                'bool': {
                    'must': [
                        {'exists': {'field': 'analysis.boonai-image-similarity.simhash'}}
                    ],
                    'filter': [{
                        'nested': {
                            'path': 'labels',
                            'query': {
                                'bool': {
                                    'must': [
                                        {'term': {'labels.datasetId': self.app_model.dataset_id}},
                                        {'term': {'labels.scope': 'TRAIN'}},
                                    ]
                                }
                            }
                        }
                    }]
                }

            }
        }

        classifier_hashes = []
        for asset in self.app.assets.scroll_search(query):
            for label in asset['labels']:
                if label['datasetId'] == self.app_model.dataset_id:
                    simhash = asset.get_attr('analysis.boonai-image-similarity.simhash')
                    if simhash is None:
                        continue
                    classifier_hashes.append({'simhash': simhash, 'label': label['label']})

        return classifier_hashes

    def publish_model(self, classifier):
        """
        Publish the model.

        Args:
            classifier (KNeighborsClassifier): The Kmeans classificer instance.

        Returns:
            AnalysisModule: The published Pipeline Module.
        """
        self.reactor.emit_status('Saving model: {}'.format(self.app_model.name))
        model_dir = os.path.join(tempfile.mkdtemp(), self.app_model.name)
        os.makedirs(model_dir, exist_ok=True)

        with open(os.path.join(model_dir, 'knn_classifier.pickle'), 'wb') as fp:
            pickle.dump(classifier, fp)

        pmod = file_storage.models.save_model(model_dir, self.app_model, self.tag, self.post_action)
        self.reactor.emit_status("Published model {}".format(self.app_model.name))
        return pmod
