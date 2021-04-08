from unittest.mock import Mock
from wallet.mixins import BoonAISortArgsMixin


class TestBoonAISortArgsMixin:

    def test_converts_args_no_query_param(self):
        mock_request = Mock(query_params={})
        sort_args = BoonAISortArgsMixin().get_boonai_sort_args(mock_request)
        assert sort_args is None

    def test_converts_args_single_param_asc(self):
        mock_request = Mock(query_params={'ordering': 'timeCreated'})
        sort_args = BoonAISortArgsMixin().get_boonai_sort_args(mock_request)
        assert sort_args == ['timeCreated:a']

    def test_converts_args_single_param_desc(self):
        mock_request = Mock(query_params={'ordering': '-timeCreated'})
        sort_args = BoonAISortArgsMixin().get_boonai_sort_args(mock_request)
        assert sort_args == ['timeCreated:d']

    def test_converts_multiple_args(self):
        mock_request = Mock(query_params={'ordering': '-timeCreated,timeUpdated'})
        sort_args = BoonAISortArgsMixin().get_boonai_sort_args(mock_request)
        assert sort_args == ['timeCreated:d', 'timeUpdated:a']
