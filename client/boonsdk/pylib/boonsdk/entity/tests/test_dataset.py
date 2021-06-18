import unittest

from boonsdk import Dataset, Model


class DatasetTests(unittest.TestCase):

    def test_dataset_id(self):
        assert Dataset.as_id(Dataset({'id': '12345'})) == '12345'
        assert Dataset.as_id(Model({'id': '12345', 'datasetId': 'abcdefg'})) == 'abcdefg'

    def test_make_label(self):
        ds = Dataset({'id': '12345'})
        label = ds.make_label('cat')
        assert label.dataset_id == '12345'
        assert label.label == 'cat'

    def test_make_label_from_prediction(self):
        ds = Dataset({'id': '12345'})
        pred = {
            'label': 'dog', 'bbox': [0.1, 0.1, 0.5, 0.5], 'simhash': 'ABC1234'
        }
        label = ds.make_label_from_prediction(pred)

        assert 'dog' == label.label
        assert [0.1, 0.1, 0.5, 0.5] == label.bbox
        assert 'ABC1234' == label.simhash

    def test_get_label_search(self):
        ds = Dataset({'id': '12345'})
        search = ds.get_label_search()
        assert search['size'] == 64
        assert search['sort'] == ['_doc']
        assert search['_source'] == ['labels', 'files']
