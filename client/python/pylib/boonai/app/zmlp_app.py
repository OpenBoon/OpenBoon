import base64
import logging
import os

from . import AssetApp, DataSourceApp, ProjectApp, \
    JobApp, ModelApp, AnalysisModuleApp, VideoClipApp, CustomFieldApp
from ..client import ZmlpClient, DEFAULT_SERVER

logger = logging.getLogger(__name__)


class ZmlpApp(object):
    """
    Exposes the main ZMLP API.

    """
    def __init__(self, apikey, server=None):
        """
        Initialize a ZMLP Application instance.

        Args:
            apikey (mixed): An API key, can be either a key or file handle.
            server (str): The URL to the ZMLP API server, defaults cloud api.
        """
        logger.debug("Initializing ZMLP to {}".format(server))
        self.client = ZmlpClient(apikey, server or
                                 os.environ.get("ZMLP_SERVER", DEFAULT_SERVER))
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
    Create a ZmlpApp configured via environment variables. This method
    will not throw if the environment is configured improperly, however
    attempting the use the ZmlpApp instance to make a request
    will fail.

    - ZMLP_APIKEY : A base64 encoded API key.
    - ZMLPL_APIKEY_FILE : A path to a JSON formatted API key.
    - ZMLP_SERVER : The URL to the ZMLP API server.

    Returns:
        ZmlpClient : A configured ZmlpClient

    """
    apikey = None
    if 'ZMLP_APIKEY' in os.environ:
        apikey = os.environ['ZMLP_APIKEY']
    elif 'ZMLP_APIKEY_FILE' in os.environ:
        with open(os.environ['ZMLP_APIKEY_FILE'], 'rb') as fp:
            apikey = base64.b64encode(fp.read())
    return ZmlpApp(apikey, os.environ.get('ZMLP_SERVER'))
