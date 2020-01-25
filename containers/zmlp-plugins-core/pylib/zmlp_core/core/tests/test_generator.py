#!/usr/bin/python
import unittest
from unittest.mock import patch

from zmlp import ZmlpClient
from zmlpsdk import Context
from zmlp_core.core.generators import GcsBucketGenerator, AssetSearchGenerator


class TestConsumer:
    def __init__(self):
        self.count = 0

    def accept(self, _):
        self.count += 1


class GcsBucketGeneratorUnitTests(unittest.TestCase):

    def test_generate(self):
        consumer = TestConsumer()
        generator = GcsBucketGenerator()
        generator.set_context(Context(None, {'uri': 'gs://zorroa-dev-data'}, {}))
        generator.generate(consumer)
        assert consumer.count > 0


class AssetSearchGeneratorTests(unittest.TestCase):

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_generate(self, post_patch, del_patch):
        post_patch.side_effect = [mock_search_result, {'_scroll_id': 'bones', 'hits': {'hits': []}}]
        del_patch.return_value = {}

        consumer = TestConsumer()
        generator = AssetSearchGenerator()
        generator.set_context(Context(None, {'uri': 'gs://zorroa-dev-data'}, {}))
        generator.generate(consumer)
        assert consumer.count > 0


mock_search_result = {
    'took': 4,
    'timed_out': False,
    '_scroll_id': 'bones',
    'hits': {
        'total': {'value': 2},
        'max_score': 0.2876821,
        'hits': [
            {
                '_index': 'litvqrkus86sna2w',
                '_type': 'asset',
                '_id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                '_score': 0.2876821,
                '_source': {
                    'source': {
                        'path': 'https://i.imgur.com/SSN26nN.jpg'
                    }
                }
            },
            {
                '_index': 'litvqrkus86sna2w',
                '_type': 'asset',
                '_id': 'aabbccddec48n1q1fginVMV5yllhRRGx2WKyKLjDphg',
                '_score': 0.2876821,
                '_source': {
                    'source': {
                        'path': 'https://i.imgur.com/foo.jpg'
                    }
                }
            }
        ]
    }
}
