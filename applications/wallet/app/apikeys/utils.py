import base64
import binascii
import json
import uuid


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


def create_zmlp_api_key(client, name, permissions, encode_b64=True, internal=False):
    """Creates an API key for ZMLP and returns it as a base64 encoded string.

    Args:
        client(ZmlpClient): ZmlpClient used to communicate with ZMLP.
        name(str): Name of the API key to create.
        permissions(list<str>): List of permissions to add to the API key.
        encode_b64(bool): If True the key is returned as a base64 encoded string.
        internal(bool): If True the key is given a special prefix which will cause it to
         be filtered from the results when listing API Keys in the UI.

    Returns:
        str: Base64 encoded API key that was created.

    """
    if internal:
        name = f'Admin Console Generated Key - {uuid.uuid4()} - {name}'
    body = {'name': name, 'permissions': permissions}
    apikey = client.post('/auth/v1/apikey', body)
    apikey.update(client.get(f'/auth/v1/apikey/{apikey["id"]}/_download'))
    if encode_b64:
        apikey = encode_apikey(apikey).decode('utf-8')
    return apikey
