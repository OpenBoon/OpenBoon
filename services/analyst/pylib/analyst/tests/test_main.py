import unittest
import sys
import os

from mock import patch
from analyst import main


class AnalystMainUnitTestCase(unittest.TestCase):

    @patch('subprocess.call')
    @patch('analyst.main.setup_routes')
    @patch('flask.Flask.run')
    def test_default_port(self, mock_op, mock_routes, call_patch):
        mock_routes.return_value = None
        mock_routes.asset_called()

        testargs = ["analyst"]
        with patch.object(sys, 'argv', testargs):
            main.main()
            mock_op.assert_called_with(host='0.0.0.0', port=5000,
                                       ssl_context=('certs/analyst.cert', 'certs/analyst.key'))

    @patch('subprocess.call')
    @patch('analyst.main.setup_routes')
    @patch('flask.Flask.run')
    def test_set_port_by_environment(self, mock_op, mock_routes, call_patch):
        mock_routes.return_value = None
        mock_routes.asset_called()

        testargs = ["analyst"]
        with patch.object(sys, 'argv', testargs):
            try:
                os.environ["ZORROA_ANALYST_PORT"] = "5500"
                main.main()
                mock_op.assert_called_with(host='0.0.0.0', port=5500,
                                           ssl_context=('certs/analyst.cert', 'certs/analyst.key'))
            finally:
                del os.environ["ZORROA_ANALYST_PORT"]

    @patch('subprocess.call')
    @patch('analyst.main.setup_routes')
    @patch('flask.Flask.run')
    def test_set_port(self, mock_op, mock_routes, call_patch):
        mock_routes.return_value = None
        mock_routes.asset_called()

        testargs = ["analyst", "-p", "6000"]
        with patch.object(sys, 'argv', testargs):
            main.main()
            mock_op.assert_called_with(host='0.0.0.0', port=6000,
                                       ssl_context=('certs/analyst.cert', 'certs/analyst.key'))
