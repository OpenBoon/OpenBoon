#!/usr/bin/env python

import zorroa.zsdk
from zplugins.mxnet.processors import ResNetSimilarityProcessor, ResNetClassifyProcessor
from zorroa.zsdk import Asset

from zorroa.zsdk.testing import PluginUnitTestCase, zorroa_test_data
from zplugins.util.proxy import add_proxy_file


class MxUnitTests(PluginUnitTestCase):
    @classmethod
    def setUpClass(cls):
        super(MxUnitTests, cls).setUpClass()
        cls.toucan_path = zorroa_test_data("images/set01/toucan.jpg")

    def setUp(self):
        self.frame = zorroa.zsdk.Frame(Asset(self.toucan_path))
        asset = self.frame.asset
        add_proxy_file(asset, self.toucan_path)
        add_proxy_file(asset, self.toucan_path)
        add_proxy_file(asset, self.toucan_path)

    def test_ResNetSimilarity_defaults(self):
        processor = self.init_processor(ResNetSimilarityProcessor(), {"debug": True})
        processor.process(self.frame)

        self.assertEquals(2053,
                          len(self.frame.asset.get_attr("analysis")["imageSimilarity"]["shash"]))

    def test_MxNetClassify_defaults(self):
        processor = self.init_processor(ResNetClassifyProcessor(), {"debug": True})
        processor.process(self.frame)

        self.assertTrue("albatross" in self.frame.asset.get_attr("analysis.imageClassify.keywords"))
