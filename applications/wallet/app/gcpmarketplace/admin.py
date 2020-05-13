from django.contrib import admin

from gcpmarketplace.models import MarketplaceAccount, MarketplaceEntitlement

admin.site.register(MarketplaceAccount)
admin.site.register(MarketplaceEntitlement)
