from pixml import app_from_env

from google.cloud import storage as gcs
from google.oauth2 import service_account

from .base import AnalysisEnv


def get_google_storage_client():
    """
    Return a Google Credentials object.  This method can only be run fr

    Returns:
        Credentials: A google credentials object.
    """
    dataset = AnalysisEnv.get_dataset_id()
    if dataset:
        app = app_from_env()
        creds = app.client.get(
            "/api/v1/datasets/{}/_credentials".format(dataset))
        gcp_creds = service_account.Credentials.from_service_account_info(creds)
        return gcs.Client(project=creds["project_id"], credentials=gcp_creds)
    else:
        try:
            return gcs.Client()
        except OSError:
            return gcs.Client.create_anonymous_client()
