import base64
import logging
import os

from .asset import AssetApp
from .rest import PixmlClient

logger = logging.getLogger(__name__)

DEFAULT_SERVER = 'https://api.pixelml.com'


class PixmlApp(object):
    """
    Exposes the main PixelML API.

    """
    def __init__(self, apikey, server=None):
        """
        Initialize a PixelML Application instance.

        Args:
            apikey (mixed): An API key, can be either a key or file handle.
            server (str): The URL to the PixelML API server, defaults cloud api.
        """
        logger.debug("Initializing PixmlApp to {}".format(server))
        self.client = PixmlClient(apikey, server or
                                  os.environ.get("PIXML_SERVER", DEFAULT_SERVER))
        self.assets = AssetApp(self)


def app_from_env():
    """
    Create a PixmlApp configured via environment variables. This method
    will not throw if the environment is configured improperly, however
    attempting the use the PixmlApp instance to make a request
    will fail.

    - PIXML_APIKEY : A base64 encoded API key.
    - PIXML_APIKEY_FILE : A path to a JSON formatted API key.
    - PIXML_SERVER : The URL to the Pixml API server.

    Returns:
        PixmlClient : A configured PixmlClient

    """
    apikey = None
    if 'PIXML_APIKEY' in os.environ:
        apikey = os.environ['PIXML_APIKEY']
    elif 'PIXML_APIKEY_FILE' in os.environ:
        with open(os.environ['PIXML_APIKEY_FILE'], 'rb') as fp:
            apikey = base64.b64encode(fp.read())
    return PixmlApp(apikey, os.environ.get('PIXML_SERVER'))


