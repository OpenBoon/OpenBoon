# flake8: noqa
import os
import datetime
import logging
import unittest
from unittest.mock import patch

from boonai import ZmlpClient, BoonAiApp
from boonai.entity import Job, Task


logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class ZmlpCustomFieldAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.key_dict = {
            'projectId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'keyId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'sharedKey': 'test123test135'
        }
        self.app = BoonAiApp(self.key_dict)

        self.field_data = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'country',
            'type': 'keyword',
            'esField': 'custom.keyword'
        }

    @patch.object(ZmlpClient, 'post')
    def test_create_custom_field(self, post_patch):
        post_patch.return_value = self.field_data
        field = self.app.fields.create_custom_field('country', 'keyword')

    @patch.object(ZmlpClient, 'get')
    def test_get_custom_field(self, post_patch):
        post_patch.return_value = self.field_data
        field = self.app.fields.get_custom_field('A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80')

    def assert_field(self, item):
        assert self.field_data['id'] == item.id
        assert self.field_data['name'] == item.name
        assert self.field_data['type'] == item.type
        assert self.field_data['esField'] == item.es_field_name

