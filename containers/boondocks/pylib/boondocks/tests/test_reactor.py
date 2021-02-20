import unittest

from boondocks.reactor import Reactor
from boonflow.base import ExpandFrame, Frame
from boonflow.testing import TestEventEmitter, TestAsset
from boonsdk import FileImport


class ReactorTests(unittest.TestCase):

    def setUp(self):
        self.emitter = TestEventEmitter()
        self.reactor = Reactor(self.emitter)

    def test_reactor_expand_batch_pop(self):
        """Expand queue pops when full"""
        self.reactor.batch_size = 5

        asset = TestAsset(id='12345')
        frame = Frame(asset)
        for i in range(0, 5):
            self.reactor.add_expand_frame(frame, ExpandFrame(FileImport(frame.asset.uri)))

        assert len(self.emitter.get_events('expand')) == 1

    def test_reactor_expand_batch_expand_size_check(self):
        """Expand queue pops when full"""
        self.reactor.batch_size = 5

        asset = TestAsset(id='12345')
        frame = Frame(asset)
        for i in range(0, 5):
            self.reactor.add_expand_frame(frame, ExpandFrame(FileImport(frame.asset.uri)))
        expand = self.emitter.get_events('expand')[0]
        assert 5 == len(expand["payload"]["assets"])
