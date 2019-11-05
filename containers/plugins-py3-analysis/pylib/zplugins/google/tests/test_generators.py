from mock import patch
from unittest import TestCase

from zplugins.google.generators import GcsBucketGenerator
from zsdk import Context, Asset


class Consumer:
    def __init__(self):
        self.count = 0

    def accept(self, frame):
        self.count += 1


class GcsBucketGeneratorUnitTests(TestCase):
    @patch('subprocess.check_output')
    @patch.object(Asset, 'set_source')
    def test_file_generator(self, set_source_patch, subprocess_patch):
        subprocess_patch.return_value = 'gs://zorroa-test-data/dummy.pdf\n'
        consumer = Consumer()
        generator = GcsBucketGenerator()
        generator.set_context(Context(None, {"bucket": 'gs://zorroa-test-data'}, {}))
        generator.generate(consumer)
        self.assertEquals(1, consumer.count)
