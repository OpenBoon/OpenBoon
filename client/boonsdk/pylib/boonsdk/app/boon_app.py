import base64
import logging
import os
import json

from . import AssetApp, DataSourceApp, ProjectApp, \
    JobApp, ModelApp, AnalysisModuleApp, VideoClipApp, CustomFieldApp, \
    WebHookApp, DatasetApp, BoonLibApp
from ..client import BoonClient, DEFAULT_SERVER

logger = logging.getLogger(__name__)


class BoonApp:
    """
    The BoonApp class exposes the BoonAI API through various

    """
    def __init__(self, apikey, server=None):
        """
        Initialize a Boon AI Application instance.

        Args:
            apikey (mixed): An API key, can be either a key or file handle.
            server (str): The URL to the Boon AI API server, defaults cloud api.
        """
        logger.debug("Initializing Boon AI to {}".format(server))
        self.client = BoonClient(apikey, server or
                                 os.environ.get("BOONAI_SERVER", DEFAULT_SERVER))
        self.assets = AssetApp(self)
        """A ``boonsdk.app.AssetApp`` instance"""
        self.datasource = DataSourceApp(self)
        """A ``boonsdk.app.DataSourceApp`` instance"""
        self.projects = ProjectApp(self)
        """A ``boonsdk.app.ProjectApp`` instance"""
        self.jobs = JobApp(self)
        """A ``boonsdk.app.JobApp`` instance"""
        self.models = ModelApp(self)
        """A ``boonsdk.app.ModelApp`` instance"""
        self.analysis = AnalysisModuleApp(self)
        """A ``boonsdk.app.AnalysisModuleApp`` instance"""
        self.clips = VideoClipApp(self)
        """A ``boonsdk.app.VideoClipApp`` instance"""
        self.fields = CustomFieldApp(self)
        """A ``boonsdk.app.CustomFieldApp`` instance"""
        self.webhooks = WebHookApp(self)
        """A ``boonsdk.app.WebHookApp`` instance"""
        self.datasets = DatasetApp(self)
        """A ``boonsdk.app.DatasetApp`` instance"""
        self.boonlibs = BoonLibApp(self)
        """A ``boonsdk.app.DatasetApp`` instance"""


def app_from_env():
    """
    Create a BoonApp configured via environment variables. This method
    will not throw if the environment is configured improperly, however
    attempting the use the BoonApp instance to make a request
    will fail.

    - BOONAI_APIKEY : A base64 encoded API key.
    - BOONAIL_APIKEY_FILE : A path to a JSON formatted API key.
    - BOONAI_SERVER : The URL to the BOONAI API server.

    Returns:
        BoonApp: A configured BoonApp

    """
    apikey = None
    if 'BOONAI_APIKEY' in os.environ:
        apikey = os.environ['BOONAI_APIKEY']
    elif 'BOONAI_APIKEY_FILE' in os.environ:
        with open(os.environ['BOONAI_APIKEY_FILE'], 'rb') as fp:
            apikey = base64.b64encode(fp.read())
    return BoonApp(apikey, os.environ.get('BOONAI_SERVER'))


def app_from_file(path):
    """
    Create a BoonApp configured via the path to an API Key.

    Args:
        path (str): A file path to an API Key.

    Returns:
        BoonApp: A configured BoonApp.
    """
    with open(str(path), 'r') as fp:
        return BoonApp(json.load(fp))
