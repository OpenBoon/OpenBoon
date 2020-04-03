from unittest.mock import patch

from zmlp import ZmlpClient
from zmlpsdk import Frame
from zmlpsdk.proxy import store_asset_proxy
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_data, TestAsset, \
    get_prediction_labels, get_mock_stored_file
from ..processors import ZviLabelDetectionResNet152, ZviSimilarityProcessor


class MxUnitTests(PluginUnitTestCase):
    @classmethod
    def setUpClass(cls):
        super(MxUnitTests, cls).setUpClass()
        cls.toucan_path = zorroa_test_data('images/set01/toucan.jpg')

    def setUp(self):
        self.frame = Frame(TestAsset(self.toucan_path))

    @patch.object(ZmlpClient, 'upload_file')
    def test_ResNetSimilarity_defaults(self, upload_patch):
        upload_patch.return_value = get_mock_stored_file()._data

        store_asset_proxy(self.frame.asset, self.toucan_path, (512, 512))
        processor = self.init_processor(ZviSimilarityProcessor(), {'debug': True})
        processor.process(self.frame)

        self.assertEquals(2048, len(self.frame.asset['analysis.zvi-image-similarity.simhash']))

    @patch.object(ZmlpClient, 'upload_file')
    def test_MxNetClassify_defaults(self, upload_patch):
        upload_patch.return_value = get_mock_stored_file()._data
        store_asset_proxy(self.frame.asset, self.toucan_path, (512, 512))
        processor = self.init_processor(ZviLabelDetectionResNet152(), {'debug': True})
        processor.process(self.frame)

        analysis = self.frame.asset['analysis.zvi-label-detection']
        print(analysis)
        assert 'toucan' in get_prediction_labels(analysis)

