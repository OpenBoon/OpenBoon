import unittest
from unittest.mock import patch

import boonsdk
import boonczar
from boonsdk.client import BoonClient
from boonsdk.entity.boonlib import BoonLibEntity


class BoonLibAppTests(unittest.TestCase):

    def setUp(self):
        self.key_dict = {
            'accessKey': 'test123test135',
            'secretKey': 'test123test135'
        }
        self.app = boonsdk.BoonApp(self.key_dict)
        self.bz = boonczar.BoonCzarApp(self.app)

    @patch.object(BoonClient, 'post')
    def test_create_boonlib(self, post_patch):
        post_patch.return_value = boonlib_data
        lib = self.bz.boonlibs.create_boonlib('Dataset', 'abc123', 'foo', 'bar')
        assert lib.name == 'foo'
        assert lib.description == 'bar'
        assert lib.entity == BoonLibEntity.Dataset
        assert lib.entity_type == 'Classification'


boonlib_data = {
    'id': 'abc123',
    'name': 'foo',
    'description': 'bar',
    'entity': 'Dataset',
    'entityType': 'Classification'
}
