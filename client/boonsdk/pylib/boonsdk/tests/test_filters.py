import unittest

from boonsdk.filters import apply_search_filters, \
    ExcludeDatasetFilter, DatasetFilter, ExcludeAllDatasetsFilter
from boonsdk import LabelScope, Model, Dataset


class AssetSearchFilterTests(unittest.TestCase):

    def test_exclude_data_sets_filter(self):
        search = {}
        apply_search_filters(search, ExcludeAllDatasetsFilter())
        assert 'exclude_all_datasets' in search

    def test_data_set_filter_simple(self):
        search = {}
        apply_search_filters(search, DatasetFilter('abc123'))
        tsq = search['dataset']
        assert tsq['datasetId'] == 'abc123'
        assert tsq['scopes'] is None
        assert tsq['labels'] is None

    def test_data_set_filter_complex(self):
        search = {}
        apply_search_filters(search,
                             DatasetFilter('abc123', scopes=LabelScope.TRAIN, labels=['cat']))
        tsq = search['dataset']
        assert tsq['datasetId'] == 'abc123'
        assert tsq['scopes'] == [LabelScope.TRAIN.name]
        assert tsq['labels'] == ['cat']

    def test_data_set_filter_from_label(self):
        ds = Dataset({'id': '12345'})
        label = ds.make_label('dog', bbox=[0.1, 0.1, 0.5, 0.5], simhash='ABC1234')

        search = {}
        apply_search_filters(search, label.asset_search_filter())
        tsq = search['dataset']
        assert tsq['datasetId'] == '12345'
        assert tsq['scopes'] == [LabelScope.TRAIN.name]
        assert tsq['labels'] == ['dog']

    def test_data_set_filter_from_model(self):
        ds = Dataset({'id': '12345'})

        search = {}
        apply_search_filters(search, ds.asset_search_filter())
        tsq = search['dataset']
        assert tsq['datasetId'] == '12345'
        assert tsq['scopes'] is None
        assert tsq['labels'] is None

    def test_data_set_filter_from_model2(self):
        ds = Model({'id': '12345', 'datasetId': '12345'})

        search = {}
        apply_search_filters(search, ds)
        tsq = search['dataset']
        assert tsq['datasetId'] == '12345'
        assert tsq['scopes'] is None
        assert tsq['labels'] is None

    def test_exclude_dataset(self):
        ds = Dataset({'id': '12345', 'datasetId': '12345'})

        search = {}
        apply_search_filters(search, ExcludeDatasetFilter(ds))
        tsq = search['exclude_dataset']
        assert tsq['datasetId'] == '12345'
        assert tsq['scopes'] is None
        assert tsq['labels'] is None
