import logging

import boto3
import google.auth
from google.auth.exceptions import DefaultCredentialsError
from google.cloud import storage as gcs
from google.oauth2 import service_account
from azure.storage.blob import BlobServiceClient

from zmlp import app_from_env
from zmlp.client import ZmlpNotFoundException
from zmlp.util import memoize
from .base import ZmlpEnv

logger = logging.getLogger(__name__)


def get_credentials_blob(creds_type):
    """
    Get the given credentials blob associated with the current job.

    Args:
        creds_type (str): A type of credentials blob. (GCP, AWS, CLARIFAI,etc)

    Returns:
        dict: A credentials blob.
    """
    creds = ZmlpEnv.get_available_credentials_types()
    job_id = ZmlpEnv.get_job_id()

    if creds_type in creds and job_id:
        app = app_from_env()
        try:
            return app.client.get(
                '/api/v1/jobs/{}/_credentials/{}'.format(job_id, creds_type))
        except ZmlpNotFoundException:
            pass

    logger.warning("Unable to find '{}' credentials for this job.".format(creds_type))
    return None


@memoize
def get_cached_google_storage_client():
    """
    The result of this function is cached forever due to the fact hat
    there is only going to be one storage Client ever returned
    in the container environment.  This ensures that the method is
    not constantly pulling credentials and building storage new clients.

    Returns:
        Client: A Google Storage Client
    """
    return get_google_storage_client()


@memoize
def get_cached_aws_client(service):
    """
    Return a cached Amazon Boto3 client for the given service.  Once the first client
    of a given type is created, that instance is returned on subsequent calls. This
    avoids the connection and authentication overhead.

    Args:
        service (str): The AWS service identifier, 's3', 'rekognition', etc.

    Returns:
        mixed: The appropriate client instance for the service.

    """
    return get_aws_client(service)


def get_aws_client(service):
    """
    Return a Boto3 client for the given service.

    Args:
        service (str): The AWS service identifier, 's3', 'rekognition', etc.

    Returns:
        mixed: The appropriate client instance for the service.
    """
    creds = get_credentials_blob('AWS')
    if creds:
        return boto3.client(service,
                            aws_access_key_id=creds['aws_access_key_id'],
                            aws_secret_access_key=creds['aws_secret_access_key'])
    else:
        return boto3.client('s3')


def get_google_storage_client():
    """
    Return a Google Storage Client instance.  This method attempts to return
    the appropriate client for the environment.

        1. Use ZMLP DataSource creds if available.
        2. Use a storge client configured by the GOOGLE_APPLICATION_CREDENTIALS env var
        3. Fallback onto an anonymous client.

    Returns:
        Client: A Google Storage Client

    """

    creds = get_credentials_blob("GCP")
    if creds:
        gcp_creds = service_account.Credentials.from_service_account_info(creds)
        return gcs.Client(project=creds['project_id'], credentials=gcp_creds)
    else:
        logger.info("No GCP credentials specified, using defaults")
        try:
            return gcs.Client()
        except (DefaultCredentialsError, OSError):
            return gcs.Client.create_anonymous_client()


def get_azure_storage_client():
    creds = get_credentials_blob('AZURE')
    if creds:
        return BlobServiceClient.from_connection_string(creds['connection_string'])
    else:
        return None


@memoize
def get_cached_azure_storage_client():
    return get_azure_storage_client()


def get_gcp_project_id():
    """
    Return the current authenticated GCP project Id.

    Returns:
        str: The project Id.
    """
    _, pid = google.auth.default()
    if not pid:
        raise RuntimeError("No google project is configured")
    return pid
