import logging
import os
from unittest import TestCase

from zmlpsdk.base import ZmlpEnv

logging.basicConfig(level=logging.DEBUG)


class TestZmlpEnv(TestCase):
    def test_get_available_credentials_types(self):
        os.environ["ZMLP_CREDENTIALS_TYPES"] = "GCP,AWS"
        try:
            creds = ZmlpEnv.get_available_credentials_types()
            assert "GCP" in creds
            assert "AWS" in creds
            assert "AZURE" not in creds
        finally:
            del os.environ["ZMLP_CREDENTIALS_TYPES"]
