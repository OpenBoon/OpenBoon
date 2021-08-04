import logging
import os

from flask import request

from boonsdk import BoonClient, BoonApp

logger = logging.getLogger(__name__)

__all__ = [
    'app_instance'
]


def app_instance():
    """
    Create a return a pre-authenticated BoonApp instance for use
    within a Flasks BoonFunction environment.

    Returns:
        BoonApp: The configured BoonApp
    """
    app = BoonApp(apikey=None)
    app.client = BoonFunctionClient()
    return app


class BoonFunctionClient(BoonClient):
    """
    A BoonAI client that automatically uses an Authorization header
    from an exisgting
    """

    def __init__(self):
        super(BoonFunctionClient, self).__init__(None, None)

    def get_server(self):
        return os.environ.get("BOONAI_SERVER")

    def sign_request(self):
        return request.headers.get("Authorization")
