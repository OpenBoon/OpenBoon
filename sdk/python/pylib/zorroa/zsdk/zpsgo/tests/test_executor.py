#!/usr/bin/env python
import unittest
import os

from mock import patch

from zorroa.zsdk import Asset, Frame, Reactor
from zorroa.zsdk.zpsgo import ZpsExecutor
from zorroa.zsdk.zpsgo.executor import is_frame_file_type_filtered, ProcessorEnvironment
from zorroa.zsdk.tfixtures import zorroa_test_data

FACES_JPG = zorroa_test_data("images/set01/faces.jpg")


class TestProcessorEnvironment(unittest.TestCase):

    def test_nested_env(self):
        with ProcessorEnvironment({"color": "brown"}):
            assert os.environ["color"] == "brown"
            with ProcessorEnvironment({"color": "blue"}):
                assert os.environ["color"] == "blue"
            assert os.environ["color"] == "brown"
        assert not os.environ.get("color")


class Tests(unittest.TestCase):

    def setUp(self):
        super(Tests, self).setUp()

    def test_initialize(self):
        """
        Test initialize function.
        """
        zps = ZpsExecutor()
        segment = zps.initialize(None, [
                    {
                        "className": "zorroa.zsdk.zpsgo.tests.test_processors.GroupProcessor",
                        "args": {}
                    }
                ], {}, {})
        self.assertEquals(1, len(segment["processors"]))
        self.assertIsNotNone(segment["processors"][0].context)

    def test_initialize_with_over_data(self):
        """
        Test initialize function with over data.  Ensure that it gets iterated which
        means creating an intenral DocumentGenerator.
        """
        zps = ZpsExecutor()
        result = zps.run(
            {
                "settings": {
                    "inline": True
                },
                "over": [
                    {"document": {"file": "peter.txt"}},
                    {"document": {"file": "paul.txt"}},
                    {"document": {"file": "mary.txt"}}
                ],
                "execute": [
                    {
                        "className": "zorroa.zsdk.zpsgo.tests.test_processors.GroupProcessor"
                    }
                ]
            })
        self.assertEquals(3, result.frame_count)
        self.assertEquals(3, result.execute_count)

    def test_run(self):
        """
        Run a simple ZPS structure
        """
        zps = ZpsExecutor()
        result = zps.run(
            {
                "settings": {
                    "inline": True,
                },
                "generate": [
                    {"className": "zorroa.zsdk.zpsgo.tests.test_processors.FileGenerator",
                     "args": {"paths": [FACES_JPG]}}
                ],
                "execute": [
                    {"className": "zorroa.zsdk.zpsgo.tests.test_processors.GroupProcessor", "args": {}}
                ]
            }, )
        self.assertEquals(1, result.frame_count)
        self.assertEquals(1, result.execute_count)

    @patch.object(Reactor, 'error')
    def test_execute_with_env(self, mock):
        """
        Run a simple ZPS structure
        """
        zps = ZpsExecutor()
        result = zps.run(
            {
                "settings": {
                    "inline": True,
                    "strict": True
                },
                "generate": [
                    {"className": "zorroa.zsdk.zpsgo.tests.test_processors.FileGenerator",
                     "args": {"paths": [FACES_JPG]}}
                ],
                "execute": [
                    {"className": "zorroa.zsdk.zpsgo.tests.test_processors.TestEnvironmentProcessor",
                     "args": {"key": "FOO_BAR", "value": None}},
                    {"className": "zorroa.zsdk.zpsgo.tests.test_processors.TestEnvironmentProcessor",
                     "args": {"key": "FOO_BAR", "value": "123"},
                     "env": {"FOO_BAR": "1234"}},
                    {"className": "zorroa.zsdk.zpsgo.tests.test_processors.TestEnvironmentProcessor",
                     "args": {"key": "FOO_BAR", "value": None}},
                ]
            }, )
        self.assertEquals(1, result.frame_count)
        self.assertEquals(2, result.execute_count)
        assert mock.called

    @patch.object(Reactor, 'error')
    def test_exception(self, mock):
        zps = ZpsExecutor()
        zps.run(
            {
                "settings": {
                    "inline": True,
                },
                "over": [{}],
                "execute": [
                    {
                        "className":
                            "zorroa.zsdk.zpsgo.tests.test_processors.TestExceptionProcessor", "args": {}
                     }
                ]
            })
        assert mock.called

    def test_frame_skip(self):
        """
        Run a simple ZPS structure
        """
        test_files = [FACES_JPG]
        zps = ZpsExecutor()
        result = zps.run(
            {
                "settings": {
                    "inline": True,
                },
                "generate": [
                    {"className": "zorroa.zsdk.zpsgo.tests.test_processors.FileGenerator",
                     "args": {"paths": test_files}}
                ],
                "execute": [
                    {"className": "zorroa.zsdk.zpsgo.tests.test_processors.SkipProcessor",
                     "args": {}}
                ]
            })
        self.assertEquals(1, result.frame_count)
        self.assertEquals(1, result.execute_count)

    def test_file_type_skip(self):
        """
        Run a simple ZPS structure
        """
        test_files = [FACES_JPG]
        zps = ZpsExecutor()
        result = zps.run(
            {
                "settings": {
                    "inline": True
                },
                "generate": [
                    {"className": "zorroa.zsdk.zpsgo.tests.test_processors.FileGenerator",
                     "args": {"paths": test_files}}
                ],
                "execute": [
                    {"className": "zorroa.zsdk.zpsgo.tests.test_processors.GroupProcessor",
                     "args": {"fileTypes": ["pdf"]}}
                ]
            })
        self.assertEquals(0, result.frame_count)
        self.assertEquals(0, result.execute_count)

    def test_file_type_process(self):
        """
        Run a simple ZPS structure
        """
        test_files = [FACES_JPG]
        zps = ZpsExecutor()
        result = zps.run(
            {
                "settings": {
                    "inline": True
                },
                "generate": [
                    {
                        "className": "zorroa.zsdk.zpsgo.tests.test_processors.FileGenerator",
                        "args": {"paths": test_files}
                    }
                ],
                "execute": [
                    {"className": "zorroa.zsdk.zpsgo.tests.test_processors.GroupProcessor",
                     "file_types": ["jpg"], "args": {}}
                ]
            })
        self.assertEquals(1, result.frame_count)
        self.assertEquals(1, result.execute_count)

    def test_run_with_global_expression(self):
        """
        Run a simple ZPS structure
        """
        test_expr = {"_expr_": "_ctx.global_args['paths']"}
        zps = ZpsExecutor()
        result = zps.run(
            {
                "settings": {
                    "inline": True
                },
                "generate": [
                    {
                        "className": "zorroa.zsdk.zpsgo.tests.test_processors.FileGenerator",
                        "args": {"paths": test_expr}
                    }
                ],
                "execute": [
                    {
                        "className": "zorroa.zsdk.zpsgo.tests.test_processors.GroupProcessor", "args": {}
                    }
                ]
            }, {"paths": [FACES_JPG]})
        self.assertEquals(1, result.frame_count)
        self.assertEquals(1, result.execute_count)

    def test_expression(self):
        """
        Run a simple ZPS structure
        """
        test_files = [FACES_JPG]
        expr = {"_expr_": "_doc.get_attr('source.filename')"}
        zps = ZpsExecutor()
        result = zps.run(
            {
                "settings": {
                    "inline": True
                },
                "generate": [
                    {
                        "className": "zorroa.zsdk.zpsgo.tests.test_processors.FileGenerator",
                        "args": {"paths": test_files}
                    }
                ],
                "execute": [
                    {
                        "className": "zorroa.zsdk.zpsgo.tests.test_processors.TestSetValueProcessor",
                        "args": {"value": expr}},
                    {
                        "className": "zorroa.zsdk.zpsgo.tests.test_processors.TestCollector"
                    }
                ]
            }, teardown=False)
        self.assertEquals(1, len(result.procs["processors"][1].frames))
        self.assertEquals("faces.jpg",
                          result.procs["processors"][1].frames[0].asset.get_attr("test.value"))

    def test_run_subexecute(self):
        """
        Run a nested ZPS structure
        """
        test_files = [FACES_JPG]
        zps = ZpsExecutor()
        result = zps.run(
            {
                "settings": {
                    "inline": True
                },
                "generate": [
                    {"className": "zorroa.zsdk.zpsgo.tests.test_processors.FileGenerator",
                     "args": {"paths": test_files}}
                ],
                "execute": [
                    {
                        "className": "zorroa.zsdk.zpsgo.tests.test_processors.GroupProcessor",
                        "execute": [
                            {
                                "className": "zorroa.zsdk.zpsgo.tests.test_processors.GroupProcessor",
                            }
                        ]
                    }
                ]
            })
        self.assertEquals(1, result.frame_count)
        self.assertEquals(2, result.execute_count)

    def test_run_filtered_subexecute(self):
        """
        Run a nested ZPS structure
        """
        test_files = [FACES_JPG]
        zps = ZpsExecutor()
        result = zps.run(
            {
                "settings": {
                    "inline": True
                },
                "generate": [
                    {
                        "className": "zorroa.zsdk.zpsgo.tests.test_processors.FileGenerator",
                        "args": {
                            "paths": test_files
                            }
                        }
                ],
                "execute": [
                    {
                        "className": "zorroa.zsdk.zpsgo.tests.test_processors.GroupProcessor",
                        "filters": [
                            {"expr": "_doc.get_attr('source.extension') == 'png'"}
                        ],
                        "execute": [
                            {
                                "className":
                                    "zorroa.zsdk.zpsgo.tests.test_processors.GroupProcessor",

                            }
                        ]
                    }
                ]
            })
        self.assertEquals(1, result.frame_count)
        # The filter stops the executes from happening
        self.assertEquals(0, result.execute_count)

    def test_is_frame_file_type_filtered(self):
        frame = Frame(Asset(zorroa_test_data("images/set01/faces.JPG")))
        assert not is_frame_file_type_filtered(frame, ['jpg'])
