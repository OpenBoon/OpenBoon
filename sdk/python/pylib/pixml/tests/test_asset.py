import logging
import unittest
from unittest.mock import patch

from pixml import Asset
from pixml import PixmlClient, app_from_env
from pixml.asset import FileImport, FileUpload
from pixml.analysis.testing import zorroa_test_data

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class AssetTests(unittest.TestCase):

    def setUp(self):
        self.test_files = [{
            "assetId": "123",
            "category": "proxy",
            "name": "proxy_200x200.jpg",
            "mimetype": "image/jpeg",
            "attrs": {
                "width": 200,
                "height": 200
            }
        }]

    def test_get_files_filter_name(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 1 == len(asset.get_files(name="proxy_200x200.jpg"))
        assert 1 == len(asset.get_files(name=["proxy_200x200.jpg"]))
        assert 0 == len(asset.get_files(name="spock"))

    def test_get_files_filter_category(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 1 == len(asset.get_files(category="proxy"))
        assert 1 == len(asset.get_files(category=["proxy"]))
        assert 0 == len(asset.get_files(name="face"))

    def test_get_files_filter_mimetype(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 1 == len(asset.get_files(mimetype="image/jpeg"))
        assert 1 == len(asset.get_files(mimetype=["image/", "video/mp4"]))
        assert 0 == len(asset.get_files(mimetype="video/mp4"))

    def test_get_files_by_extension(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 1 == len(asset.get_files(extension="jpg"))
        assert 0 == len(asset.get_files(extension="png"))
        assert 1 == len(asset.get_files(extension=["png", "jpg"]))

    def test_get_files_by_attrs(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 1 == len(asset.get_files(attrs={"width": 200}))
        assert 0 == len(asset.get_files(attrs={"width": 200, "height": 100}))

    def test_get_files_by_all(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 1 == len(asset.get_files(mimetype="image/jpeg",
                                        extension=["png", "jpg"],
                                        attrs={"width": 200}))

    def test_equal(self):
        assert Asset({"id": "123"}) == Asset({"id": "123"})


class AssetAppTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()

    @patch.object(PixmlClient, 'post')
    def test_import_files(self, post_patch):
        post_patch.return_value = {
            "status": [
                {"assetId": "abc123", "failed": False}
            ],
            "assets": [
                {
                    "id": "abc123",
                    "document": {
                        "source": {
                            "path": "gs://zorroa-dev-data/image/pluto.png"
                        }
                    }
                }
            ]
        }
        assets = [FileImport("gs://zorroa-dev-data/image/pluto.png")]
        rsp = self.app.assets.import_files(assets)
        assert rsp["status"][0]["assetId"] == "abc123"
        assert not rsp["status"][0]["failed"]

    @patch.object(PixmlClient, 'get')
    def test_get_asset(self, get_patch):
        get_patch.return_value = {
            "id": "abc13",
            "document": {
                "source": {
                    "path": "gs://zorroa-dev-data/image/pluto.png"
                }
            }
        }
        asset = self.app.assets.get_asset("abc123")
        assert type(asset) == Asset
        assert asset.uri is not None
        assert asset.id is not None
        assert asset.document is not None

    @patch.object(PixmlClient, 'upload_files')
    def test_upload_assets(self, post_patch):
        post_patch.return_value = {
            "status": [
                {"assetId": "abc123", "failed": False}
            ],
            "assets": [
                {
                    "id": "abc123",
                    "document": {
                        "source": {
                            "path": "zmlp:///abc123/source/toucan.jpg"
                        }
                    }
                }
            ]
        }
        print(zorroa_test_data("images/set01/toucan.jpg", False))
        assets = [FileUpload(zorroa_test_data("images/set01/toucan.jpg", False))]
        rsp = self.app.assets.upload_files(assets)
        assert rsp["status"][0]["assetId"] == "abc123"
