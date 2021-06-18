import json
import logging
import unittest

from boonsdk import Asset, StoredFile, FileImport, FileUpload, FileTypes, Label, Model
from boonsdk.client import to_json

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class AssetTests(unittest.TestCase):

    def setUp(self):
        self.test_files = [{
            "id": "assets/123/proxy/proxy_200x200.jpg",
            "category": "proxy",
            "name": "proxy_200x200.jpg",
            "mimetype": "image/jpeg",
            "attrs": {
                "width": 200,
                "height": 200
                }
            },
            {
                "id": "assets/123/proxy/proxy_400x400.jpg",
                "category": "proxy",
                "name": "proxy_400x400.jpg",
                "mimetype": "image/jpeg",
                "attrs": {
                    "width": 400,
                    "height": 400
                }
            }]

    def test_from_hit(self):
        asset = Asset.from_hit({
            "_id": "12345",
            "_source": {"foo": "bar"},
            "inner_hits": {
                "children": {
                    "hits": {
                        "hits": [
                            {
                                "_id": "45678",
                                "_source": {
                                    "bing": "bong"
                                }
                            }
                        ]
                    }
                }
            }
        })

        assert "12345" == asset.id
        assert "bar" == asset['foo']
        assert 1 == len(asset.get_inner_hits("children"))
        assert 0 == len(asset.get_inner_hits("corn"))

    def test_add_file(self):
        asset = Asset({"id": "123"})
        asset.add_file(StoredFile(self.test_files[0]))
        assert 1 == len(asset.get_files(name="proxy_200x200.jpg"))
        assert 1 == len(asset.get_files(name=["proxy_200x200.jpg"]))
        assert 0 == len(asset.get_files(name="spock"))

    def test_add_and_get_analysis(self):
        class Labels:
            def for_json(self):
                return {"predictions": [{"cat": 12345}]}

        asset = Asset({"id": "123"})
        asset.add_analysis("boonai-foo", Labels())
        analysis = asset.get_analysis("boonai-foo")
        assert len(analysis['predictions']) == 1
        assert analysis['predictions'][0]['cat'] == 12345

    def test_get_files_filter_name(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 1 == len(asset.get_files(name="proxy_200x200.jpg"))
        assert 1 == len(asset.get_files(name=["proxy_200x200.jpg"]))
        assert 0 == len(asset.get_files(name="spock"))

    def test_get_files_filter_category(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 2 == len(asset.get_files(category="proxy"))
        assert 2 == len(asset.get_files(category=["proxy"]))
        assert 0 == len(asset.get_files(name="face"))

    def test_get_files_filter_mimetype(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 2 == len(asset.get_files(mimetype="image/jpeg"))
        assert 2 == len(asset.get_files(mimetype=["image/", "video/mp4"]))
        assert 0 == len(asset.get_files(mimetype="video/mp4"))

    def test_get_files_by_extension(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 2 == len(asset.get_files(extension="jpg"))
        assert 0 == len(asset.get_files(extension="png"))
        assert 2 == len(asset.get_files(extension=["png", "jpg"]))

    def test_get_files_by_attrs(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 1 == len(asset.get_files(attrs={"width": 200}))
        assert 0 == len(asset.get_files(attrs={"width": 200, "height": 100}))

    def test_get_files_by_attr_keys(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 2 == len(asset.get_files(attr_keys=["width"]))
        assert 2 == len(asset.get_files(attr_keys="width"))
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
        top = asset.get_files(attr_keys=["width"], sort_func=lambda x: x.name)[0]
        assert top.name == "aaa.jpg"

    def test_get_files_sort_func_and_filtered(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)
        top = asset.get_files(attr_keys=["dog"], sort_func=lambda x: x.name)
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

    def test_get_thumbnail(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        f = asset.get_thumbnail(0)
        assert "assets/123/proxy/proxy_200x200.jpg" == f.id

        f = asset.get_thumbnail(1)
        assert "assets/123/proxy/proxy_400x400.jpg" == f.id

        f = asset.get_thumbnail(100)
        assert "assets/123/proxy/proxy_400x400.jpg" == f.id

    def test_get_predicted_labels(self):
        asset = Asset({"id": "123"})
        asset.set_attr("analysis.boonai-label-detection.predictions", [
            {
                "score": 0.99,
                "label": "house"
            },
            {
                "score": 0.5,
                "label": "house"
            },
            {
                "score": 0.1,
                "label": "market"
            }
        ])
        assert 3 == len(asset.get_predicted_labels("boonai-label-detection"))
        assert 1 == len(asset.get_predicted_labels("boonai-label-detection", min_score=0.85))

    def test_get_predicted_label(self):
        asset = Asset({"id": "123"})
        asset.set_attr("analysis.boonai-label-detection.predictions", [
            {
                "score": 0.99,
                "label": "house"
            },
            {
                "score": 0.5,
                "label": "broom"
            }
        ])
        # Get by index
        pred = asset.get_predicted_label("boonai-label-detection", 0)
        assert "house" == pred["label"]
        assert 0.99 == pred["score"]

        pred = asset.get_predicted_label("boonai-label-detection", "broom")
        assert "broom" == pred["label"]
        assert 0.5 == pred["score"]

    def test_get_analysis(self):
        asset = Asset({"id": "123"})
        asset.set_attr("analysis.boonai-label-detection.predictions", [
            {
                "score": 0.99,
                "label": "house"
            },
            {
                "score": 0.5,
                "label": "broom"
            }
        ])
        analysis = asset.get_analysis("boonai-label-detection")
        assert "house" == analysis['predictions'][0]['label']

    def test_get_analysis_by_model(self):
        asset = Asset({"id": "123"})
        asset.set_attr("analysis.dogs.predictions", [
            {
                "score": 0.99,
                "label": "house"
            },
            {
                "score": 0.5,
                "label": "broom"
            }
        ])

        model = Model({"id": "123", "moduleName": "dogs"})
        analysis = asset.get_analysis(model)
        assert "house" == analysis['predictions'][0]['label']

    def test_extension(self):
        asset = Asset({"id": "123", "document": {"source": {"extension": "JPG"}}})
        assert asset.extension == "jpg"


class FileImportTests(unittest.TestCase):

    def test_get_item_and_set_item(self):
        imp = FileImport("gs://zorroa-dev-data/image/pluto.png")
        imp["foo"] = "bar"
        assert imp["foo"] == "bar"

    def test_for_json(self):
        imp = FileImport('gs://zorroa-dev-data/image/pluto.png',
                         label=Label("12345", "dog"))

        d = json.loads(to_json(imp))
        assert 'gs://zorroa-dev-data/image/pluto.png' == d['uri']
        assert {} == d['custom']
        assert '12345' == d['label']['datasetId']
        assert 'dog' == d['label']['label']


class FileUploadTests(unittest.TestCase):

    def test_for_json(self):
        imp = FileUpload(__file__,
                         page=1,
                         label=Label("12345", "dog"))

        d = json.loads(to_json(imp))
        assert __file__ == d['uri']
        assert 1 == d['page']
        assert '12345' == d['label']['datasetId']
        assert 'dog' == d['label']['label']


class FileTypesTests(unittest.TestCase):

    def test_resolve_images(self):
        exts = FileTypes.resolve('images')
        assert 'bmp' in exts

    def test_resolve_videos(self):
        exts = FileTypes.resolve('videos')
        assert 'mp4' in exts

    def test_resolve_documents(self):
        exts = FileTypes.resolve('documents')
        assert 'doc' in exts

    def test_resolve_ext(self):
        exts = FileTypes.resolve(['exr', 'mp4', 'doc'])
        assert 'exr' in exts
        assert 'mp4' in exts
        assert 'doc' in exts
        assert 3 == len(exts)

    def test_resolve_mixed(self):
        exts = FileTypes.resolve(['jpg', 'videos'])
        assert 'jpg' in exts
        assert 'mp4' in exts
