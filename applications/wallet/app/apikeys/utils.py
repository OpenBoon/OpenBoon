import uuid

from wallet.utils import convert_json_to_base64


def create_zmlp_api_key(client, name, permissions, encode_b64=True, internal=False):
    """Creates an API key for ZMLP and returns it as a base64 encoded string.

    Args:
        client(BoonClient): BoonClient used to communicate with ZMLP.
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
    body = {'name': name, 'permissions': permissions, 'hidden': internal}
    apikey = client.post('/auth/v1/apikey', body)
    apikey.update(client.get(f'/auth/v1/apikey/{apikey["id"]}/_download'))
    if encode_b64:
        apikey = convert_json_to_base64(apikey).decode('utf-8')
    return apikey
