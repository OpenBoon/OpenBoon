import unittest

from zmlp import Model


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
                                              {'bbox': [0.1, 0.1, 0.5, 0.5], 'simhash': 'ABC1234'})
        assert 'dog' == label.label
        assert [0.1, 0.1, 0.5, 0.5] == label.bbox
        assert 'ABC1234' == label.simhash
