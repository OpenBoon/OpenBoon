import logging
import unittest
from unittest.mock import patch

from zmlp import ZmlpClient, ZmlpApp
from zmlp.dataset import DataSetType

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class ZmlpDataSetAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.key_dict = {
            'projectId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'keyId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'sharedKey': 'test123test135'
        }
        self.app = ZmlpApp(self.key_dict)

    @patch.object(ZmlpClient, 'post')
    def test_create_dataset(self, post_patch):
        value = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'type': 'LabelDetection'
        }
        post_patch.return_value = value
        ds = self.app.datasets.create_dataset('test', DataSetType.LabelDetection)
        assert value['id'] == ds.id
        assert value['name'] == ds.name
        assert value['type'] == ds.type

    @patch.object(ZmlpClient, 'get')
    def test_get_dataset(self, get_patch):
        value = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'type': 'LabelDetection'
        }
        get_patch.return_value = value
        ds = self.app.datasets.get_dataset('A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80')
        assert value['id'] == ds.id
        assert value['name'] == ds.name
        assert value['type'] == ds.type
