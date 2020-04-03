import uuid

from django.db import models
from django.contrib.auth import get_user_model
from django.contrib.postgres.fields import JSONField

from projects.models import Project

User = get_user_model()


class Search(models.Model):
    """A Search query saved for further use."""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    project = models.ForeignKey(Project, on_delete=models.CASCADE)
    name = models.CharField(max_length=128)
    search = JSONField()
    created_date = models.DateTimeField(auto_now_add=True)
    modified_date = models.DateTimeField(auto_now=True)
    created_by = models.ForeignKey(User, on_delete=models.PROTECT)

    class Meta:
        verbose_name_plural = 'Searches'