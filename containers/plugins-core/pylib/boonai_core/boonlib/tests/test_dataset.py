from unittest.mock import patch

from boonai_core.boonlib.dataset import ExportDatasetProcessor
from boonflow import Frame
from boonflow.testing import PluginUnitTestCase, TestAsset, test_data


class TestBoonLibDataSet(PluginUnitTestCase):
    TOUCAN = test_data("images/set01/toucan.jpg")

    @patch.object(ExportDatasetProcessor, 'upload_asset')
    @patch.object(ExportDatasetProcessor, 'copy_files_to_lib')
    def test_assert_process(self, copy_patch, upload_patch):
        frame = Frame(TestAsset(self.TOUCAN, id="abcd123"))

        frame.asset.set_attr("labels", [
            {
                'datasetId': 'abcd123',
                'label': 'bird'
            }
        ])

        frame.asset.set_attr('files', [
            {
                'id': 'foo/bar/bing/proxy.jpg',
                'category': 'proxy',
                'name': 'proxy.jpg'
            },
            {
                'id': 'foo/bar/bong/web-proxy.jpg',
                'category': 'web-proxy',
                'name': 'web-proxy.jpg'
            },
        ]
                             )

        args = {
            'dataset_id': 'abcd123',
            'boonlib_id': 'zaq931'
        }
        ih = self.init_processor(ExportDatasetProcessor(), args=args)
        ih.process(frame)

        copy_map = copy_patch.call_args[0][0]
        assert len(copy_map) == 2
        assert copy_map['foo/bar/bing/proxy.jpg'] == 'boonlib/zaq931/abcd123/proxy.jpg'
        assert copy_map['foo/bar/bong/web-proxy.jpg'] == 'boonlib/zaq931/abcd123/web-proxy.jpg'

        assert upload_patch.call_args[0][0].id == frame.asset.id
