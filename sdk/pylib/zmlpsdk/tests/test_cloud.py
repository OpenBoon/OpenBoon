import os
import logging
import json
from unittest import TestCase
from unittest.mock import patch

from zmlpsdk.cloud import get_google_storage_client, \
    get_aws_client, get_credentials_blob, get_azure_storage_client
from zmlp.client import ZmlpClient
from zmlpsdk.testing import zorroa_test_path

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
        path = zorroa_test_path('creds/gcp_test.json')
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = path
        try:
            client = get_google_storage_client()
            assert 'zorroa-processors@zgcp-1.iam.gserviceaccount.com' == \
                   client._credentials._service_account_email
        finally:
            del os.environ['GOOGLE_APPLICATION_CREDENTIALS']

    @patch.object(ZmlpClient, 'get')
    def test_get_google_storage_client_from_job_creds(self, get_patch):
        with open(zorroa_test_path('creds/gcp_test.json'), 'r') as fp:
            gcs_creds = fp.read()

        get_patch.return_value = json.loads(gcs_creds)
        os.environ['ZMLP_JOB_ID'] = 'abc123'
        os.environ['ZMLP_CREDENTIALS_TYPES'] = 'GCP'
        try:
            client = get_google_storage_client()
            assert client._credentials._service_account_email == \
                   'zorroa-processors@zgcp-1.iam.gserviceaccount.com'

        finally:
            del os.environ['ZMLP_JOB_ID']
            del os.environ['ZMLP_CREDENTIALS_TYPES']

    @patch.object(ZmlpClient, 'get')
    def test_get_aws_client(self, get_patch):
        get_patch.return_value = {
            'aws_access_key_id': 'boom',
            'aws_secret_access_key': 'chakalakalaka'
        }
        client = get_aws_client('s3')
        assert getattr(client, 'create_bucket')
        assert getattr(client, 'get_object')

    @patch.object(ZmlpClient, 'get')
    def test_get_azure_client(self, get_patch):
        get_patch.return_value = {
            'connection_string': 'DefaultEndpointsProtocol=https;AccountName=zorroatest;'
                                 'AccountKey=RANDOMDUDE/TOMBRADY==;EndpointSuffix=core.windows.net'
        }
        os.environ['ZMLP_JOB_ID'] = 'abc123'
        os.environ['ZMLP_CREDENTIALS_TYPES'] = 'AZURE'
        try:
            client = get_azure_storage_client()
            assert getattr(client, 'list_containers')
            assert getattr(client, 'get_container_client')
        finally:
            del os.environ['ZMLP_JOB_ID']
            del os.environ['ZMLP_CREDENTIALS_TYPES']

    @patch.object(ZmlpClient, 'get')
    def test_get_credentials_blob(self, get_patch):
        os.environ['ZMLP_JOB_ID'] = 'abc123'
        os.environ['ZMLP_CREDENTIALS_TYPES'] = 'AWS'
        try:
            get_patch.return_value = {
                'aws_access_key_id': 'boom',
                'aws_secret_access_key': 'chakalakalaka'
            }
            blob = get_credentials_blob("AWS")
            assert blob['aws_access_key_id'] == 'boom'
            assert blob['aws_secret_access_key'] == 'chakalakalaka'
        finally:
            del os.environ['ZMLP_JOB_ID']
            del os.environ['ZMLP_CREDENTIALS_TYPES']
