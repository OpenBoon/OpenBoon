import base64
import binascii
import json
import logging

from boonsdk.client import BoonSdkNotFoundException, BoonSdkRequestException
from django.conf import settings
from boonsdk import BoonClient

from wallet.exceptions import InvalidZmlpDataError


logger = logging.getLogger(__name__)


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
        return BoonClient(apikey=apikey, server=settings.BOONAI_API_URL,
                          project_id=str(project_id))
    else:
        return BoonClient(apikey=apikey, server=settings.BOONAI_API_URL)


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


def sync_project_with_zmlp(project, create=False):
    """Helper to sync a project with ZMLP. Necessary for data migrations, where
    model methods are not available. This should reflect the same code that exists in
    the Project.sync_with_zmlp method.

    Args:
        project (Project): Project to sync with ZMLP.
        create (Bool): If True and the project does not exist on ZMLP it will be created.

    """
    from apikeys.utils import create_zmlp_api_key
    from roles.utils import get_permissions_for_roles

    client = get_zmlp_superuser_client(project_id=str(project.id))

    # Get or create the ZMLP project.
    try:
        zmlp_project = client.get('/api/v1/project')
    except (BoonSdkNotFoundException, BoonSdkRequestException):
        if create:
            body = {'name': project.name, 'id': str(project.id)}
            zmlp_project = client.post('/api/v1/projects', body)
            project.apikey = None
        else:
            print(f'Tried to sync ZMLP project {project.name} ({project.id}) but it does not exist.')
            return

    # Sync the project name.
    if project.name != zmlp_project['name']:
        client.put('/api/v1/project/_rename', {'name': project.name})

    # Sync the project status.
    if project.isActive != zmlp_project['enabled']:
        if project.isActive:
            project_status_response = client.put(f'/api/v1/projects/{project.id}/_enable', {})
        else:
            project_status_response = client.put(f'/api/v1/projects/{project.id}/_disable', {})
        if not project_status_response.get('success'):
            raise IOError(f'Unable to sync project {project.id} status.')

    # Create an apikey if one doesn't exist.
    if hasattr(project, 'apikey') and not project.apikey:
        name = f'wallet-project-key-{project.id}'
        permissions = get_permissions_for_roles([r['name'] for r in settings.ROLES])
        project.apikey = create_zmlp_api_key(client, name, permissions, internal=True)
        project.save()


def sync_membership_with_zmlp(membership, client=None, force=False):
    """Helper to sync a membership with ZMLP. Necessary for data migrations, where
    model methods are not available. This should reflect the same code that exists in
    the Membership.sync_with_zmlp method."""
    from apikeys.utils import create_zmlp_api_key
    from roles.utils import get_permissions_for_roles

    apikey_name = f'{membership.user.email}_{membership.project_id}'
    wallet_desired_permissions = get_permissions_for_roles(membership.roles)
    if not client:
        client = get_zmlp_superuser_client(project_id=membership.project_id)

    if not membership.apikey:
        membership.apikey = create_zmlp_api_key(client, apikey_name,
                                                wallet_desired_permissions, internal=True)
        membership.save()
    else:
        # Check to make sure Wallet roles/permissions currently match
        apikey_json = convert_base64_to_json(membership.apikey)
        try:
            apikey_id = apikey_json['id']
        except (TypeError, KeyError):
            # If there's no id, just recreate it.
            membership.apikey = create_zmlp_api_key(client, apikey_name,
                                                    wallet_desired_permissions, internal=True)
            membership.save()
            return
        else:
            apikey_permissions = apikey_json.get('permissions', [])
            internally_consistent = set(apikey_permissions) == set(wallet_desired_permissions)

        # Check to make sure the key still matches what's in ZMLP
        externally_consistent = True
        try:
            response = client.get(f'/auth/v1/apikey/{apikey_id}')
        except BoonSdkNotFoundException:
            logger.warning(f'The API Key {apikey_id} for user f{membership.user.id} could not be '
                           f'found in ZMLP, it will be recreated.')
            externally_consistent = False
            zmlp_permissions = []
        else:
            zmlp_permissions = response.get('permissions', [])
        # Compare Wallet and ZMLP permissions
        if set(wallet_desired_permissions) != set(zmlp_permissions):
            externally_consistent = False

        # Recreate the key in ZMLP, delete the old one, and save the new one
        if not internally_consistent or not externally_consistent or force:
            if apikey_json.get('name') == 'admin-key':
                raise ValueError('Modifying the admin-key is not allowed.')
            new_apikey = create_zmlp_api_key(client, apikey_name, wallet_desired_permissions,
                                             internal=True)
            try:
                response = client.delete(f'/auth/v1/apikey/{apikey_id}')
                if not response.status_code == 200:
                    raise IOError(f'There was an error deleting {apikey_id}')
            except BoonSdkNotFoundException:
                logger.warning(
                    f'Tried to delete API Key {apikey_id} for user f{membership.user.id} '
                    f'while updating permissions. The API key could not be found.')
            membership.apikey = new_apikey
            membership.save()
