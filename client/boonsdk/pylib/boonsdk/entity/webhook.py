from enum import Enum

import jwt

from .base import BaseEntity


def validate_webhook_request_headers(headers, secret_key):
    """
    A utility function to validate signed HTTP request headers
    for a webhook request.  You must know your secret key to
    validate the headers.  If the header are valid you know
    the request came from Boon AI.

    Args:
        headers (dict):
        secret_key (str):

    Returns:
        dict: A dictionary of attributes that describe the request.
    """
    token = headers.get('X-BoonAI-Signature-256', '')
    return jwt.decode(token, secret_key, algorithms=['HS256'])


class WebHookTrigger(Enum):
    """
    Different types of WebHook Triggers.
    """

    AssetAnalyzed = 0
    """Emitted the first time an asset is Analyzed"""

    AssetModified = 1
    """Emitted if an Asset is reanalyzed or manually edited via a custom field."""


class WebHook(BaseEntity):
    """
    Properties of a WebHook
    """
    def __init__(self, data):
        super(WebHook, self).__init__(data)

    @property
    def url(self):
        """The URL where the WebHook is hosted."""
        return self._data['url']

    @property
    def secret_key(self):
        """A secret key the endpoint will use to validate requests."""
        return self._data['secret_key']

    @property
    def active(self):
        """True if the WebHook is active."""
        return self._data['active']

    @property
    def triggers(self):
        """A list of WebHookTriggers the hook will fire on."""
        return [WebHookTrigger(t) for t in self._data['triggers']]
