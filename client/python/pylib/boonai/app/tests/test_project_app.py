import datetime
import logging
import unittest
from unittest.mock import patch

from boonai import ZmlpClient, BoonAiApp

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
        self.app = BoonAiApp(self.key_dict)

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
