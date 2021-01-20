from django.conf import settings
from django.db import models

from organizations.models import Organization
from projects.models import Project


class MarketplaceEntitlement(models.Model):
    """Represents a connection between a Wallet Organization and a Google Marketplace entitlement."""
    name = models.CharField(max_length=255, unique=True)
    organization = models.OneToOneField(Organization, models.SET_NULL,
                                        related_name='marketplace_entitlement')

    def __str__(self):
        return f'{self.organization.name} - {self.name}'

    def __repr__(self):
        return f"MarketplaceEntitlement(name='{self.name}, organization_id='{self.organization_id}')"


class MarketplaceAccount(models.Model):
    """Represents a connection between a Wallet user and a Google Marketplace account."""
    name = models.CharField(max_length=255, unique=True)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, models.CASCADE,
                             related_name='marketplace_accounts')

    def __str__(self):
        return f'{self.user.username} - {self.name}'

    def __repr__(self):
        return f"MarketplaceAccount(name='{self.name}, user_id='{self.user_id}')"
