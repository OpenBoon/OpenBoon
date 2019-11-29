from pixml.analysis import Frame
from pixml.analysis.testing import PluginUnitTestCase, TestAsset
from pixml_core.core.processors import SetAttributesProcessor

MOCK_PERMISSION = {
    'authority': 'string',
    'description': 'string',
    'fullName': 'string',
    'id': 'string',
    'name': 'string',
    'type': 'string'
}


class ProcessorsUnitTestCase(PluginUnitTestCase):

    def test_set_attributes_processor_set_attrs(self):
        frame = Frame(TestAsset("cat.jpg"))
        ih = self.init_processor(SetAttributesProcessor(), args={'attrs': {'foo.bar': 1}})
        ih.process(frame)
        self.assertEquals(1, frame.asset.get_attr("foo.bar"))

    def test_set_attributes_processor_remove_attrs(self):
        frame = Frame(TestAsset("cat.jpg", {"foo": "bar"}))
        self.assertEquals("bar", frame.asset.get_attr("foo"))
        ih = self.init_processor(SetAttributesProcessor(), args={'remove_attrs': ['foo']})
        ih.process(frame)
        self.assertIsNone(frame.asset.get_attr("foo"))
