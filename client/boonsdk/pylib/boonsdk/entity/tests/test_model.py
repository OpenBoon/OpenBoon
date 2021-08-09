import unittest

from boonsdk import Model


class ModelTests(unittest.TestCase):

    def test_get_confusion_matrix_search_knn(self):
        model = Model({'id': '1234',
                       'datasetId': '12345',
                       'moduleName': 'knn',
                       'type': 'KNN_CLASSIFIER'})
        search = model.get_confusion_matrix_search()
        assert search == {'size': 0, 'query': {'bool': {'filter': [{'range': {'analysis.knn.predictions.score': {'gte': 0.0, 'lte': 1.0}}}]}}, 'aggs': {'nested_labels': {'nested': {'path': 'labels'}, 'aggs': {'model_train_labels': {'filter': {'bool': {'must': [{'term': {'labels.datasetId': '12345'}}, {'term': {'labels.scope': 'TEST'}}]}}, 'aggs': {'labels': {'terms': {'field': 'labels.label', 'size': 1000}, 'aggs': {'predictions_by_label': {'reverse_nested': {}, 'aggs': {'predictions': {'terms': {'field': 'analysis.knn.predictions.label', 'size': 1000}}}}}}}}}}}} # noqa

    def test_get_confusion_matrix_search_face(self):
        model = Model({'id': '1234',
                       'datasetId': '12345',
                       'moduleName': 'face',
                       'type': 'FACE_RECOGNITION'})
        search = model.get_confusion_matrix_search()
        assert search == {'size': 0, 'query': {'bool': {'filter': [{'range': {'analysis.face.predictions.score': {'gte': 0.0, 'lte': 1.0}}}]}}, 'aggs': {'nested_labels': {'nested': {'path': 'labels'}, 'aggs': {'model_train_labels': {'filter': {'bool': {'must': [{'term': {'labels.datasetId': '12345'}}, {'term': {'labels.scope': 'TEST'}}]}}, 'aggs': {'labels': {'terms': {'field': 'labels.label', 'size': 1000}, 'aggs': {'predictions_by_label': {'reverse_nested': {}, 'aggs': {'predictions': {'terms': {'field': 'analysis.face.predictions.label', 'size': 1000}}}}}}}}}}}}  # noqa

    def test_get_confusion_matrix_search_with_args(self):
        model = Model({'id': '1234',
                       'datasetId': '12345',
                       'moduleName': 'knn',
                       'type': 'KNN_CLASSIFIER'})
        search = model.get_confusion_matrix_search(min_score=0.2, max_score=0.8,
                                                   test_set_only=False)
        assert search == {'size': 0, 'query': {'bool': {'filter': [{'range': {'analysis.knn.predictions.score': {'gte': 0.2, 'lte': 0.8}}}]}}, 'aggs': {'nested_labels': {'nested': {'path': 'labels'}, 'aggs': {'model_train_labels': {'filter': {'bool': {'must': [{'term': {'labels.datasetId': '12345'}}]}}, 'aggs': {'labels': {'terms': {'field': 'labels.label', 'size': 1000}, 'aggs': {'predictions_by_label': {'reverse_nested': {}, 'aggs': {'predictions': {'terms': {'field': 'analysis.knn.predictions.label', 'size': 1000}}}}}}}}}}}}  # noqa

    def test_get_confusion_matrix_search_no_dataset_id(self):
        model = Model({'id': '1234',
                       'moduleName': 'knn',
                       'type': 'KNN_CLASSIFIER'})
        with self.assertRaises(ValueError):
            model.get_confusion_matrix_search(min_score=0.2,
                                              max_score=0.8,
                                              test_set_only=False)
