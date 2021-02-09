from unittest.mock import patch

from zmlp_analysis.google.cloud_dlp import CloudDLPDetectEntities
from zmlp_analysis.google.cloud_vision import file_storage
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_mock_stored_file
from google.cloud.dlp_v2 import types

TOUCAN = zorroa_test_path("images/set01/toucan.jpg")


class MockDlpServiceClient:
    def __init__(self, *args, **kwargs):
        pass

    def inspect_content(self, request={}):
        bbox = types.BoundingBox(top=146, left=86, width=83, height=26)
        image_location = types.ImageLocation(bounding_boxes=[bbox, bbox])
        content_location = types.ContentLocation(image_location=image_location)
        location = types.Location(content_locations=[content_location])
        infotype = types.InfoType(name='DATE')
        finding = types.Finding(info_type=infotype, quote='June 28,1993',
                                location=location, likelihood=5)
        result = types.InspectResult(findings=[finding, finding, finding, finding, finding])

        response = types.InspectContentResponse(result=result)
        return response


class CloudDLPDetectEntitiesTests(PluginUnitTestCase):
    @patch.object(file_storage.assets, 'store_blob')
    @patch.object(file_storage.assets, 'store_file')
    @patch('zmlp_analysis.google.cloud_dlp.initialize_gcp_client',
           side_effect=MockDlpServiceClient)
    @patch('zmlp_analysis.google.cloud_dlp.get_gcp_project_id')
    @patch('zmlp_analysis.google.cloud_dlp.get_proxy_level_path')
    @patch.object(file_storage.assets, 'get_native_uri')
    @patch.object(file_storage, 'localize_file')
    def test_extract_entities(self, localize_patch, native_patch,
                              proxy_patch, pid_patch, _, store_patch, store_blob_patch):
        localize_patch.return_value = TOUCAN
        native_patch.return_value = TOUCAN
        proxy_patch.return_value = TOUCAN
        pid_patch.return_value = 'foo'
        store_patch.return_value = get_mock_stored_file()
        store_blob_patch.return_value = get_mock_stored_file()

        asset = TestAsset(TOUCAN)
        frame = Frame(asset)
        processor = self.init_processor(CloudDLPDetectEntities())

        # run processor with declared frame and assert asset attributes
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-dlp-date')

        assert analysis['count'] == 5
        assert analysis['predictions'][0]['bbox'] == [0.168, 0.428, 0.33, 0.504]
        assert analysis['predictions'][0]['label'] == '06/28/1993'

        name = 'BARBAZ Jr, Foo'
        assert processor.sanitize_entity('PERSON_NAME', name) == 'Foo Barbaz Jr'

        address = '666 Foobar Avenue, BAZ'
        assert processor.sanitize_entity('STREET_ADDRESS', address) == '666 Foobar Ave Baz'

        address = '666 Barbaz Road, P.o box 35, foobar, FO 53921'
        assert processor.sanitize_entity('STREET_ADDRESS', address) == \
               '666 Barbaz Rd PO Box 35 Foobar Fo 53921'

        date = 'December 30, 2012'
        assert processor.sanitize_entity('DATE', date) == '12/30/2012'
