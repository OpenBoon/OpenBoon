import datetime
import logging
import unittest
from unittest.mock import patch

from zmlp import ZmlpClient, ZmlpApp

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class ZmlpProjectAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.key_dict = {
            'projectId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'keyId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'sharedKey': 'test123test135'
        }
        self.app = ZmlpApp(self.key_dict)

    @patch.object(ZmlpClient, 'get')
    def test_get_project(self, get_patch):
        value = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'actorCreated': '123',
            'actorModified': '456',
            'timeCreated':  1580830037232,
            'timeModified': 1580830037999
        }
        get_patch.return_value = value
        proj = self.app.projects.get_project()
        assert value['id'] == proj.id
        assert value['name'] == proj.name
        assert isinstance(proj.time_created, datetime.datetime)
        assert isinstance(proj.time_modified, datetime.datetime)
        assert value['actorCreated'] == '123'
        assert value['actorModified'] == '456'

    @patch.object(ZmlpClient, 'get')
    def test_get_project_settings(self, get_patch):
        value = {
            'defaultPipelineId': 'abc',
            'defaultIndexRouteId': '123'
        }
        get_patch.return_value = value
        settings = self.app.projects.get_project_settings()
        assert value['defaultPipelineId'] == settings['defaultPipelineId']
        assert value['defaultIndexRouteId'] == settings['defaultIndexRouteId']

    @patch.object(ZmlpClient, 'put')
    def test_set_project_settings(self, put_patch):
        updated = {
            'defaultPipelineId': 'abc',
            'defaultIndexRouteId': '123'
        }
        put_patch.return_value = updated
        settings = self.app.projects.update_project_settings(updated)
        assert updated['defaultPipelineId'] == settings['defaultPipelineId']
        assert updated['defaultIndexRouteId'] == settings['defaultIndexRouteId']
