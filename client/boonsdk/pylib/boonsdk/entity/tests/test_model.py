import unittest

from boonsdk import Model


class ModelTests(unittest.TestCase):

    def test_make_label(self):
        ds = Model({'id': '12345'})
        label = ds.make_label('dog', bbox=[0.1, 0.1, 0.5, 0.5], simhash='ABC1234')
        assert 'dog' == label.label
        assert [0.1, 0.1, 0.5, 0.5] == label.bbox
        assert 'ABC1234' == label.simhash

    def test_make_label_from_prediction(self):
        ds = Model({'id': '12345'})
        label = ds.make_label_from_prediction('dog',
                                              {'bbox': [0.1, 0.1, 0.5, 0.5],
                                               'simhash': 'ABC1234'})
        assert 'dog' == label.label
        assert [0.1, 0.1, 0.5, 0.5] == label.bbox
        assert 'ABC1234' == label.simhash

    def test_get_label_search(self):
        ds = Model({'id': '12345'})
        search = ds.get_label_search()
        assert search['size'] == 64
        assert search['sort'] == ['_doc']
        assert search['_source'] == ['labels', 'files']

    def test_get_confusion_matrix_search_knn(self):
        model = Model({'id': '12345',
                       'moduleName': 'knn',
                       'type': 'KNN_CLASSIFIER'})
        search = model.get_confusion_matrix_search()
        assert search == {'size': 0, 'query': {'bool': {'filter': [{'range': {'analysis.knn.score': {'gte': 0.0, 'lte': 1.0}}}]}}, 'aggs': {'nested_labels': {'nested': {'path': 'labels'}, 'aggs': {'model_train_labels': {'filter': {'bool': {'must': [{'term': {'labels.modelId': '12345'}}, {'term': {'labels.scope': 'TEST'}}]}}, 'aggs': {'labels': {'terms': {'field': 'labels.label'}, 'aggs': {'predictions_by_label': {'reverse_nested': {}, 'aggs': {'predictions': {'terms': {'field': 'analysis.knn.label'}}}}}}}}}}}} # noqa

    def test_get_confusion_matrix_search_face(self):
        model = Model({'id': '12345',
                       'moduleName': 'face',
                       'type': 'FACE_RECOGNITION'})
        search = model.get_confusion_matrix_search()
        assert search == {'size': 0, 'query': {'bool': {'filter': [{'range': {'analysis.face.predictions.score': {'gte': 0.0, 'lte': 1.0}}}]}}, 'aggs': {'nested_labels': {'nested': {'path': 'labels'}, 'aggs': {'model_train_labels': {'filter': {'bool': {'must': [{'term': {'labels.modelId': '12345'}}, {'term': {'labels.scope': 'TEST'}}]}}, 'aggs': {'labels': {'terms': {'field': 'labels.label'}, 'aggs': {'predictions_by_label': {'reverse_nested': {}, 'aggs': {'predictions': {'terms': {'field': 'analysis.face.predictions.label'}}}}}}}}}}}}  # noqa

    def test_get_confusion_matrix_search_with_args(self):
        model = Model({'id': '12345',
                       'moduleName': 'knn',
                       'type': 'KNN_CLASSIFIER'})
        search = model.get_confusion_matrix_search(min_score=0.2, max_score=0.8,
                                                   test_set_only=False)
        assert search == {'size': 0, 'query': {'bool': {'filter': [{'range': {'analysis.knn.score': {'gte': 0.2, 'lte': 0.8}}}]}}, 'aggs': {'nested_labels': {'nested': {'path': 'labels'}, 'aggs': {'model_train_labels': {'filter': {'bool': {'must': [{'term': {'labels.modelId': '12345'}}]}}, 'aggs': {'labels': {'terms': {'field': 'labels.label'}, 'aggs': {'predictions_by_label': {'reverse_nested': {}, 'aggs': {'predictions': {'terms': {'field': 'analysis.knn.label'}}}}}}}}}}}}  # noqa
