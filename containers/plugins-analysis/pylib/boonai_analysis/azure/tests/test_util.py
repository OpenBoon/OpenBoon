import os
from unittest import TestCase

import pytest

from boonai_analysis.azure.util import get_zvi_azure_cv_client


class AzureVisionProcessorTests(TestCase):

    def test_get_zvi_azure_cv_client(self):
        os.environ["BOONAI_AZURE_VISION_KEY"] = "abc123"
        try:
            get_zvi_azure_cv_client()
        finally:
            del os.environ["BOONAI_AZURE_VISION_KEY"]

    def test_get_zvi_azure_cv_client_not_setup(self):
        with pytest.raises(RuntimeError):
            get_zvi_azure_cv_client()
