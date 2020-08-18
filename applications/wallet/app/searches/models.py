import uuid

from django.contrib.auth import get_user_model
from django.db import models

from projects.models import Project
from wallet.fields import JSONField

User = get_user_model()


class Search(models.Model):
    """A Search query saved for further use."""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    project = models.ForeignKey(Project, on_delete=models.CASCADE)
    name = models.CharField(max_length=128)
    search = JSONField()
    createdDate = models.DateTimeField(auto_now_add=True)
    modifiedDate = models.DateTimeField(auto_now=True)
    createdBy = models.ForeignKey(User, on_delete=models.PROTECT)

    class Meta:
        verbose_name_plural = 'Searches'

    def __str__(self):
        return f'{self.project} - {self.name}'
