from django.contrib.auth import get_user_model
from django.db import models

from wallet.mixins import TimeStampMixin, UUIDMixin

User = get_user_model()


class Organization(UUIDMixin, TimeStampMixin):
    """An organization is a collection of projects with an owner. Currently this is only
    used for billing purposes."""
    name = models.CharField(max_length=64)
    owner = models.ForeignKey(User, on_delete=models.DO_NOTHING, null=True, blank=True)

    def __str__(self):
        return self.name

    def __repr__(self):
        return f"Organization(name='{self.name}', owner_id={self.owner_id})"