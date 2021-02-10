import os
import pytest
from unittest.mock import patch

from zmlp_analysis.google.cloud_dlp import CloudDLPDetectEntities
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path
from google.oauth2 import service_account
import google.cloud.dlp


@pytest.mark.skip(reason='dont run automatically')
class CloudDLPDetectEntitiesTestCase(PluginUnitTestCase):

    @patch('zmlp_analysis.google.cloud_dlp.CloudDLPDetectEntities.get_proxy_image')
    @patch('zmlp_analysis.google.cloud_dlp.initialize_gcp_client')
    @patch('zmlp_analysis.google.cloud_dlp.get_gcp_project_id')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    @patch.object(file_storage.assets, 'store_blob')
    def test_dlp_processor(self, store_patch, native_patch,
                           localize_patch, pid_patch, init_patch, proxy_patch):

        CREDS = service_account.Credentials.from_service_account_file(os.path.dirname(__file__)
                                                                      + '/gcp-creds.json')
        path = zorroa_test_path('images/dlp/87497658.jpg')
        store_patch.return_value = None
        native_patch.return_value = path
        localize_patch.return_value = path
        pid_patch.return_value = 'irm-poc'
        init_patch.return_value = google.cloud.dlp_v2.DlpServiceClient(credentials=CREDS)
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudDLPDetectEntities())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-dlp-date')
        assert analysis['count'] == 4
        assert analysis['predictions'][0]['bbox'] == [0.112, 0.146, 0.22, 0.172]
