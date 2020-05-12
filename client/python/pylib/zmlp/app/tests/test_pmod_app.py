import logging
import unittest
from unittest.mock import patch

from zmlp import ZmlpClient
from .util import get_zmlp_app

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class PipelineModAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.app = get_zmlp_app()

        self.obj_data = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'description': 'foo',
            'provider': 'Zorroa',
            'category': 'Visual Intelligence',
            'type': "LabelDetection"
        }

    @patch.object(ZmlpClient, 'get')
    def test_get_pipeline_mod(self, get_patch):
        get_patch.return_value = self.obj_data
        plmod = self.app.pmods.get_pipeline_mod('12345')
        self.assert_pipeline_mod(plmod)

    @patch.object(ZmlpClient, 'post')
    def test_find_one_pipeline_mod(self, post_patch):
        post_patch.return_value = self.obj_data
        plmod = self.app.pmods.find_one_pipeline_mod(id="12345")
        self.assert_pipeline_mod(plmod)

    @patch.object(ZmlpClient, 'post')
    def test_find_pipeline_mods(self, post_patch):
        post_patch.return_value = {"list": [self.obj_data]}
        plmod = list(self.app.pmods.find_pipeline_mods(id="12345", limit=1))
        self.assert_pipeline_mod(plmod[0])

    def assert_pipeline_mod(self, mod):
        assert self.obj_data['id'] == mod.id
        assert self.obj_data['name'] == mod.name
