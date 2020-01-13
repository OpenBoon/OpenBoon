#!/usr/bin/python
import unittest

from zmlpsdk import Context
from zmlp_core.core.generators import GcsBucketGenerator


class TestConsumer:
    def __init__(self):
        self.count = 0

    def accept(self, frame):
        self.count += 1


class GcsBucketGeneratorUnitTests(unittest.TestCase):

    def test_generate(self):
        consumer = TestConsumer()
        generator = GcsBucketGenerator()
        generator.set_context(Context(None, {"uri": 'gs://zorroa-dev-data'}, {}))
        generator.generate(consumer)
        assert consumer.count > 0
