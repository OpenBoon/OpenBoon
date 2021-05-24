import unittest

from boonsdk import DataSet, Model


class DataSetTests(unittest.TestCase):

    def test_dataset_id(self):
        assert DataSet.as_id(DataSet({'id': '12345'})) == '12345'
        assert DataSet.as_id(Model({'id': '12345', 'dataSetId': 'abcdefg'})) == 'abcdefg'

    def test_make_label(self):
        ds = DataSet({'id': '12345'})
        label = ds.make_label('cat')
        assert label.dataset_id == '12345'
        assert label.label == 'cat'

    def test_make_label_from_prediction(self):
        ds = DataSet({'id': '12345'})
        pred = {
            'label': 'dog', 'bbox': [0.1, 0.1, 0.5, 0.5], 'simhash': 'ABC1234'
        }
        label = ds.make_label_from_prediction(pred)

        assert 'dog' == label.label
        assert [0.1, 0.1, 0.5, 0.5] == label.bbox
        assert 'ABC1234' == label.simhash
