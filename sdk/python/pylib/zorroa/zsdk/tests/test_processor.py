#!/usr/bin/env python
import unittest
import sys

from zorroa import zsdk
from zorroa.zsdk.tfixtures import TestExecutor


class Executor:
    def __init__(self):
        self.expanded = 0
        self.last_script = None

    def write(self, script):
        self.last_script = script
        self.expanded += 1


class ReactorTests(unittest.TestCase):

    def test_error(self):
        proc = zsdk.DocumentProcessor()
        ex = TestExecutor(proc)
        r = zsdk.Reactor(ex, {})
        frame = zsdk.Frame(zsdk.Document())

        try:
            raise Exception("TEST")
        except Exception as e:
            r.error(frame, proc, e, True, "unittest", sys.exc_info()[2])
        event = ex.script
        assert "Exception: TEST" == event["payload"]["message"]
        assert len(event["payload"]["stackTrace"]) == 1
        assert "className" in event["payload"]["stackTrace"][0]
        assert "methodName" in event["payload"]["stackTrace"][0]
        assert "file" in event["payload"]["stackTrace"][0]
        assert "lineNumber" in event["payload"]["stackTrace"][0]

    def test_reactor_check_expand(self):
        e = Executor()
        r = zsdk.Reactor(e, {})
        frame = zsdk.Frame(zsdk.Document())
        for i in range(0, 30):
            r.add_expand_frame(frame, zsdk.ExpandFrame(zsdk.Document()))
            r.check_expand()
        self.assertEquals(30, len(r.expand_frames))
        self.assertEquals(5, r.check_expand(batch_size=6, force=True))

    def test_reactor_check_expand_copy_metadata(self):
        e = Executor()
        r = zsdk.Reactor(e, 50)
        frame = zsdk.Frame(zsdk.Document())
        frame.asset.set_attr('shotgun.project.name', 'Test Project')
        frame.asset.set_attr('notes', 'This is a test.')
        r.add_expand_frame(frame, zsdk.ExpandFrame(zsdk.Document(),
                                                   copy_attrs=['shotgun', 'notes']))
        r.check_expand(force=True)
        metadata = e.last_script['payload']['over'][0]['document']
        self.assertEqual(metadata.get('shotgun'), {'project': {'name': 'Test Project'}})
        self.assertEqual(metadata.get('notes'), 'This is a test.')

    def test_reactor_check_expand_copy_tmp_set_metadata(self):
        e = Executor()
        r = zsdk.Reactor(e, 50)
        frame = zsdk.Frame(zsdk.Document())
        frame.asset.set_attr('shotgun.project.name', 'Test Project')
        frame.asset.set_attr('notes', 'This is a test.')
        frame.asset.set_attr('tmp.copy_attrs_to_clip', ["notes"])
        child = zsdk.Document()
        r.add_expand_frame(frame, zsdk.ExpandFrame(child))
        r.check_expand(force=True)
        self.assertEqual('This is a test.', child.get_attr("notes"))
        self.assertEqual(None, child.get_attr("shotgun"))

    def test_reactor_check_expand_force(self):
        e = Executor()
        r = zsdk.Reactor(e, 50)
        frame = zsdk.Frame(zsdk.Document())
        for i in range(0, 60):
            r.add_expand_frame(frame, zsdk.ExpandFrame(zsdk.Document()))
        # Its 1 batch since the call to add_expand_frame processed the first 50 automatically.
        self.assertEquals(1, r.check_expand(force=True))
        self.assertEquals(0, len(r.expand_frames))

    def test_clear_expand_frames(self):
        e = Executor()
        r = zsdk.Reactor(e, {})
        frame1 = zsdk.Frame(zsdk.Document({"id": "456"}))
        frame2 = zsdk.Frame(zsdk.Document({"id": "123"}))
        for i in range(0, 10):
            r.add_expand_frame(frame1, zsdk.ExpandFrame(zsdk.Document()))
            r.add_expand_frame(frame2, zsdk.ExpandFrame(zsdk.Document()))
        r.clear_expand_frames(frame1.asset.id)
        # Assert that the 10 child frames from frame1 are gone.
        assert len(r.expand_frames) == 10
        assert r.expand_frames[0][0].asset.id == "123"


