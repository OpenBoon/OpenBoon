import unittest

from boonsdk.filters import apply_search_filters, ExcludeTrainingSetsFilter, TrainingSetFilter
from boonsdk import LabelScope, Model


class AssetSearchFilterTests(unittest.TestCase):

    def test_exclude_training_sets_filter(self):
        search = {}
        apply_search_filters(search, ExcludeTrainingSetsFilter())
        assert 'exclude_training_sets' in search

    def test_training_set_filter_simple(self):
        search = {}
        apply_search_filters(search, TrainingSetFilter('abc123'))
        tsq = search['training_set']
        assert tsq['modelId'] == 'abc123'
        assert tsq['scopes'] is None
        assert tsq['labels'] is None

    def test_training_set_filter_complex(self):
        search = {}
        apply_search_filters(search,
                             TrainingSetFilter('abc123', scopes=LabelScope.TRAIN, labels=['cat']))
        tsq = search['training_set']
        assert tsq['modelId'] == 'abc123'
        assert tsq['scopes'] == [LabelScope.TRAIN.name]
        assert tsq['labels'] == ['cat']

    def test_training_set_filter_from_label(self):
        ds = Model({'id': '12345'})
        label = ds.make_label('dog', bbox=[0.1, 0.1, 0.5, 0.5], simhash='ABC1234')

        search = {}
        apply_search_filters(search, label.asset_search_filter())
        tsq = search['training_set']
        assert tsq['modelId'] == '12345'
        assert tsq['scopes'] == [LabelScope.TRAIN.name]
        assert tsq['labels'] == ['dog']

    def test_training_set_filter_from_model(self):
        ds = Model({'id': '12345'})

        search = {}
        apply_search_filters(search, ds.asset_search_filter())
        tsq = search['training_set']
        assert tsq['modelId'] == '12345'
        assert tsq['scopes'] is None
        assert tsq['labels'] is None

    def test_training_set_filter_from_model2(self):
        ds = Model({'id': '12345'})

        search = {}
        apply_search_filters(search, ds)
        tsq = search['training_set']
        assert tsq['modelId'] == '12345'
        assert tsq['scopes'] is None
        assert tsq['labels'] is None
