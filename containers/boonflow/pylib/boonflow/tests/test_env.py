import logging
import os
from unittest import TestCase

import flask

import boonflow.base as base

logging.basicConfig(level=logging.DEBUG)


class TesEnvClasses(TestCase):

    def setUp(self):
        os.environ['BOONFLOW_IN_FLASK'] = "yes"

    def tearDown(self):
        try:
            del os.environ['BOONFLOW_IN_FLASK']
        except Exception:
            pass

    token_path = os.path.dirname(__file__) + "/token.txt"

    def test_app_istance_from_flask(self):
        app = flask.Flask('test')
        with open(self.token_path) as fp:
            token = fp.read()
        request_ctx = app.test_request_context(headers={'Authorization': f'Bearer {token}'})
        request_ctx.push()

        sdk = base.app_instance()
        f'Bearer {token}' in sdk.client.headers().values()

    def test_get_server(self):
        app = flask.Flask('test')
        with open(self.token_path) as fp:
            token = fp.read()
        with app.test_request_context(headers={'Authorization': f'Bearer {token}'}):
            sdk = base.app_instance()
            server = sdk.client.get_server()
            assert server == 'https://dev.api.boonai.app'
