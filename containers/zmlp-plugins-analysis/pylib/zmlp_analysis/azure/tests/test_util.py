import os
from unittest import TestCase

import pytest

from zmlp_analysis.azure.util import get_zvi_azure_cv_client


class AzureVisionProcessorTests(TestCase):

    def test_get_zvi_azure_cv_client(self):
        os.environ["ZORROA_AZURE_VISION_KEY"] = "abc123"
        try:
            get_zvi_azure_cv_client()
        finally:
            del os.environ["ZORROA_AZURE_VISION_KEY"]

    def test_get_zvi_azure_cv_client_not_setup(self):
        with pytest.raises(RuntimeError):
            get_zvi_azure_cv_client()
