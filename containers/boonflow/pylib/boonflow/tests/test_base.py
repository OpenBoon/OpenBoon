import logging
import os
import json
from unittest import TestCase

import boonflow.base as base
import boonflow.analysis as analysis
import boonflow.testing as testing
from boonflow import BoonEnv
from boonsdk import to_json

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

    def test_image_input_stream_pil(self):
        path = testing.test_path("images/set01/toucan.jpg")
        stream = base.ImageInputStream.from_path(path)

        # Should be able to read bytes in twice or more.
        for i in range(0, 2):
            image = stream.pil_img()
            exif = image.getexif()
            print(exif)
            assert exif[306] == '2009:12:12 19:48:58'

    def test_image_input_stream_cv(self):
        path = testing.test_path("images/set01/toucan.jpg")
        stream = base.ImageInputStream.from_path(path)

        # Should be able to read bytes in twice or more.
        for i in range(0, 2):
            image = stream.cv_img()
            assert image.shape == (341, 512, 3)

    def test_boon_func_rsp_set_analysis(self):
        an = analysis.LabelDetectionAnalysis()
        an.add_label_and_score('cat', 0.5)

        rsp = base.BoonFunctionResponse()
        rsp.set_analysis(an)

        res = json.loads(to_json(rsp))
        assert res['analysis']['__MAIN__']

    def test_boon_func_rsp_add_more_analysis(self):
        an = analysis.LabelDetectionAnalysis()
        an.add_label_and_score('cat', 0.5)

        rsp = base.BoonFunctionResponse()
        rsp.add_more_analysis('cats', an)

        res = json.loads(to_json(rsp))
        assert res['analysis']['cats']
