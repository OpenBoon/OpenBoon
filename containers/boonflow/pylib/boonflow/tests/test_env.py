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

    def test_app_istance_from_flask(self):
        app = flask.Flask("test")
        request_ctx = app.test_request_context(headers={'Authorization': 'Bearer XXXXX'})
        request_ctx.push()

        sdk = base.app_instance()
        'Bearer XXXXX' in sdk.client.headers().values()
