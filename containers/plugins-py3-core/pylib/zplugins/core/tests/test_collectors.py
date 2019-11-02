#!/usr/bin/python
from unittest.mock import patch

from zplugins.core.collectors import ImportCollector
from zorroa.zsdk import Document, Frame
from zorroa.zsdk.testing import PluginUnitTestCase


class Consumer:
    def __init__(self):
        self.count = 0

    def accept(self, frame):
        self.count += 1


class ImportCollectorUnitTestCase(PluginUnitTestCase):

    @patch('zplugins.core.collectors.Client.post')
    def testCollect(self, post_patch):
        post_patch.return_value = {}

        frames = [
            Frame(Document({'id': '1', 'document':
                {'foo': 'bar'}, 'permissions': {'zorroa::foo': 1}}))
        ]

        collector = self.init_processor(ImportCollector())
        collector.collect(frames)

        asset = post_patch.call_args_list[0][0][1]['sources'][0]
        assert asset['links'] is None
        assert asset['replace'] is False
        assert asset['id'] == "1"
        assert asset['document'] == {'foo': 'bar'}
        assert asset['permissions'] == {'zorroa::foo': 1}
