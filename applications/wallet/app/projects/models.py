import logging
import uuid

from django.conf import settings
from django.contrib.auth.models import User
from django.db import models
from django_cryptography.fields import encrypt
from multiselectfield import MultiSelectField
from zmlp.client import ZmlpDuplicateException

from projects.utils import random_project_name
from wallet.utils import get_zmlp_superuser_client

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
        user = User.objects.get(email=settings.SUPERUSER_EMAIL)
        return get_zmlp_superuser_client(user, project_id=str(self.id))

    def sync_with_zmlp(self, syncing_user):
        """Tries to create a project in ZMLP with the same name and ID. This syncs the projects
        between the Wallet DB and ZMLP and is a necessary step for any project to function
        correctly.

        Args:
            syncing_user (User): User that is attempting to sync this project with ZMLP.

        """
        client = get_zmlp_superuser_client(syncing_user)
        body = {'name': self.name, 'id': str(self.id)}
        try:
            client.post('/api/v1/projects', body)
        except ZmlpDuplicateException:
            logger.info(f'Project {self.id} already exists in ZMLP')
        if hasattr(self, 'subscription'):
            client.put('/api/v1/projects/_update_tier', {'tier': self.subscription.tier.upper()})


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
