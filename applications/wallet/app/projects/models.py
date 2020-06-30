import logging
import uuid

from django.conf import settings
from django.db import models
from django_cryptography.fields import encrypt
from multiselectfield import MultiSelectField
from zmlp.client import ZmlpDuplicateException, ZmlpClient, ZmlpNotFoundException

from apikeys.utils import create_zmlp_api_key
from projects.utils import random_project_name
from roles.utils import get_permissions_for_roles
from wallet.utils import get_zmlp_superuser_client, convert_base64_to_json

logger = logging.getLogger(__name__)

ROLES = [(role['name'], role['name'].replace('_', ' ')) for role in settings.ROLES]


class ActiveProjectManager(models.Manager):
    """Model manager that only returns projects that are active."""
    def get_queryset(self):
        return super(ActiveProjectManager, self).get_queryset().filter(is_active=True)


class Project(models.Model):
    """Represents a ZMLP project."""
    all_objects = models.Manager()
    objects = ActiveProjectManager()

    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    name = models.CharField(max_length=144, default=random_project_name)
    users = models.ManyToManyField(settings.AUTH_USER_MODEL, through='projects.Membership',
                                   related_name='projects')
    is_active = models.BooleanField(default=True)

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
        client = get_zmlp_superuser_client(self.id)
        body = {'name': self.name, 'id': str(self.id)}
        try:
            client.post('/api/v1/projects', body)
        except ZmlpDuplicateException:
            logger.info(f'Project {self.id} already exists in ZMLP')
        if hasattr(self, 'subscription'):
            client.put(f'/api/v1/projects/{self.id}/_update_tier',
                       {'tier': self.subscription.tier.upper()})


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
        return ZmlpClient(self.apikey, server=settings.ZMLP_API_URL)

    def sync_with_zmlp(self, client):
        """Syncs the permissions requested in Wallet with ZMLP by updating the api key.

        Args:
            client(ZmlpClient): Client used to communicate with ZMLP.

        """
        # TODO: Remove this logic when the Superuser does not use the inception key.
        if self.user.email == settings.SUPERUSER_EMAIL:
            return

        if not self.apikey:
            self.apikey = create_zmlp_api_key(client,
                                              self._get_api_key_name(),
                                              get_permissions_for_roles(self.roles),
                                              internal=True)
            self.save()
        else:
            apikey_json = convert_base64_to_json(self.apikey)
            apikey_id = apikey_json['id']
            zmlp_permissions = apikey_json.get('permissions')
            wallet_permissions = get_permissions_for_roles(self.roles)
            if zmlp_permissions != wallet_permissions:
                if apikey_json.get('name') == 'admin-key':
                    raise ValueError('Modifying the admin-key is not allowed.')
                new_apikey = create_zmlp_api_key(client, self._get_api_key_name(),
                                                 wallet_permissions, internal=True)
                try:
                    response = client.delete(f'/auth/v1/apikey/{apikey_id}')
                    if not response.status_code == 200:
                        raise IOError(f'There was an error deleting {apikey_id}')
                except ZmlpNotFoundException:
                    logger.warning(
                        f'Tried to delete API Key {apikey_id} for user f{self.user.id} '
                        f'while updating permissions. The API key could not be found.')
                self.apikey = new_apikey
                self.save()

    def destroy_zmlp_api_key(self, client):
        """Destroys the ZMLP API key associated with this membership.

        Args:
            client(ZmlpClient): Client used to communicate with ZMLP.

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
            except ZmlpNotFoundException:
                logger.warning(f'API key {apikey_id} could not be found when trying to delete it.')
                return
            if not response.status_code == 200:
                raise IOError('Error deleting apikey.')

    def delete_and_sync_with_zmlp(self, client):
        """Deletes the models and removes the associated API key from ZMLP.

        Args:
            client(ZmlpClient): Client used to communicate with ZMLP.

        """
        self.destroy_zmlp_api_key(client)
        self.delete()

    def _get_api_key_name(self):
        """Generate a unique name to use for the api key."""
        return f'{self.user.email}_{self.project_id}'
