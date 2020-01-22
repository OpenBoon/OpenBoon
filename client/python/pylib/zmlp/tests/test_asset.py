import logging
import os
import unittest
from unittest.mock import patch

from zmlp import Asset
from zmlp import ZmlpClient, app_from_env
from zmlp.asset import FileImport, FileUpload, Clip

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

        self.mock_import_result = {
            "bulkResponse": {
                "took": 15,
                "errors": False,
                "items": [{
                    "create": {
                        "_index": "yvqg1901zmu5bw9q",
                        "_type": "_doc",
                        "_id": "dd0KZtqyec48n1q1fniqVMV5yllhRRGx",
                        "_version": 1,
                        "result": "created",
                        "forced_refresh": True,
                        "_shards": {
                            "total": 1,
                            "successful": 1,
                            "failed": 0
                        },
                        "_seq_no": 0,
                        "_primary_term": 1,
                        "status": 201
                    }
                }]
            },
            "failed": [],
            "created": ["dd0KZtqyec48n1q1fniqVMV5yllhRRGx"],
            "jobId": "ba310246-1f87-1ece-b67c-be3f79a80d11"
        }

        # A mock search result used for asset search tests
        self.mock_search_result = {
            "took": 4,
            "timed_out": False,
            "hits": {
                "total": {"value": 2},
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

    @patch.object(ZmlpClient, 'post')
    def test_import_files(self, post_patch):
        post_patch.return_value = self.mock_import_result
        assets = [FileImport("gs://zorroa-dev-data/image/pluto.png")]
        rsp = self.app.assets.batch_import_files(assets)
        assert rsp["created"][0] == "dd0KZtqyec48n1q1fniqVMV5yllhRRGx"

    @patch.object(ZmlpClient, 'get')
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

    @patch.object(ZmlpClient, 'upload_files')
    def test_upload_assets(self, post_patch):
        post_patch.return_value = self.mock_import_result

        path = os.path.dirname(__file__) + "/../../../../../test-data/images/set01/toucan.jpg"
        assets = [FileUpload(path)]
        rsp = self.app.assets.batch_upload_files(assets)
        assert rsp["created"][0] == "dd0KZtqyec48n1q1fniqVMV5yllhRRGx"

    @patch.object(ZmlpClient, 'post')
    def test_search_raw(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            "query": {"match_all": {}}
        }
        rsp = self.app.assets.search(search=search, raw=True)
        path = rsp["hits"]["hits"][0]["_source"]["source"]["path"]
        assert path == "https://i.imgur.com/SSN26nN.jpg"

    @patch.object(ZmlpClient, 'post')
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

    @patch.object(ZmlpClient, 'put')
    def test_index(self, put_patch):
        asset = Asset({'id': '123'})
        asset.set_attr('foo.bar', 'bing')
        self.app.assets.index(asset)
        args = put_patch.call_args_list
        body = args[0][0][1]
        assert body['foo'] == {'bar': 'bing'}

    @patch.object(ZmlpClient, 'post')
    def test_batch_index(self, post_patch):
        asset = Asset({'id': '123'})
        asset.set_attr('foo.bar', 'bing')
        self.app.assets.batch_index([asset])
        args = post_patch.call_args_list
        body = args[0][0][1]
        assert body['123'] == {'foo': {'bar': 'bing'}}

    @patch.object(ZmlpClient, 'post')
    def test_update(self, post_patch):
        req = {
            'foo': 'bar'
        }

        self.app.assets.update('123', req)
        args = post_patch.call_args_list
        body = args[0][0][1]
        assert body['doc'] == {'foo': 'bar'}

    @patch.object(ZmlpClient, 'post')
    def test_batch_update(self, post_patch):
        req = {
            'abc123': {
                'doc': {
                    'foo': 'bar'
                }
            }
        }

        self.app.assets.batch_update(req)
        args = post_patch.call_args_list
        body = args[0][0][1]
        assert body['abc123'] == {'doc': {'foo' : 'bar'}}


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