class ArgTests(unittest.TestCase):

    def test_simple_expr_args(self):
        p = zsdk.Processor()
        p.add_arg(zsdk.Argument("str_arg", "string"))
        p.add_arg(zsdk.Argument("int_arg", "int"))
        p.add_arg(zsdk.Argument("float_arg", "int"))

        args = {
            "str_arg": {"_expr_": "'a'"},
            "int_arg": {"_expr_": "1+1"},
            "float_arg": {"_expr_": "2.0 * 2.0"}
        }

        p.set_context(zsdk.Context(None, args, {}))
        p.set_expression_values(zsdk.Frame(zsdk.Document()))

        self.assertEquals("a", p.arg_value("str_arg"))
        self.assertEquals(2, p.arg_value("int_arg"))
        self.assertEquals(4.0, p.arg_value("float_arg"))

    def test_expr_failure(self):
        p = zsdk.Processor()
        p.add_arg(zsdk.Argument("str_arg", "string"))

        args = {
            "str_arg": {"_expr_": "duh"},
        }

        p.set_context(zsdk.Context(None, args, {}))
        self.assertRaises(zsdk.exception.UnrecoverableProcessorException,
                          p.set_expression_values, zsdk.Frame(zsdk.Document()))

    def test_ignored_expr_failure(self):
        p = zsdk.Processor()
        p.add_arg(zsdk.Argument("str_arg", "string"))

        args = {
            "str_arg": {"_expr_": "duh", "ignore_error": True},
        }
        p.set_context(zsdk.Context(None, args, {}))
        p.set_expression_values(zsdk.Frame(zsdk.Document()))
        self.assertEquals("<Unset Value>", str(p.arg_value("str_arg")))

    def test_simple_doc_args(self):
        p = zsdk.Processor()
        p.add_arg(zsdk.Argument("str_arg", "string"))
        p.add_arg(zsdk.Argument("int_arg", "int"))
        p.add_arg(zsdk.Argument("float_arg", "int"))

        args = {
            "str_arg": {"_expr_": "_doc.get_attr('str_value') + ' is old'"},
            "int_arg": {"_expr_": "_doc.get_attr('int_value') + 1"},
            "float_arg": {"_expr_": "_doc.get_attr('float_value') + 1"}
        }

        doc = zsdk.Document()
        doc.set_attr("str_value", "bob")
        doc.set_attr("int_value", 1000)
        doc.set_attr("float_value", 3.14)

        p.set_context(zsdk.Context(None, args, {}))
        p.set_expression_values(zsdk.Frame(doc))

        self.assertEquals("bob is old", p.arg_value("str_arg"))
        self.assertEquals(1001, p.arg_value("int_arg"))
        self.assertAlmostEqual(4.14, p.arg_value("float_arg"))

    def test_scalar_args(self):
        p = zsdk.Processor()
        p.add_arg(zsdk.Argument("str_arg", "string"))
        p.add_arg(zsdk.Argument("int_arg", "int"))
        p.add_arg(zsdk.Argument("float_arg", "int"))

        args = {
            "str_arg": "bar",
            "int_arg": 1000,
            "float_arg": 3.14
        }

        p.set_context(zsdk.Context(None, args, {}))
        self.assertEquals("bar", p.arg_value("str_arg"))
        self.assertEquals(1000, p.arg_value("int_arg"))
        self.assertEquals(3.14, p.arg_value("float_arg"))

    def test_nested_struct_args(self):
        p = zsdk.Processor()

        struct_arg = zsdk.Argument("struct_arg", "struct").add_arg(
            zsdk.Argument("str_arg", "string"),
            zsdk.Argument("int_arg", "int"),
            zsdk.Argument("float_arg", "float"))
        p.add_arg(struct_arg)

        args = {
            "struct_arg": {
                "str_arg": "bar",
                "int_arg": 1000,
                "float_arg": 3.14
            }
        }

        p.set_context(zsdk.Context(None, args, {}))
        self.assertEquals("bar", p.arg_value("struct_arg")["str_arg"].value)
        self.assertEquals(1000, p.arg_value("struct_arg")["int_arg"].value)
        self.assertEquals(3.14, p.arg_value("struct_arg")["float_arg"].value)

    def test_scalar_list_args(self):
        p = zsdk.Processor()
        list_arg = zsdk.Argument("list_arg", "list")
        p.add_arg(list_arg)
        args = {
            "list_arg": ["a", "b", "c"]
        }

        p.set_context(zsdk.Context(None, args, {}))
        self.assertEquals(["a", "b", "c"], p.arg_value("list_arg"))

    def test_nested_list_of_struct_arg(self):

        p = zsdk.Processor()
        list_arg = zsdk.Argument("list_arg", "list").add_arg(
            zsdk.Argument("str_arg", "string", default="bilbo"),
            zsdk.Argument("int_arg", "int", default=9999),
            zsdk.Argument("float_arg", "float", default=1.21))
        p.add_arg(list_arg)

        args = {
            "list_arg": [
                {
                    "str_arg": "bar",
                    "int_arg": 1000,
                    "float_arg": 3.14
                }
            ]
        }

        p.set_context(zsdk.Context(None, args, {}))
        self.assertEquals("bar", p.arg_value("list_arg")[0]["str_arg"])
        self.assertEquals(1000, p.arg_value("list_arg")[0]["int_arg"])
        self.assertEquals(3.14, p.arg_value("list_arg")[0]["float_arg"])

    def test_nested_list_of_struct_arg_defaults(self):

        p = zsdk.Processor()
        # Defines a list of stuct, where each struct has a str_arg, int_arg, and float_arg
        list_arg = zsdk.Argument("list_arg", "list").add_arg(
            zsdk.Argument("struct_arg", "struct").add_arg(
                zsdk.Argument("str_arg", "string", default="bilbo"),
                zsdk.Argument("int_arg", "int", default=9999),
                zsdk.Argument("float_arg", "float", default=1.21)))
        p.add_arg(list_arg)

        args = {
            "list_arg": [
                {
                    "str_arg": "bar",
                    "int_arg": 1000,
                    "float_arg": 3.14
                },
                # Note: all values are missing here so they
                # will be set as default
                {

                }
            ]
        }

        p.set_context(zsdk.Context(None, args, {}))
        self.assertEquals("bilbo", p.arg_value("list_arg")[1]["str_arg"])
        self.assertEquals(9999, p.arg_value("list_arg")[1]["int_arg"])
        self.assertEquals(1.21, p.arg_value("list_arg")[1]["float_arg"])


if __name__ == '__main__':
    unittest.main()
