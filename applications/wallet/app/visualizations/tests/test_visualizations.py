import pytest
from unittest.mock import Mock

from boonsdk import BoonClient
from visualizations.visualizations import FacetVisualization, HistogramVisualization
from wallet.utils import convert_json_to_base64


class TestBaseVisualizationTestCase:
    Viz = None

    @pytest.fixture
    def data(self):
        return {}

    def test_is_valid(self, data):
        if self.Viz:
            viz = self.Viz(data)
            assert viz.is_valid()


class TestFacetVisualization(TestBaseVisualizationTestCase):
    Viz = FacetVisualization

    @pytest.fixture
    def data(self):
        return {
            'type': 'facet',
            'id': 'MySpecialGuy',
            'attribute': 'media.author',
            'options': {
                'order': 'desc',
                'size': 100,
                'minimumCount': 2
            }
        }

    def test_get_agg(self, data):
        viz = self.Viz(data)
        viz._field_type = 'keyword'
        agg = viz.get_es_agg()
        assert agg == {
            'terms': {
                'field': 'media.author',
                'size': 100,
                'order': {'_count': 'desc'},
                'min_doc_count': 2
            }
        }

    def test_get_agg_label_confidence(self, data):
        viz = self.Viz(data)
        viz._field_type = 'prediction'
        agg = viz.get_es_agg()
        assert agg == {
            'terms': {
                'field': 'media.author.predictions.label',
                'size': 100,
                'order': {'_count': 'desc'},
                'min_doc_count': 2
            }
        }


class TestHistogram(TestBaseVisualizationTestCase):

    Viz = HistogramVisualization

    @pytest.fixture
    def data(self):
        return {
            'type': 'histogram',
            'id': 'mySpecialGuy',
            'attribute': 'media.author',
            'options': {
                'size': 10,
            }
        }

    def test_get_agg(self, data, monkeypatch, zmlp_apikey):
        def mock_response(*args, **kwargs):
            return {
                'took': 0,
                'timed_out': False,
                '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0},
                'hits': {'total': {'value': 2, 'relation': 'eq'}, 'max_score': None,
                         'hits': []},
                'aggregations': {
                    'nested#mySpecialGuy': {
                        'doc_count': 3,
                        'extended_stats#stats': {
                            'count': 3,
                            'min': 10500219.0,
                            'max': 23002209.0,
                            'avg': 15336417.631578946,
                            'sum': 582783870.0,
                            'sum_of_squares': 1.186705982893944,
                            'variance': 0.08494821988646191,
                            'std_deviation': 0.2914587790519646,
                            'std_deviation_bounds': {
                                'upper': 1.140250888417294,
                                'lower': -0.025584227790564573}}}}}

        client = BoonClient(apikey=convert_json_to_base64(zmlp_apikey), server='localhost')
        viz = self.Viz(data, Mock(client=client), query={})
        viz._field_type = 'prediction'
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        agg = viz.get_es_agg()
        assert agg == {
            'aggs': {
                'labels': {
                    'aggs': {
                        'scores': {
                            'histogram': {'field': 'media.author.predictions.score',
                                          'interval': 1389110.0,
                                          'offset': 10500219.0}}},
                    'filter': {'match_all': {}}}},
            'nested': {'path': 'media.author.predictions'}}

    def test_get_agg_normal_field(self, data, monkeypatch, zmlp_apikey):

        data['fieldType'] = 'range'

        def mock_response(*args, **kwargs):
            return {
                'took': 2,
                'timed_out': False,
                '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0},
                'hits': {'total': {'value': 38, 'relation': 'eq'},
                         'max_score': None, 'hits': []},
                'aggregations': {
                    'extended_stats#mySpecialGuy': {
                        'count': 38,
                        'min': 10500219.0,
                        'max': 23002209.0,
                        'avg': 15336417.631578946,
                        'sum': 582783870.0,
                        'sum_of_squares': 23148928703501.0,
                        'variance': 2292811297.163797,
                        'std_deviation': 47883.309170981454,
                        'std_deviation_bounds': {
                            'upper': 159115.49739399232,
                            'lower': -32417.739289933488}}}}

        client = BoonClient(apikey=convert_json_to_base64(zmlp_apikey), server='localhost')
        viz = self.Viz(data, Mock(client=client), query={})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        viz._field_type = 'keyword'
        agg = viz.get_es_agg()
        assert agg == {
            'histogram': {
                'field': 'media.author',
                'interval': 1389110.0,
                'offset': 10500219.0
            }
        }

    def test_get_agg_empty_interval_response(self, data, monkeypatch, zmlp_apikey):

        def mock_response(*args, **kwargs):
            return {
                'took': 4,
                'timed_out': False,
                '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0},
                'hits': {'total': {'value': 0, 'relation': 'eq'},
                         'max_score': None, 'hits': []},
                'aggregations': {
                    'nested#mySpecialGuy': {
                        'doc_count': 0,
                        'extended_stats#stats': {
                            'count': 0,
                            'min': None,
                            'max': None,
                            'avg': None,
                            'sum': 0.0,
                            'sum_of_squares': None,
                            'variance': None,
                            'std_deviation': None,
                            'std_deviation_bounds': {
                                'upper': None,
                                'lower': None}}}}}

        client = BoonClient(apikey=convert_json_to_base64(zmlp_apikey), server='localhost')
        viz = self.Viz(data, Mock(client=client), query={})
        monkeypatch.setattr(BoonClient, 'post', mock_response)
        viz._field_type = 'prediction'
        agg = viz.get_es_agg()
        assert agg == {
            'aggs': {
                'labels': {
                    'aggs': {
                        'scores': {
                            'histogram': {'field': 'media.author.predictions.score',
                                          'interval': 1,
                                          'offset': 1}}},
                    'filter': {'match_all': {}}}},
            'nested': {'path': 'media.author.predictions'}}
