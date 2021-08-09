import logging
import os
from unittest import TestCase

import flask

import boonflow.base as base

logging.basicConfig(level=logging.DEBUG)


class TesEnvClasses(TestCase):

    def setUp(self):
        os.environ['BOONFLOW_IN_FLASK'] = "yes"
        os.environ['BOONAI_ENV'] = "qa"

    def tearDown(self):
        del_envs = ['BOONFLOW_IN_FLASK', 'BOONAI_ENV']
        for env_name in del_envs:
            try:
                del os.environ[env_name]
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

    def test_get_server_by_endpoint_map(self):
        sdk = base.app_instance()
        server = sdk.client.get_server()
        assert server == 'https://qa.api.boonai.app'

    def test_get_server_by_server_env(self):
        try:
            os.environ['BOONAI_SERVER'] = 'https://localhost'
            sdk = base.app_instance()
            server = sdk.client.get_server()
            assert server == 'https://localhost'
        finally:
            del os.environ['BOONAI_SERVER']
