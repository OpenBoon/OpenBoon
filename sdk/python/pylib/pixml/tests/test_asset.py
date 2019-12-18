import logging
import unittest
from unittest.mock import patch

from pixml import Asset
from pixml import PixmlClient, app_from_env
from pixml.analysis.testing import zorroa_test_data
from pixml.asset import FileImport, FileUpload, Clip

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class AssetTests(unittest.TestCase):

    def setUp(self):
        self.test_files = [{
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

    def test_get_files_by_attr_keys(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 1 == len(asset.get_files(attr_keys=["width"]))
        assert 1 == len(asset.get_files(attr_keys="width"))
        assert 0 == len(asset.get_files(attr_keys=["kirk"]))

    def test_get_files_sort_func(self):
        asset = Asset({"id": "123"})
        test_files = [
            {
                "category": "proxy",
                "name": "zzz.jpg",
                "mimetype": "image/jpeg",
                "attrs": {
                    "width": 200,
                    "height": 200
                }
            },
            {
                "category": "proxy",
                "name": "aaa.jpg",
                "mimetype": "image/jpeg",
                "attrs": {
                    "width": 200,
                    "height": 200
                }
            }
        ]
        asset.set_attr("files", test_files)
        top = asset.get_files(attr_keys=["width"], sort_func=lambda x: x["name"])[0]
        assert top["name"] == "aaa.jpg"

    def test_get_files_sort_func_and_filtered(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)
        top = asset.get_files(attr_keys=["dog"], sort_func=lambda x: x["name"])
        assert len(top) == 0

    def test_get_files_by_all(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 1 == len(asset.get_files(mimetype="image/jpeg",
                                        extension=["png", "jpg"],
                                        attrs={"width": 200}))

    def test_equal(self):
        assert Asset({"id": "123"}) == Asset({"id": "123"})

    def test_get_item_and_set_item(self):
        asset = Asset({"id": "123"})
        asset["foo.bar.bing"] = "123"
        assert asset["foo.bar.bing"] == "123"


class AssetAppTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()

        # A mock search result used for asset search tests
        self.mock_search_result = {
            "took": 4,
            "timed_out": False,
            "hits": {
                "total": 2,
                "max_score": 0.2876821,
                "hits": [
                    {
                        "_index": "litvqrkus86sna2w",
                        "_type": "asset",
                        "_id": "dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg",
                        "_score": 0.2876821,
                        "_source": {
                            "source": {
                                "path": "https://i.imgur.com/SSN26nN.jpg"
                            }
                        }
                    },
                    {
                        "_index": "litvqrkus86sna2w",
                        "_type": "asset",
                        "_id": "aabbccddec48n1q1fginVMV5yllhRRGx2WKyKLjDphg",
                        "_score": 0.2876821,
                        "_source": {
                            "source": {
                                "path": "https://i.imgur.com/foo.jpg"
                            }
                        }
                    }
                ]
            }
        }

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
    def test_get_by_id(self, get_patch):
        get_patch.return_value = {
            "id": "abc13",
            "document": {
                "source": {
                    "path": "gs://zorroa-dev-data/image/pluto.png"
                }
            }
        }
        asset = self.app.assets.get_by_id("abc123")
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
        assets = [FileUpload(zorroa_test_data("images/set01/toucan.jpg", False))]
        rsp = self.app.assets.upload_files(assets)
        assert rsp["status"][0]["assetId"] == "abc123"

    @patch.object(PixmlClient, 'post')
    def test_search_raw(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            "query": {"match_all": {}}
        }
        rsp = self.app.assets.search(search=search, raw=True)
        path = rsp["hits"]["hits"][0]["_source"]["source"]["path"]
        assert path == "https://i.imgur.com/SSN26nN.jpg"

    @patch.object(PixmlClient, 'post')
    def test_search_wrapped(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            "query": {"match_all": {}}
        }
        rsp = self.app.assets.search(search=search)
        path = rsp[0].get_attr("source.path")
        assert path == "https://i.imgur.com/SSN26nN.jpg"
        assert 2 == rsp.size
        assert 0 == rsp.offset
        assert 2 == rsp.total

        # Iterate the result to test iteration.
        count = 0
        for item in rsp:
            count += 1
        assert count == 2


class FileImportTests(unittest.TestCase):

    def test_get_item_and_set_item(self):
        imp = FileImport("gs://zorroa-dev-data/image/pluto.png")
        imp["foo"] = "bar"
        assert imp["foo"] == "bar"


class ClipTests(unittest.TestCase):

    def test_page_clip(self):
        clip = Clip.page(10)
        assert clip.start == 10
        assert clip.stop == 10
        assert clip.type == 'page'

    def test_scene_clip(self):
        clip = Clip.scene(1.44, 2.25, "shot")
        assert clip.start == 1.44
        assert clip.stop == 2.25
        assert clip.type == 'scene'
        assert clip.timeline == 'shot'

    def test_create_clip(self):
        clip = Clip("scene", 1, 2, "faces")
        assert clip.start == 1
        assert clip.stop == 2
        assert clip.type == 'scene'
        assert clip.timeline == 'faces'
