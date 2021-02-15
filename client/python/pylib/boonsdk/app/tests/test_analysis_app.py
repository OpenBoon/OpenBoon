import logging
import unittest
from unittest.mock import patch

from boonsdk import BoonSdkClient
from .util import get_boon_app

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class AnalysisModuleAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.app = get_boon_app()

        self.obj_data = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'description': 'foo',
            'provider': 'Zorroa',
            'category': 'Visual Intelligence',
            'type': "LabelDetection"
        }

    @patch.object(BoonSdkClient, 'get')
    def test_get_analyis_module(self, get_patch):
        get_patch.return_value = self.obj_data
        plmod = self.app.analysis.get_analysis_module('12345')
        self.assert_pipeline_mod(plmod)

    @patch.object(BoonSdkClient, 'post')
    def test_find_one_analysis_module(self, post_patch):
        post_patch.return_value = self.obj_data
        plmod = self.app.analysis.find_one_analysis_module(id="12345")
        self.assert_pipeline_mod(plmod)

    @patch.object(BoonSdkClient, 'post')
    def test_find_pipeline_mods(self, post_patch):
        post_patch.return_value = {"list": [self.obj_data]}
        plmod = list(self.app.analysis.find_analysis_modules(id="12345", limit=1))
        self.assert_pipeline_mod(plmod[0])

    def assert_pipeline_mod(self, mod):
        assert self.obj_data['id'] == mod.id
        assert self.obj_data['name'] == mod.name
