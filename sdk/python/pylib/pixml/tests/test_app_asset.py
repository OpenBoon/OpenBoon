import logging
import unittest

from unittest.mock import patch

from pixml import PixmlClient, app_from_env
from pixml.asset import AssetSpec

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

class AssetAppTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()

    @patch.object(PixmlClient, 'post')
    def test_bulk_process_assets(self, post_patch):
        post_patch.return_value = {
            "status": { "abc123": "provisioned"},
            "assets": [
                {
                    "id": "abc123",
                    "document": {

                    }
                }
            ]
        }
        assets = [AssetSpec("gs://zorroa-dev-data/image/pluto.png")]
        self.app.assets.bulk_process_assets(assets)

