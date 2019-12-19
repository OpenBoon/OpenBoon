import unittest
import sys
import os

from unittest.mock import patch
from analyst import main


class AnalystMainUnitTestCase(unittest.TestCase):

    @patch('analyst.main.WSGIServer.serve_forever')
    @patch('analyst.main.setup_routes')
    @patch('analyst.main.WSGIServer')
    def test_default_port(self, mock_op, mock_routes, start_patch):
        mock_routes.return_value = None
        mock_routes.asset_called()
        start_patch.return_value = True

        testargs = ["analyst"]
        with patch.object(sys, 'argv', testargs):
            main.main()
            mock_op.assert_called_with((
                '0.0.0.0', 5000), main.app)

    @patch('analyst.main.WSGIServer.serve_forever')
    @patch('analyst.main.setup_routes')
    @patch('analyst.main.WSGIServer')
    def test_set_port_by_environment(self, mock_op, mock_routes, start_patch):
        mock_routes.return_value = None
        mock_routes.asset_called()
        start_patch.return_value = True

        testargs = ["analyst"]
        with patch.object(sys, 'argv', testargs):
            try:
                os.environ["ZORROA_ANALYST_PORT"] = "5500"
                main.main()
                mock_op.assert_called_with((
                    '0.0.0.0', 5500), main.app)
            finally:
                del os.environ["ZORROA_ANALYST_PORT"]

    @patch('analyst.main.WSGIServer.serve_forever')
    @patch('analyst.main.setup_routes')
    @patch('analyst.main.WSGIServer')
    def test_set_port(self, mock_op, mock_routes, start_patch):
        mock_routes.return_value = None
        mock_routes.asset_called()
        start_patch.return_value = True

        testargs = ["analyst", "-p", "6000"]
        with patch.object(sys, 'argv', testargs):
            main.main()
            mock_op.assert_called_with((
                '0.0.0.0', 6000), main.app)
