from io import BytesIO

import cv2
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from sklearn.metrics import confusion_matrix


class ConfusionMatrix(object):
    """Represents a confusion matrix for a model.

    Upon creation this object stores all of the information required to render a
    confusion matrix showing the accuracy of a Model.

    Args:
        model(zmlp.Model): Model to evaluate a confusion matrix for.
        app(zmlp.ZmlpApp): ZMLP app to use for fetching data.
        test_set_only(bool): If True the confusion matrix is calculate for the TEST set
         only. If False, all assets will be evaluated.
        min_score(float): Minimum confidence score to filter by.
        max_score(float): Maximum confidence score to filter by.

    """
    def __init__(self, model, app, test_set_only=True, min_score=0.0, max_score=1.0):
        self.model = model
        self.app = app
        self.min_score = min_score
        self.max_score = max_score
        self.test_set_only = test_set_only
        self._labels = None
        self._data_frame = None

    @property
    def labels(self):
        if not self._labels:
            self._data_frame, self._labels = self.__get_data_frame_and_labels()
        return self._labels

    @property
    def data_frame(self):
        if self._data_frame is None:
            self._data_frame, self._labels = self.__get_data_frame_and_labels()
        return self._data_frame

    @property
    def accuracy(self):
        matrix = self.get_matrix()
        accuracy = np.sum(np.diag(matrix)) / np.sum(matrix)
        if np.isnan(accuracy):
            accuracy = 0.0
        return float(accuracy)

    def get_matrix(self, normalize=False):
        if normalize:
            normalize = 'true'
        else:
            normalize = None
        true_list = list(self.data_frame['True'])
        prediction_list = list(self.data_frame['Predicted'])
        count_list = list(self.data_frame['Number'])
        return confusion_matrix(true_list, prediction_list,
                                sample_weight=count_list, normalize=normalize)

    def __get_data_frame_and_labels(self):
        """Gets a pandas data frame and a list of labels that can be used to build the
        confusion matrixs.

        Args:
            aggs(dict): Aggregations from the ES confusion matrix query.

        """
        aggs = self.__get_confusion_matrix_aggregations()
        buckets = aggs['nested#nested_labels']['filter#model_train_labels']['sterms#labels']['buckets']
        data = []
        labels = set()
        for label_bucket in buckets:
            truth = label_bucket['key']
            for prediction_bucket in label_bucket['reverse_nested#predictions_by_label'][
                        'sterms#predictions'][
                        'buckets']:
                prediction = prediction_bucket['key']
                labels.add(prediction)
                count = prediction_bucket['doc_count']
                data.append((truth, prediction, count))
        labels = list(labels)
        labels.sort()
        return pd.DataFrame(data, columns=['True', 'Predicted', 'Number']), labels

    def __get_confusion_matrix_aggregations(self):
        """Testing seam to allow mocking calls to ZMLP. Performs an ES search with
        aggregations needed to build the confusion matrix.

        """
        search = self.model.get_confusion_matrix_search(test_set_only=self.test_set_only,
                                                        min_score=self.min_score,
                                                        max_score=self.max_score)
        search_results = self.app.assets.search(search)
        return search_results.aggregations()

    def create_thumbnail_image(self):
        """Renders a thumbnail image of the confusion matrix and saves it to disk."""
        plt.figure(figsize=(8, 6))
        imshow = plt.imshow(self.get_matrix(), cmap=plt.cm.Blues)
        image_data = imshow.make_image(plt.gcf().canvas.get_renderer(), magnification=2.0)[0]
        image_data = cv2.cvtColor(image_data, cv2.COLOR_BGR2RGB)
        image_data = cv2.resize(image_data, (500, 500))
        retval, buffer = cv2.imencode('.png', image_data)
        _file = BytesIO(buffer)
        return _file

    def to_dict(self, normalize_matrix=False):
        """Returns a dictionary representation suitable for json."""
        return {"name": self.model.name,
                "moduleName": self.model.module_name,
                "overallAccuracy": self.accuracy,
                "labels": self.labels,
                'minScore': self.min_score,
                'maxScore': self.max_score,
                'testSetOnly': self.test_set_only,
                "matrix": self.get_matrix(normalize_matrix).tolist(),
                "isMatrixApplicable": True}

    def show(self):
        """Display the Confusion Matrix. For interactive use only."""
        fig = plt.figure()
        ax = fig.add_subplot(111)
        cax = ax.matshow(self.get_matrix(), cmap=plt.cm.Blues)
        plt.title(f'Confusion matrix of the {self.model.name}')
        fig.colorbar(cax)
        ax.set_xticklabels([''] + self.labels)
        ax.set_yticklabels([''] + self.labels)
        plt.xlabel('Predicted')
        plt.ylabel('True')
        plt.show()
