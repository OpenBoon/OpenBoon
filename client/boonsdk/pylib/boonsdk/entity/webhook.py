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

    ASSET_ANALYZED = 0
    """Emitted the first time an asset is Analyzed"""

    ASSET_MODIFIED = 1
    """Emitted if an Asset is modified"""


class WebHook(BaseEntity):
    """
    WebHooks deliver data to your application as it happens in BoonAI.  You must host an
    HTTP endpoint on your end to utilize a web hook.
    """
    def __init__(self, data):
        super(WebHook, self).__init__(data)

    @property
    def url(self):
        return self._data['url']

    @property
    def secret_key(self):
        return self._data['secret_key']

    @property
    def active(self):
        return self._data['active']

    @property
    def triggers(self):
        return [WebHookTrigger(t) for t in self._data['triggers']]
