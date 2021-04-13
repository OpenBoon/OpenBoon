import logging
import math
import os
from datetime import datetime

import requests
from boonsdk.client import BoonClient, BoonSdkNotFoundException, \
    BoonSdkConnectionException
from django.conf import settings
from django.db import models
from django_cryptography.fields import encrypt
from multiselectfield import MultiSelectField

from wallet.mixins import TimeStampMixin, UUIDMixin, ActiveMixin
from wallet.utils import get_zmlp_superuser_client, convert_base64_to_json, \
    sync_project_with_zmlp, sync_membership_with_zmlp

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

    name = models.CharField(max_length=144)
    users = models.ManyToManyField(settings.AUTH_USER_MODEL, through='projects.Membership',
                                   related_name='projects')
    organization = models.ForeignKey('organizations.Organization', on_delete=models.PROTECT,
                                     related_name='projects')
    apikey = encrypt(models.TextField(blank=True, editable=False, null=True))

    class Meta:
        unique_together = (('name', 'organization'))

    def __str__(self):
        return self.name

    def get_zmlp_super_client(self):
        """Returns a ZMLP client configured with a super user for this project.

        WARNING: Use with seldom and with great caution. This should only be used when
        absolutely necessary. Most ZMLP actions should occur using the the credentials
        of the logged in user.

        """
        return get_zmlp_superuser_client(project_id=str(self.id))

    def get_admin_client(self):
        """Returns a BoonClient with the admin api key associated with the project."""
        return BoonClient(self.apikey, server=settings.BOONAI_API_URL)

    def sync_with_zmlp(self):
        """Tries to create a project in ZMLP with the same name and ID. This syncs the projects
        between the Wallet DB and ZMLP and is a necessary step for any project to function
        correctly.

        """
        sync_project_with_zmlp(self)

    def ml_usage_this_month(self):
        """Returns the ml module usage for the current month."""
        today = datetime.today()
        first_of_the_month = f'{today.year:04d}-{today.month:02d}-01'
        path = os.path.join(settings.METRICS_API_URL, 'api/v1/apicalls/tiered_usage')
        response = requests.get(path, {'after': first_of_the_month, 'project': self.id})
        response.raise_for_status()
        return response.json()

    def total_storage_usage(self):
        """Returns the video and image/document usage of currently live assets."""
        path = 'api/v3/assets/_search'
        client = self.get_admin_client()
        usage = {}

        # Get Image/Document count
        query = {
            'track_total_hits': True,
            'query': {
                'bool': {
                    'filter': [
                        {'terms': {
                            'media.type': ['image', 'document']
                        }}
                    ]
                }
            }
        }
        try:
            response = client.post(path, query)
        except (requests.exceptions.ConnectionError, BoonSdkConnectionException):
            msg = (f'Unable to retrieve image/document count query for project {self.id}.')
            logger.warning(msg)
        else:
            usage['image_count'] = response['hits']['total']['value']

        # Get Aggregation for video minutes
        query = {
            'track_total_hits': True,
            'query': {
                'bool': {
                    'filter': [
                        {'terms': {
                            'media.type': ['video']
                        }}
                    ]
                }
            },
            'aggs': {
                'video_seconds': {
                    'sum': {
                        'field': 'media.length'
                    }
                }
            }
        }
        try:
            response = client.post(path, query)
        except (requests.exceptions.ConnectionError, BoonSdkConnectionException):
            msg = (f'Unable to retrieve video seconds sum for project {self.id}.')
            logger.warning(msg)
        else:
            video_seconds = response['aggregations']['sum#video_seconds']['value']
            usage['video_minutes'] = math.ceil(video_seconds / 60)
        return usage


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
        sync_membership_with_zmlp(self, client, force)

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
