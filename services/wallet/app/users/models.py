import base64
import json

from django.conf import settings
from django.contrib.auth.models import AbstractUser
from django.contrib.postgres.fields import ArrayField
from django.db import models, transaction
from django_cryptography.fields import encrypt
from zorroa import ZmlpClient

from users.clients import ZviClient


class User(AbstractUser):
    projects = ArrayField(models.UUIDField(), default=list)

    def get_client(self, project):
        apikey = UserApiKey.objects.get(user=self, project=project).key
        if settings.PLATFORM == 'zvi':
            return ZviClient(apikey=apikey, server=settings.ARCHIVIST_URL)
        else:
            return ZmlpClient(apikey=apikey, server=settings.ARCHIVIST_URL)

    def add_project(self, key, project):
        if project not in self.projects:
            with transaction.atomic():
                self.projects.append(project)
                UserApiKey.objects.create(key=key, project=project, user=self)
                self.save()
        else:
            raise KeyError(f'Project {project} already exists for the user.')


class UserApiKey(models.Model):
    key = encrypt(models.TextField())
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE,
                             related_name='apikey')
    project = models.UUIDField()

    class Meta:
        unique_together = (
            ('user', 'project')
        )
