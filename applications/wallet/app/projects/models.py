import logging
import uuid

import requests
from django.conf import settings
from django.db import models
from django_cryptography.fields import encrypt
from zmlp.client import ZmlpDuplicateException

from wallet.utils import get_zmlp_superuser_client

logger = logging.getLogger(__name__)


class Project(models.Model):
    """Represents a ZMLP project."""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    name = models.CharField(max_length=144)
    users = models.ManyToManyField(settings.AUTH_USER_MODEL, through='projects.Membership',
                                   related_name='projects')

    def __str__(self):
        return self.name

    def sync_with_zmlp(self, syncing_user):
        client = get_zmlp_superuser_client(syncing_user)
        body = {'name': self.name, 'projectId': str(self.id)}
        try:
            client.post('/api/v1/projects', body)
        except ZmlpDuplicateException:
            logger.info('Project Zero already exists in ZMLP')
        except Exception:
            # Having a hard time catching all possible exceptions, reraise one we know.
            raise requests.exceptions.ConnectionError()


class Membership(models.Model):
    """Associates a wallet User with a Project. Primarily used as a
    through model for connecting users and projects. Also stores the
    api key the user needs to access the ZMLP project.

    """
    user = models.ForeignKey(settings.AUTH_USER_MODEL, related_name='memberships',
                             on_delete=models.CASCADE)
    project = models.ForeignKey(Project, on_delete=models.CASCADE)
    apikey = encrypt(models.TextField())

    class Meta:
        unique_together = (
            ('user', 'project')
        )

    def __str__(self):
        return f'{self.project.name} - {self.user.username}'
