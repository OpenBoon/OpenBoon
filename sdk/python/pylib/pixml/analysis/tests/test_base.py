import unittest

from pixml.asset import FileImport
from pixml.analysis.testing import TestEventEmitter, TestAsset
from pixml.analysis.base import ExpandFrame, Frame, Reactor


class ReactorTests(unittest.TestCase):

    def setUp(self):
        self.emitter = TestEventEmitter()
        self.reactor = Reactor(self.emitter)

    def test_reactor_expand_batch_pop(self):
        """Expand queue pops when full"""
        self.reactor.batch_size = 5

        asset = TestAsset(id="12345")
        frame = Frame(asset)
        for i in range(0, 5):
            self.reactor.add_expand_frame(frame, ExpandFrame(FileImport(frame.asset.uri)))

        assert len(self.emitter.get_events('expand')) == 1

    def test_reactor_expand_copy_source_file(self):
        """Source file definitions get propagated to derived assets."""
        asset = TestAsset(id="12345")
        asset.set_attr("files", [{
            "category": "source"
        }])

        frame = Frame(asset)
        for i in range(0, 5):
            self.reactor.add_expand_frame(frame, ExpandFrame(FileImport(frame.asset.uri)))
        self.reactor.check_expand(5, force=True)

        expands = self.emitter.get_events('expand')[0]
        for expand in expands['payload']['assets']:
            assert len(expand['attrs']['files'])== 1
            assert expand['attrs']['files'][0]['assetId'] == '12345'
