import base64
import logging
import os

from . import AssetApp, DataSourceApp, ProjectApp, \
    JobApp, ModelApp, AnalysisModuleApp, VideoClipApp, CustomFieldApp
from ..client import BoonClient, DEFAULT_SERVER

logger = logging.getLogger(__name__)


class BoonApp(object):
    """
    Exposes the main Boon AI API.

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
        self.datasource = DataSourceApp(self)
        self.projects = ProjectApp(self)
        self.jobs = JobApp(self)
        self.models = ModelApp(self)
        self.analysis = AnalysisModuleApp(self)
        self.clips = VideoClipApp(self)
        self.fields = CustomFieldApp(self)


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
        BoonClient : A configured BoonClient

    """
    apikey = None
    if 'BOONAI_APIKEY' in os.environ:
        apikey = os.environ['BOONAI_APIKEY']
    elif 'BOONAI_APIKEY_FILE' in os.environ:
        with open(os.environ['BOONAI_APIKEY_FILE'], 'rb') as fp:
            apikey = base64.b64encode(fp.read())
    return BoonApp(apikey, os.environ.get('BOONAI_SERVER'))
