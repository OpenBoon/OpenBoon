# flake8: noqa
import logging
import unittest
from unittest.mock import patch

from boonsdk import BoonClient, BoonApp

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class BoonSdkCustomFieldAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.key_dict = {
            'projectId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'keyId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'sharedKey': 'test123test135'
        }
        self.app = BoonApp(self.key_dict)

        self.field_data = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'country',
            'type': 'keyword',
            'path': 'custom.country'
        }

    @patch.object(BoonClient, 'post')
    def test_create_field(self, post_patch):
        post_patch.return_value = self.field_data
        field = self.app.fields.create_field('country', 'keyword')
        self.assert_field(field)

    @patch.object(BoonClient, 'get')
    def test_get_field(self, post_patch):
        post_patch.return_value = self.field_data
        field = self.app.fields.get_field('A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80')
        self.assert_field(field)

    @patch.object(BoonClient, 'post')
    def test_find_fields(self, post_patch):
        post_patch.return_value = {
            "list": [{
                "name": "foo",
                "type": "text"
            }]
        }
        items = self.app.fields.find_fields(limit=1)
        assert len(list(items)) == 1
        for item in items:
            assert item.name == "foo"
            assert item.type == "type"

        items = self.app.fields.find_fields(id=['1234'],
                                            type=['text'],
                                            name=['foo'],
                                            limit=1,
                                            sort={'name': 'desc'})
        assert len(list(items)) == 1
        for item in items:
            assert item.name == "foo"
            assert item.type == "text"

    @patch.object(BoonClient, 'post')
    def test_find_one_job(self, post_patch):
        post_patch.return_value = {
            "name": "foo",
            "type": "text"
        }
        field = self.app.fields.find_one_field()
        assert field.name == "foo"
        assert field.type == "text"

    def assert_field(self, item):
        assert self.field_data['id'] == item.id
        assert self.field_data['name'] == item.name
        assert self.field_data['type'] == item.type
        assert self.field_data['path'] == item.path
