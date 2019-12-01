import json
import os
from unittest import TestCase
from unittest.mock import patch

from pixml.analysis.cloud import get_google_storage_client
from pixml.rest import PixmlClient


class TetCloudUtilFunction(TestCase):

    def test_get_google_storage_client_anon(self):
        """
        Enure that we fall back on an anonymous client.
        """
        client = get_google_storage_client()
        assert client._credentials.token is None

    def test_get_google_storage_client_env(self):
        """
        Ensure we load a service account file from the
        GOOGLE_APPLICATION_CREDENTIALS environment variable.
        """
        local_dir = os.path.dirname(__file__)
        path = os.path.join(local_dir, 'fake_gcs_account.json')
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = path
        try:
            client = get_google_storage_client()
            assert "fake_service_account@zorroa-deploy.iam.gserviceaccount.com" == \
                   client._credentials._service_account_email
        finally:
            del os.environ['GOOGLE_APPLICATION_CREDENTIALS']

    @patch.object(PixmlClient, 'get')
    def test_get_google_storage_client_datasource(self, get_patch):
        with open("fake_gcs_account.json", "r") as fp:
            gcs_creds = {'blob': fp.read()}

        get_patch.return_value = gcs_creds
        os.environ['PIXML_DATASOURCE_ID'] = "abc123"
        try:
            client = get_google_storage_client()
            assert "fake_service_account@zorroa-deploy.iam.gserviceaccount.com" == \
                   client._credentials._service_account_email
        finally:
            del os.environ['PIXML_DATASOURCE_ID']
