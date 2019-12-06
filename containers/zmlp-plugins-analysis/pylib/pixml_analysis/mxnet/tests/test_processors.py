from unittest.mock import patch

from ..processors import ResNetSimilarityProcessor, ResNetClassifyProcessor

from pixml import PixmlClient
from pixml.analysis import Frame
from pixml.analysis.testing import PluginUnitTestCase, zorroa_test_data, TestAsset
from pixml.analysis.storage import add_proxy_file


class MxUnitTests(PluginUnitTestCase):
    @classmethod
    def setUpClass(cls):
        super(MxUnitTests, cls).setUpClass()
        cls.toucan_path = zorroa_test_data('images/set01/toucan.jpg')

    def setUp(self):
        self.frame = Frame(TestAsset(self.toucan_path))

    @patch.object(PixmlClient, 'upload_file')
    def test_ResNetSimilarity_defaults(self, upload_patch):
        upload_patch.return_value = {
            'name': 'proxy_200x200.jpg',
            'category': 'proxy',
            'assetId': '12345',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 1023,
                'height': 1024
            }
        }
        add_proxy_file(self.frame.asset, self.toucan_path, (512, 512))
        processor = self.init_processor(ResNetSimilarityProcessor(), {'debug': True})
        processor.process(self.frame)

        self.assertEquals(2048, len(self.frame.asset['analysis.pixelml.similarity.vector']))

    @patch.object(PixmlClient, 'upload_file')
    def test_MxNetClassify_defaults(self, upload_patch):
        upload_patch.return_value = {
            'name': 'proxy_200x200.jpg',
            'category': 'proxy',
            'assetId': '12345',
            'mimetype': 'image/jpeg',
            'attrs': {
                'width': 1023,
                'height': 1024
            }
        }
        add_proxy_file(self.frame.asset, self.toucan_path, (512, 512))
        processor = self.init_processor(ResNetClassifyProcessor(), {'debug': True})
        processor.process(self.frame)

        self.assertTrue('albatross' in self.frame.asset['analysis.pixelml.labels.keywords'])
        self.assertTrue(self.frame.asset['analysis.pixelml.labels.score'] > 0)