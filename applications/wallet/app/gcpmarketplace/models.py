from django.conf import settings
from django.db import models

from projects.models import Project


class MarketplaceEntitlement(models.Model):
    """Represents a connection between a Wallet user and a Google Marketplace entitlement."""
    name = models.CharField(max_length=255, unique=True)
    project = models.OneToOneField(Project, models.CASCADE,
                                   related_name='marketplace_entitlement')

    def __str__(self):
        return f'{self.project.name} - {self.name}'


class MarketplaceAccount(models.Model):
    """Represents a connection between a Wallet user and a Google Marketplace account."""
    name = models.CharField(max_length=255, unique=True)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, models.CASCADE,
                             related_name='marketplace_accounts')

    def __str__(self):
        return f'{self.user.username} - {self.name}'
