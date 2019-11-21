import base64
import json
import os

from .rest import PixmlClient


class PixmlApp(object):
    """

    """
    def __init__(self, apikey, server='https://api.pixelml.com'):
        """

        Args:
            apikey:
            server:
        """
        self.client = PixmlClient(server, apikey)

    def bulk_process_assets(self, assets):
        """
        Same scenario with local files, assets must be uploaded.

        Args:
            assets:

        Returns:

        """

        self.client.post("/api/v1/assets/_process", {
            "assets": [asset.for_json() for asset in assets]
        })

    def bulk_process_datasource(self, uri, analysis=None, media_types=None):
        """

        If URI is a local file path, the data has to be uploaded for processing.

        Args:
            uri:
            analysis:
            media_types:

        Returns:
            pass
        """
        pass

    def bulk_process_asset_search(self, uri, analysis=None, media_types=None):
        """

        If URI is a local file path, the data has to be uploaded for processing.

        Args:
            uri:
            analysis:
            media_types:

        Returns:
            pass
        """
        pass

    def search_assets(self, query):
        """

        Args:
            query:

        Returns:

        """
        pass

    def get_asset(self, id):
        """

        Args:
            id:

        Returns:

        """
        pass


def from_env():
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
        bytes = base64.b64decode(os.environ['Pixml_APIKEY'])
        apikey = json.loads(bytes.decode())
    elif 'PIXML_APIKEY_FILE' in os.environ:
        with open(os.environ['Pixml_APIKEY_FILE'], 'r') as fp:
            apikey = json.load(fp)

    server = os.environ.get('PIXML_SERVER', 'https://api.pixml.zorroa.com')
    return PixmlApp(apikey, server)


