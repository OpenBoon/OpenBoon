import minio
import os
import logging
import json
from unittest import TestCase
from unittest.mock import patch

from zmlpsdk.cloud import get_google_storage_client, get_pipeline_storage_client
from zmlp.client import ZmlpClient

logging.basicConfig(level=logging.DEBUG)


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

    @patch.object(ZmlpClient, 'get')
    def test_get_google_storage_client_from_job_creds(self, get_patch):
        with open(os.path.dirname(__file__) + '/fake_gcs_account.json', 'r') as fp:
            gcs_creds = fp.read()

        get_patch.return_value = json.loads(gcs_creds)
        os.environ['ZMLP_JOB_ID'] = 'abc123'
        os.environ['ZMLP_CREDENTIALS_TYPES'] = 'GCP'
        try:
            client = get_google_storage_client()
            assert 'fake_service_account@zorroa-deploy.iam.gserviceaccount.com' == \
                   client._credentials._service_account_email
        finally:
            del os.environ['ZMLP_JOB_ID']
            del os.environ['ZMLP_CREDENTIALS_TYPES']

    def test_get_zmlp_storage_client(self):
        os.environ['ZMLP_STORAGE_PIPELINE_URL'] = "http://localhost:9000"
        try:
            client = get_pipeline_storage_client()
            assert type(client) == minio.api.Minio
        finally:
            del os.environ['ZMLP_STORAGE_PIPELINE_URL']
