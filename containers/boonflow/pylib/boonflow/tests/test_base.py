import logging
import os
from unittest import TestCase

import boonflow.base as base
import boonflow.testing as testing
from boonflow import BoonEnv

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
            creds = BoonEnv.get_available_credentials_types()
            assert "GCP" in creds
            assert "AWS" in creds
            assert "AZURE" not in creds
        finally:
            del os.environ["BOONAI_CREDENTIALS_TYPES"]

    def test_byte_array_input_stream_pil(self):
        path = testing.test_path("images/set01/toucan.jpg")
        stream = base.ImageInputStream.from_path(path)

        # Should be able to read bytes in twice or more.
        for i in range(0, 2):
            image = stream.pil_img()
            exif = image.getexif()
            assert exif[36867] == '2009:11:18 16:04:43'

    def test_byte_array_input_stream_cv(self):
        path = testing.test_path("images/set01/toucan.jpg")
        stream = base.ImageInputStream.from_path(path)

        # Should be able to read bytes in twice or more.
        for i in range(0, 2):
            image = stream.cv_img()
            assert image.shape == (341, 512, 3)
