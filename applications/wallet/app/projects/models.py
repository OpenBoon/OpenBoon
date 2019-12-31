import uuid
from django.conf import settings
from django.db import models
from django_cryptography.fields import encrypt


class Project(models.Model):
    """Represents a ZMLP project."""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    name = models.CharField(max_length=144)
    users = models.ManyToManyField(settings.AUTH_USER_MODEL, through='projects.Membership',
                                   related_name='projects')

    def __str__(self):
        return self.name


class Membership(models.Model):
    """Associates a wallet User with a Project. Primarily used as a
    through model for connecting users and projects. Also stores the
    api key the user needs to access the ZMLP project.

    """
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    project = models.ForeignKey(Project, on_delete=models.CASCADE)
    apikey = encrypt(models.TextField())

    class Meta:
        unique_together = (
            ('user', 'project')
        )

    def __str__(self):
        return f'{self.project.name} - {self.user.username}'
