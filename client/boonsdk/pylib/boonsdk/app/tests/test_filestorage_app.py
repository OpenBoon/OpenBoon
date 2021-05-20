import logging
import unittest
from unittest.mock import patch
from boonsdk import BoonClient, app_from_env

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class BoonSdkFileStorageTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()

    @patch.object(BoonClient, 'get')
    def test_get_cloud_location(self, get_patch):
        uri = "https://cloud/bucket/project/project_id/entity/12345/category/filename.zip"
        media_type = "application/zip"
        get_patch.return_value = {"uri": uri,
                                  "mediaType": media_type}

        rsp = self.app.filestorage.get_cloud_location("entity", "12345", "category", "filename.zip")
        assert rsp["uri"] == uri
        assert rsp['mediaType'] == media_type
