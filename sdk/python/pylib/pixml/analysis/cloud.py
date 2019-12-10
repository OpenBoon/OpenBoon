import json
import os

import minio
from google.auth.exceptions import DefaultCredentialsError
from google.cloud import storage as gcs
from google.oauth2 import service_account

from .base import AnalysisEnv
from ..app import app_from_env
from ..util import memoize


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


def get_google_storage_client():
    """
    Return a Google Storage Client instance.  This method attempts to return
    the appropriate client for the environment.

        1. Use PixelML DataSource creds if available.
        2. Use a storge client configured by the GOOGLE_APPLICATION_CREDENTIALS env var
        3. Fallback onto an anonymous client.

    Returns:
        Client: A Google Storage Client
    """
    datasource = AnalysisEnv.get_datasource_id()
    if datasource:
        app = app_from_env()
        creds = app.client.get('/api/v1/data-sources/{}/_credentials'.format(datasource))
        blob = json.loads(creds['blob'])
        gcp_creds = service_account.Credentials.from_service_account_info(blob)
        return gcs.Client(project=blob['project_id'], credentials=gcp_creds)
    else:
        try:
            return gcs.Client()
        except (DefaultCredentialsError, OSError):
            return gcs.Client.create_anonymous_client()


@memoize
def get_pixml_storage_client():
    """
    Return a PixelML storage client.  This client is used for accessing
    internal pixml:// URIs.  These URIs allow processors to store binary
    data that is later picked up by other processors or services.

    Returns:
        Minio: A Minio client.

    """
    return minio.Minio(os.environ.get("PIXML_STORAGE_HOST", "localhost:9000"),
                       access_key=os.environ.get("PIXML_STORAGE_ACCESS_KEY"),
                       secret_key=os.environ.get("PIXML_STORAGE_SECRET_KEY"))
