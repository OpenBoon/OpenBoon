import logging
import unittest

from zmlp import Asset, Element
from zmlp.asset import FileImport, Clip

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


class ElementTests(unittest.TestCase):
    stored_file = {
        'name': 'cat_object.jpg',
        'category': 'element',
        'attrs': {
            'width': 300,
            'height': 300
        }
    }

    def test_create_min_element(self):
        element = Element('object', labels='cat')
        assert element.type == 'object'
        assert element.labels == ['cat']
        assert element.proxy is None
        assert element.rect is None
        assert element.score is None

    def test_create_rect_element_with_file(self):
        element = Element('object', labels='cat', proxy=self.stored_file)

        assert element.type == 'object'
        assert element.labels == ['cat']
        assert element.proxy == 'element/cat_object.jpg'

    def test_create_rect_element_with_nw_region(self):
        element = Element('object', labels='cat',
                          rect=[0, 0, 100, 100], proxy=self.stored_file)
        assert element.regions == ['NW']

    def test_create_rect_element_with_sw_region(self):
        element = Element('object', labels='cat',
                          rect=[0, 175, 10, 200], proxy=self.stored_file)
        assert element.regions == ['SW']

    def test_create_rect_element_with_ne_region(self):
        element = Element('object', labels='cat',
                          rect=[175, 0, 200, 10], proxy=self.stored_file)
        assert element.regions == ['NE']

    def test_create_rect_element_with_se_region(self):
        element = Element('object', labels='cat',
                          rect=[200, 200, 300, 300], proxy=self.stored_file)
        assert element.regions == ['SE']

    def test_create_rect_element_with_center_region(self):
        element = Element('object', labels='cat',
                          rect=[10, 10, 250, 250], proxy=self.stored_file)
        assert len(element.regions) == 5
        assert 'CENTER' in element.regions
