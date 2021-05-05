import logging
import os
from unittest import TestCase

import flask

import boonflow.base as base

logging.basicConfig(level=logging.DEBUG)


class TestBaseClasses(TestCase):

    def setUp(self):
        os.environ['BOONFLOW_IN_FLASK'] = "yes"

    def tearDown(self):
        try:
            del os.environ['BOONFLOW_IN_FLASK']
        except Exception:
            pass

    def test_get_available_credentials_types(self):
        os.environ["BOONAI_CREDENTIALS_TYPES"] = "GCP,AWS"
        try:
            creds = base.BoonEnv.get_available_credentials_types()
            assert "GCP" in creds
            assert "AWS" in creds
            assert "AZURE" not in creds
        finally:
            del os.environ["BOONAI_CREDENTIALS_TYPES"]

    def test_app_istance_from_flask(self):
        app = flask.Flask("test")
        request_ctx = app.test_request_context(headers={'Authorization': 'Bearer XXXXX'})
        request_ctx.push()

        sdk = base.app_instance()
        'Bearer XXXXX' in sdk.client.headers().values()
