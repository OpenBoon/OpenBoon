import base64
import binascii
import json

from django.conf import settings
from boonsdk import BoonClient

from wallet.exceptions import InvalidZmlpDataError


def validate_zmlp_data(serializer):
    """Returns a Response object to be used when data returned by ZMLP is invalid."""
    if not serializer.is_valid():
        raise InvalidZmlpDataError(detail=serializer.errors)


def get_zmlp_superuser_client(project_id=None):
    """
    Helper method to return the ZMLP client specifically for the SuperUser, who is
    the only person who can create projects.

    Args:
        user: User to try getting a superuser level client for.

    Returns:
        Initialized ZMLP client

    """
    apikey = settings.INCEPTION_KEY_B64
    if project_id:
        return BoonClient(apikey=apikey, server=settings.ZMLP_API_URL,
                          project_id=str(project_id))
    else:
        return BoonClient(apikey=apikey, server=settings.ZMLP_API_URL)


def convert_base64_to_json(encoded_blob):
    """Converts a base64 json string into it's dict representation.

    Will return the decoded json if it's already decoded, a JSON file, a dict,
    or a Base64 encoded JSON string.

    Args:
        encoded_blob: JSON string to decode.

    Returns: (dict) The JSON/dict representation of the JSON data

    Raises: (ValueError) When the JSON string is undecodable

    """
    decoded_obj = None
    if not encoded_blob:
        return decoded_obj
    elif hasattr(encoded_blob, 'read'):
        decoded_obj = json.load(encoded_blob)
    elif isinstance(encoded_blob, dict):
        decoded_obj = encoded_blob
    elif isinstance(encoded_blob, (str, bytes)):
        try:
            decoded_obj = json.loads(base64.b64decode(encoded_blob))
        except (binascii.Error, json.decoder.JSONDecodeError, UnicodeDecodeError):
            raise ValueError('Invalid base64 encoded JSON.')

    return decoded_obj


def convert_json_to_base64(json_obj):
    """Converts JSON into it's Base64 representation.

    Args: (str) JSON to encode.

    Returns: (str) The Base64 encoded representation of the JSON data

    """
    encoded_str = None
    if not json_obj:
        return encoded_str
    elif isinstance(json_obj, (dict, list)):
        try:
            encoded_str = base64.b64encode(json.dumps(json_obj).encode('utf-8'))
        except binascii.Error:
            raise ValueError('Error encoding to JSON.')

    return encoded_str
