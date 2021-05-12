from django.db import models

from wallet.mixins import SortIndexMixin


class Trigger(SortIndexMixin,
              models.Model):
    name = models.CharField(max_length=64, unique=True)
    displayName = models.CharField(max_length=64, unique=True)
    description = models.CharField(max_length=255)

    def __str__(self):
        return self.displayName
