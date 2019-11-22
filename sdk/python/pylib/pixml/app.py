import base64
import json
import logging
import os
from functools import lru_cache

from pixml.storage import LocalFileCache
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
        self.client = PixmlClient(apikey, server or DEFAULT_SERVER)
        self.lfc = LocalFileCache(self)

    def bulk_process_assets(self, assets):
        """

        Args:
            assets:

        Returns:

        """
        raise NotImplemented()

    def bulk_process_datasource(self, uri):
        """

        If URI is a local file path, the data has to be uploaded for processing.

        Returns:

        """
        raise NotImplemented()

    def bulk_process_asset_search(self, query):
        """
        If URI is a local file path, the data has to be uploaded for processing.

        Returns:
        """
        raise NotImplemented()

    def asset_search(self, query):
        """
        Perform an asset search.

        Args:
            query:

        Returns:

        """
        raise NotImplemented()

    def get_asset(self, id):
        """

        Args:
            id:

        Returns:

        """
        raise NotImplemented()

    def localize_remote_file(self, obj):
        """
        Localize a remote file.

        Args:
            obj(mixed): The uri, asset, or file storage definition to localize.

        Returns:
            str: a local file path to a remote file

        """
        return self.lfc.localize_remote_file(obj)


@lru_cache(maxsize=32)
def get_app_cached(apikey, server):
    """
    Return a possibly cached PixmlApp instance.  The caching system
    allows app_from_env() to be called from different parts of an
    application which share the same local file cache.

    Args:
        apikey (str): The api key
        server (str): The server url.

    Returns:
        PixmlApp: A PixmlApp instance.
    """
    return PixmlApp(apikey, server)


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
        with open(os.environ['PIXML_APIKEY_FILE'], 'r') as fp:
            apikey = json.load(fp)
    return get_app_cached(apikey, os.environ.get('PIXML_SERVER'))
