import logging
import minio
import os
from unittest import TestCase
from unittest.mock import patch

from zmlpsdk.base import ZmlpEnv
from zmlp.client import ZmlpClient

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