import base64
import binascii
import json


def decode_apikey(apikey):
    key_data = None
    if not apikey:
        return key_data
    elif hasattr(apikey, 'read'):
        key_data = json.load(apikey)
    elif isinstance(apikey, dict):
        key_data = apikey
    elif isinstance(apikey, (str, bytes)):
        try:
            key_data = json.loads(base64.b64decode(apikey))
        except (binascii.Error, json.decoder.JSONDecodeError):
            raise ValueError("Invalid base64 encoded API key.")

    return key_data
