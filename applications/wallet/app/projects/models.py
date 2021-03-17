import logging

from django.conf import settings
from django.db import models
from django_cryptography.fields import encrypt
from multiselectfield import MultiSelectField

from apikeys.utils import create_zmlp_api_key
from roles.utils import get_permissions_for_roles
from wallet.mixins import TimeStampMixin, UUIDMixin, ActiveMixin
from wallet.utils import get_zmlp_superuser_client, convert_base64_to_json
from boonsdk.client import BoonClient, BoonSdkNotFoundException

logger = logging.getLogger(__name__)

ROLES = [(role['name'], role['name'].replace('_', ' ')) for role in settings.ROLES]


class ActiveProjectManager(models.Manager):
    """Model manager that only returns projects that are active."""

    def get_queryset(self):
        return super(ActiveProjectManager, self).get_queryset().filter(isActive=True)


class Project(UUIDMixin, TimeStampMixin, ActiveMixin):
    """Represents a ZMLP project."""
    all_objects = models.Manager()
    objects = ActiveProjectManager()

    name = models.CharField(max_length=144, unique=True)
    users = models.ManyToManyField(settings.AUTH_USER_MODEL, through='projects.Membership',
                                   related_name='projects')
    organization = models.ForeignKey('organizations.Organization', on_delete=models.SET_NULL,
                                     null=True, blank=True, related_name='projects')
    apikey = encrypt(models.TextField(blank=True, editable=False))

    def __str__(self):
        return self.name

    def get_zmlp_super_client(self):
        """Returns a ZMLP client configured with a super user for this project.

        WARNING: Use with seldom and with great caution. This should only be used when
        absolutely necessary. Most ZMLP actions should occur using the the credentials
        of the logged in user.

        """
        return get_zmlp_superuser_client(project_id=str(self.id))

    def sync_with_zmlp(self):
        """Tries to create a project in ZMLP with the same name and ID. This syncs the projects
        between the Wallet DB and ZMLP and is a necessary step for any project to function
        correctly.

        """
        client = self.get_zmlp_super_client()

        # Get or create the ZMLP project.
        try:
            project = client.get('/api/v1/project')
        except BoonSdkNotFoundException:
            body = {'name': self.name, 'id': str(self.id)}
            project = client.post('/api/v1/projects', body)

        # Sync the project name.
        if self.name != project['name']:
            client.put('/api/v1/project/_rename', {'name': self.name})

        # Sync the project status.
        if self.isActive != project['enabled']:
            if self.isActive:
                project_status_response = client.put(f'/api/v1/projects/{self.id}/_enable', {})
            else:
                project_status_response = client.put(f'/api/v1/projects/{self.id}/_disable', {})
            if not project_status_response.get('success'):
                raise IOError(f'Unable to sync project {self.id} status.')

        # Create an apikey if one doesn't exist.
        if not self.apikey:
            name = f'wallet-project-key-{self.id}'
            permissions = get_permissions_for_roles([r['name'] for r in settings.ROLES])
            self.apikey = create_zmlp_api_key(client, name, permissions, internal=True)
            self.save()


class Membership(models.Model):
    """Associates a wallet User with a Project. Primarily used as a
    through model for connecting users and projects. Also stores the
    api key the user needs to access the ZMLP project.

    """
    user = models.ForeignKey(settings.AUTH_USER_MODEL, related_name='memberships',
                             on_delete=models.CASCADE)
    project = models.ForeignKey(Project, on_delete=models.CASCADE)
    apikey = encrypt(models.TextField(blank=True, editable=False))
    roles = MultiSelectField(choices=ROLES, blank=True)

    class Meta:
        unique_together = (
            ('user', 'project')
        )

    def __str__(self):
        return f'{self.project.name} - {self.user.username}'

    def get_client(self):
        """Returns a ZMLP Client using the apikey for this Membership."""
        return BoonClient(self.apikey, server=settings.BOONAI_API_URL)

    def sync_with_zmlp(self, client=None, force=False):
        """Syncs the permissions requested in Wallet with ZMLP by updating the api key.

        Args:
            client (BoonClient): Client used to communicate with ZMLP. If not set, it will
                create a superuser client with this memberships project set.
            force (bool): If set to True, will create a new apikey even if they currently
                match.

        """
        apikey_name = self._get_api_key_name()
        wallet_desired_permissions = get_permissions_for_roles(self.roles)
        if not client:
            client = self.project.get_zmlp_super_client()

        if not self.apikey:
            self.apikey = create_zmlp_api_key(client, apikey_name, wallet_desired_permissions,
                                              internal=True)
            self.save()
        else:
            # Check to make sure Wallet roles/permissions currently match
            apikey_json = convert_base64_to_json(self.apikey)
            try:
                apikey_id = apikey_json['id']
            except (TypeError, KeyError):
                # If there's no id, just recreate it.
                self.apikey = create_zmlp_api_key(client, apikey_name, wallet_desired_permissions,
                                                  internal=True)
                self.save()
                return
            else:
                apikey_permissions = apikey_json.get('permissions', [])
                internally_consistent = set(apikey_permissions) == set(wallet_desired_permissions)

            # Check to make sure the key still matches what's in ZMLP
            externally_consistent = True
            try:
                response = client.get(f'/auth/v1/apikey/{apikey_id}')
            except BoonSdkNotFoundException:
                logger.warning(f'The API Key {apikey_id} for user f{self.user.id} could not be '
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
                        f'Tried to delete API Key {apikey_id} for user f{self.user.id} '
                        f'while updating permissions. The API key could not be found.')
                self.apikey = new_apikey
                self.save()

    def destroy_zmlp_api_key(self, client):
        """Destroys the ZMLP API key associated with this membership.

        Args:
            client(BoonClient): Client used to communicate with ZMLP.

        """
        apikey_readable = True
        if not self.apikey:
            return
        try:
            key_data = convert_base64_to_json(self.apikey)
            apikey_id = key_data['id']
        except (ValueError, KeyError):
            logger.warning(
                f'Unable to decode apikey during delete for user {self.user.id}.')
            apikey_readable = False
        if apikey_readable:
            try:
                response = client.delete(f'/auth/v1/apikey/{apikey_id}')
            except BoonSdkNotFoundException:
                logger.warning(f'API key {apikey_id} could not be found when trying to delete it.')
                return
            if not response.status_code == 200:
                raise IOError('Error deleting apikey.')

    def delete_and_sync_with_zmlp(self, client):
        """Deletes the models and removes the associated API key from boonsdk.

        Args:
            client(BoonClient): Client used to communicate with ZMLP.

        """
        self.destroy_zmlp_api_key(client)
        self.delete()

    def _get_api_key_name(self):
        """Generate a unique name to use for the api key."""
        return f'{self.user.email}_{self.project_id}'
