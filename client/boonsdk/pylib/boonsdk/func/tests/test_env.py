import os
from unittest import TestCase

import flask

from boonsdk.func import app_instance


class TesEnvFunctions(TestCase):

    def setUp(self):
        os.environ['BOONAI_SERVER'] = "http://localhost"

    def tearDown(self):
        del_envs = ['BOONAI_SERVER']
        for env_name in del_envs:
            try:
                del os.environ[env_name]
            except Exception:
                pass

    def test_app_instance(self):
        app = app_instance()
        assert app

    def test_app_istance_from_flask(self):
        app = flask.Flask('test')
        token = "IAMATOKEN"
        request_ctx = app.test_request_context(headers={'Authorization': f'Bearer {token}'})
        request_ctx.push()

        sdk = app_instance()
        assert f'Bearer {token}' in sdk.client.headers().values()

    def test_get_server_by_server_env(self):
        app = app_instance()
        server = app.client.get_server()
        assert server == 'http://localhost'
