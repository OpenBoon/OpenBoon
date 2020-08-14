import pytest
from unittest.mock import Mock

from zmlp import ZmlpClient
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
        data['fieldType'] = 'labelConfidence'
        viz = self.Viz(data)
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
            'fieldType': 'labelConfidence',
            'options': {
                'size': 10,
            }
        }

    def test_get_agg(self, data, monkeypatch, zmlp_apikey):

        def mock_response(*args, **kwargs):
            return {'took': 4, 'timed_out': False,
                    '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0},
                    'hits': {
                        'total': {
                            'value': 38,
                            'relation': 'eq'
                        },
                        'max_score': None,
                        'hits': []},
                    'aggregations': {
                        'stats#mySpecialGuy': {
                            'count': 38,
                            'min': 10500219.0,
                            'max': 23002209.0,
                            'avg': 15336417.631578946,
                            'sum': 582783870.0}}}

        client = ZmlpClient(apikey=convert_json_to_base64(zmlp_apikey), server='localhost')
        viz = self.Viz(data, Mock(client=client), query={})
        monkeypatch.setattr(ZmlpClient, 'post', mock_response)
        agg = viz.get_es_agg()
        assert agg == {
            'histogram': {
                'field': 'media.author.predictions.score',
                'interval': 1389110.0,
                'offset': 10500219.0
            }
        }

    def test_get_agg_normal_field(self, data, monkeypatch, zmlp_apikey):

        data['fieldType'] = 'range'

        def mock_response(*args, **kwargs):
            return {'took': 4, 'timed_out': False,
                    '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0},
                    'hits': {
                        'total': {
                            'value': 38,
                            'relation': 'eq'
                        },
                        'max_score': None,
                        'hits': []},
                    'aggregations': {
                        'stats#mySpecialGuy': {
                            'count': 38,
                            'min': 10500219.0,
                            'max': 23002209.0,
                            'avg': 15336417.631578946,
                            'sum': 582783870.0}}}

        client = ZmlpClient(apikey=convert_json_to_base64(zmlp_apikey), server='localhost')
        viz = self.Viz(data, Mock(client=client), query={})
        monkeypatch.setattr(ZmlpClient, 'post', mock_response)
        agg = viz.get_es_agg()
        assert agg == {
            'histogram': {
                'field': 'media.author',
                'interval': 1389110.0,
                'offset': 10500219.0
            }
        }

    def test_get_agg_normal_field_(self, data, monkeypatch, zmlp_apikey):

        data['fieldType'] = 'range'
        data['options']['size'] = 1

        def mock_response(*args, **kwargs):
            return {'took': 4, 'timed_out': False,
                    '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0},
                    'hits': {
                        'total': {
                            'value': 38,
                            'relation': 'eq'
                        },
                        'max_score': None,
                        'hits': []},
                    'aggregations': {
                        'stats#mySpecialGuy': {
                            'count': 38,
                            'min': 10500219.0,
                            'max': 23002209.0,
                            'avg': 15336417.631578946,
                            'sum': 582783870.0}}}

        client = ZmlpClient(apikey=convert_json_to_base64(zmlp_apikey), server='localhost')
        viz = self.Viz(data, Mock(client=client), query={})
        monkeypatch.setattr(ZmlpClient, 'post', mock_response)
        agg = viz.get_es_agg()
        assert agg == {
            'histogram': {
                'field': 'media.author',
                'interval': 12501990.0,
                'offset': 10500219.0
            }
        }