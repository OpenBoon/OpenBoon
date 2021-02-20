import logging
import os
from unittest import TestCase

from boonflow.base import BoonEnv

logging.basicConfig(level=logging.DEBUG)


class TestBoonEnv(TestCase):
    def test_get_available_credentials_types(self):
        os.environ["BOONAI_CREDENTIALS_TYPES"] = "GCP,AWS"
        try:
            creds = BoonEnv.get_available_credentials_types()
            assert "GCP" in creds
            assert "AWS" in creds
            assert "AZURE" not in creds
        finally:
            del os.environ["BOONAI_CREDENTIALS_TYPES"]
