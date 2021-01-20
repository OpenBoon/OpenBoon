import uuid

from django.contrib.auth import get_user_model
from django.db import models

from projects.models import Project
from wallet.fields import JSONField
from wallet.mixins import UUIDMixin, TimeStampMixin

User = get_user_model()


class Search(UUIDMixin, TimeStampMixin):
    """A Search query saved for further use."""
    project = models.ForeignKey(Project, on_delete=models.CASCADE)
    name = models.CharField(max_length=128)
    search = JSONField()
    createdBy = models.ForeignKey(User, on_delete=models.PROTECT)

    class Meta:
        verbose_name_plural = 'Searches'

    def __str__(self):
        return f'{self.project} - {self.name}'
