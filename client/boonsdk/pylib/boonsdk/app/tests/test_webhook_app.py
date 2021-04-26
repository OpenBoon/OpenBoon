import logging
import unittest
from unittest.mock import patch

import boonsdk
from boonsdk import BoonClient, WebHook
from .util import get_boon_app

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class WebHookAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.app = get_boon_app()

        self.hook = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'url': 'https://foo',
            'triggers': ['AssetAnalyzed']
        }

    @patch.object(BoonClient, 'post')
    def test_create_webhook(self, post_patch):
        post_patch.return_value = self.hook
        hook = self.app.webhooks.create_webhook(
            'https://foo', 'secret', boonsdk.WebHookTrigger.AssetAnalyzed)
        assert self.hook == hook._data

    @patch.object(BoonClient, 'post')
    def test_pretest_webhook(self, post_patch):
        post_patch.return_value = {'success': True}
        rsp = self.app.webhooks.pretest_webhook(
            'https://foo', 'secret', boonsdk.WebHookTrigger.AssetAnalyzed)
        assert post_patch.return_value == rsp

    @patch.object(BoonClient, 'post')
    def test_test_webhook(self, post_patch):
        post_patch.return_value = {'success': True}
        hook = WebHook(self.hook)
        rsp = self.app.webhooks.test_webhook(hook)
        assert post_patch.return_value == rsp

    @patch.object(BoonClient, 'delete')
    def test_delete_webhook(self, del_patch):
        del_patch.return_value = {'success': True}
        hook = WebHook(self.hook)
        rsp = self.app.webhooks.delete_webhook(hook)
        assert del_patch.return_value == rsp

    @patch.object(BoonClient, 'post')
    def test_find_webhooks(self, post_patch):
        post_patch.return_value = {"list": [self.hook]}
        res = list(self.app.webhooks.find_webhooks(id="12345", limit=1))
        assert res[0]._data == self.hook

    @patch.object(BoonClient, 'get')
    def test_get_webhook(self, get_patch):
        get_patch.return_value = self.hook
        res = self.app.webhooks.get_webhook('12345')
        assert res._data == self.hook

    @patch.object(BoonClient, 'patch')
    def test_update_webhook(self, patch_patch):
        patch_patch.return_value = {'success': True}
        res = self.app.webhooks.update_webhook('12345', url='http://bar')
        assert res == {'success': True}
