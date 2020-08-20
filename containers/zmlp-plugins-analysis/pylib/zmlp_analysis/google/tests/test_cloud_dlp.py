from unittest.mock import patch

from google.cloud.dlp_v2 import types

from zmlp_analysis.google.cloud_dlp import CloudDLPDetectEntities
from zmlp_analysis.google.cloud_vision import file_storage
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path

TOUCAN = zorroa_test_path("images/set01/toucan.jpg")


class MockDlpServiceClient:
    def __init__(self, *args, **kwargs):
        pass

    def inspect_content(self, _, __, ___):
        bbox1 = types.BoundingBox(top=10, left=10, width=50, height=90)
        bbox2 = types.BoundingBox(top=13, left=10, width=40, height=92)

        content_locations = \
            [types.ContentLocation(
                image_location=types.ImageLocation(
                    bounding_boxes=[bbox1, bbox2]))]
        location = types.Location(content_locations=content_locations)
        f1 = types.Finding(quote='May 30, 2019',
                           info_type=types.InfoType(name='DATE'),
                           likelihood='LIKELY',
                           location=location)

        result = types.InspectContentResponse(result=types.InspectResult(findings=[f1]))

        return result


class CloudDLPDetectEntitiesTests(PluginUnitTestCase):
    @patch('zmlp_analysis.google.cloud_dlp.initialize_gcp_client',
           side_effect=MockDlpServiceClient)
    @patch('zmlp_analysis.google.cloud_dlp.get_gcp_project_id')
    @patch('zmlp_analysis.google.cloud_dlp.get_proxy_level_path')
    @patch.object(file_storage.assets, 'get_native_uri')
    @patch.object(file_storage, 'localize_file')
    def test_extract_entities(self, localize_patch, native_patch,
                              proxy_patch, pid_patch, _):
        localize_patch.return_value = TOUCAN
        native_patch.return_value = TOUCAN
        proxy_patch.return_value = TOUCAN
        pid_patch.return_value = 'foo'

        asset = TestAsset(TOUCAN)
        frame = Frame(asset)
        processor = self.init_processor(CloudDLPDetectEntities())

        # run processor with declared frame and assert asset attributes
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-dlp-date')
        print(analysis)
        assert analysis['count'] == 1
        assert analysis['predictions'][0]['bbox'] == [0.02, 0.029, 0.117, 0.308]
