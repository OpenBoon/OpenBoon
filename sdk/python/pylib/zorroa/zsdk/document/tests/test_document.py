#!/usr/bin/env python

import unittest

from zorroa import zsdk
from zorroa.zsdk.testing import zorroa_test_data


class DocumentUnitTests(unittest.TestCase):

    def test_document_set_attr(self):
        p = zsdk.Document()
        p.set_attr("foo.bar", 100)
        self.assertTrue(100, p.get_attr("foo.bar"))

    def test_document_set_attr_with_field_edit(self):
        p = zsdk.Document()
        p.set_attr("foo", "bar")
        p.set_attr("system.fieldEdits", ["foo"])
        p.set_attr("foo", "bingo")
        self.assertEquals("bar", p.get_attr("foo"))

    def test_document_deep_field_edit_failure(self):
        p = zsdk.Document()
        p.set_attr("foo", {"bacon": {"lettuce": "car"}})
        p.set_attr("system.fieldEdits", ["foo"])
        self.assertEquals("car", p.get_attr("foo.bacon.lettuce"))
        self.assertFalse(p.set_attr("foo.bacon.lettuce", "bingo"))
        self.assertEquals("car", p.get_attr("foo.bacon.lettuce"))

    def test_document_deep_field_edit_success(self):
        p = zsdk.Document()
        p.set_attr("foo", {"bacon": {"lettuce": "car"}})
        self.assertEquals("car", p.get_attr("foo.bacon.lettuce"))
        self.assertTrue(p.set_attr("foo.bacon.lettuce", "bingo"))
        self.assertEquals("bingo", p.get_attr("foo.bacon.lettuce"))

    def test_document_get_attr(self):
        p = zsdk.Document()
        self.assertFalse(p.get_attr("foo.bar"))
        p.set_attr("foo.bar", 100)
        self.assertEquals(100, p.get_attr("foo.bar"))

    def test_document_get_ref(self):
        doc = zsdk.Document()
        doc.set_attr('source.path', '/some/where.txt')
        doc.set_attr('source.idkey', 'start=1&stop=2')
        assert doc.get_ref() == '/some/where.txt?start=1&stop=2'

    def test_get_array_attr(self):
        p = zsdk.Document()
        p.set_attr("foo", [1, 2, 3, 4, 5])
        self.assertEquals(2, p.get_array_attr("foo", [1]))
        self.assertEquals(2, p.get_array_attr("foo", [10, 1]))
        self.assertEquals(None, p.get_array_attr("foo", 100))

        p.set_attr("foo", {"foo": "bar", "bing": "bang"})
        self.assertEquals("bar", p.get_array_attr("foo", ["foo"]))
        self.assertEquals("bang", p.get_array_attr("foo", ["china", "bing"]))
        self.assertEquals(None, p.get_array_attr("foo", "bob"))

    def test_document_set_attr_nested_dict(self):
        p = zsdk.Document()
        value = {
            "foo": "bar",
            "bing": [1, 2, 3],
            "bong": {
                "ching": "chang"
            }
        }
        p.set_attr("sizzle.song", value)
        self.assertEquals(value, p.get_attr("sizzle.song"))

    def test_document_attr_exists(self):
        p = zsdk.Document()
        self.assertFalse(p.attr_exists("foo.bar"))
        p.set_attr("foo.bar", 100)
        self.assertTrue(p.attr_exists("foo.bar"))

    def test_clip(self):
        clip = zsdk.Clip(1, "flipbook", 1, 10, name="foo")
        self.assertEquals(1, clip.start)
        self.assertEquals(10, clip.stop)
        self.assertEquals(['start=1.000', 'stop=10.000'], clip.tokens())

    def test_source_init_with_clip(self):
        clip = zsdk.Clip(1, "flipbook", 1, 10, name="foo")
        s = zsdk.Asset(zorroa_test_data("images/set01/faces.jpg"), clip)
        self.assertEquals(1, clip.start)
        self.assertEquals(10, clip.stop)
        self.assertEquals(s.get_attr("source.path"), zorroa_test_data("images/set01/faces.jpg"))

    def test_source_init_with_clip_check_id(self):
        clip = zsdk.Clip(1, "flipbook", 0, 10, name="foo")
        s2 = zsdk.Asset(zorroa_test_data("images/set01/faces.jpg"), clip)
        s1 = zsdk.Asset(zorroa_test_data("images/set01/faces.jpg"))
        self.assertNotEquals(s1.id, s2.id)

    def test_set_keywords(self):
        p = zsdk.Document()
        p.add_keywords("media", ["shamoo"])
        p.add_keywords("media", ["fish"])
        self.assertTrue("shamoo" in p.get_attr("media.keywords"))
        self.assertTrue("fish" in p.get_attr("media.keywords"))
        self.assertFalse("taco" in p.get_attr("media.keywords"))

    def test_set_content(self):
        p = zsdk.Document()
        p.add_content("media", ["shamoo is an orca"])
        p.add_content("media", ["fish are cool"])
        self.assertTrue("shamoo is an orca" in p.get_attr("media.content"))
        self.assertTrue("fish are cool" in p.get_attr("media.content"))
        self.assertFalse("taco" in p.get_attr("media.content"))

    def test_set_attr_with_forjson(self):
        clip = zsdk.Clip(1, "flipbook", 0, 10, name="foo")
        p = zsdk.Document()
        p.set_attr("clip", clip)
        self.assertEquals(dict, type(p.get_attr("clip")))

    def test_for_json(self):
        clip = zsdk.Clip(1, "flipbook", 0, 10, name="foo")
        self.assertEqual(clip.for_json(), {'name': 'foo', 'type': 'flipbook', 'parent': 1,
                                           'start': 0.0, 'stop': 10.0, 'length': 11.0})

    def test_extend_list_attr(self):
        document = zsdk.Document()
        document.set_attr('list', [1])
        document.set_attr('set', {1})
        items = [2, 3]

        # Add a list to a list.
        document.extend_list_attr('list', items)
        self.assertEqual(document.get_attr('list'), [1, 2, 3])

        # Add a list to a set.
        document.extend_list_attr('set', items)
        self.assertEqual(document.get_attr('set'), {1, 2, 3})

        # Add a set to a list.
        items = {4, 5}
        document.extend_list_attr('list', items)
        self.assertEqual(document.get_attr('list'), [1, 2, 3, 4, 5])

        # Add a set to a set.
        document.extend_list_attr('set', items)
        self.assertEqual(document.get_attr('set'), {1, 2, 3, 4, 5})

    def test_get_attr_dot_notation_not_dict(self):
        document = zsdk.Document()
        document.set_attr('string', 'a string')
        self.assertIsNone(document.get_attr('string.name'))
        self.assertEqual(document.get_attr('string.name', 'default'), 'default')

    def test_del_attr(self):
        p = zsdk.Document()
        p.set_attr("foo.bar", 100)
        self.assertTrue(p.attr_exists("foo.bar"))
        self.assertTrue(p.del_attr("foo"))
        self.assertFalse(p.attr_exists("foo.bar"))
        self.assertFalse(p.attr_exists("foo"))

    def test_del_attr_with_field_edits(self):
        p = zsdk.Document()
        p.set_attr("foo.bar", 100)
        p.set_attr("system.fieldEdits", ["foo.bar.bing"])
        self.assertTrue(p.attr_exists("foo.bar"))
        self.assertFalse(p.del_attr("foo"), "The attribute should have failed to be deleted")
        self.assertTrue(p.attr_exists("foo.bar"))
        self.assertTrue(p.attr_exists("foo"))

    def test_get_field_edits(self):
        p = zsdk.Document()
        p.set_attr("foo.bar", 100)
        p.set_attr("system.fieldEdits", ["foo.bar"])
        self.assertEquals({"foo.bar": 100}, p.get_field_edits())
