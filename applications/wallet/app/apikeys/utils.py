import base64
import binascii
import json


def decode_apikey(apikey):
    """Decodes an apikey into it's JSON representation.

    Will return the decoded apikey if it's already decodewd, a JSON file, a dict,
    or a Base64 encoded JSON string.

    Args:
        apikey: Apikey to decode.

    Returns: (dict) The JSON/dict representation of the apikey data

    Raises: (ValueError) When the apikey is undecodable

    """
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
            raise ValueError('Invalid base64 encoded API key.')

    return key_data


def encode_apikey(apikey):
    """Encodes an apikey into it's Base64 representation.

    Args: (str) Apikey to encode.

    Returns: (str) The Base64 encoded representation of the apikey data

    """
    encoded_key = None
    if not apikey:
        return encoded_key
    elif isinstance(apikey, dict):
        try:
            encoded_key = base64.b64encode(json.dumps(apikey).encode('utf-8'))
        except binascii.Error:
            raise ValueError('Error encoding apikey.')

    return encoded_key
