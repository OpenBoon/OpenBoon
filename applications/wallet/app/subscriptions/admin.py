from django.contrib import admin
from django.contrib.admin import ModelAdmin

from projects.models import Project
from subscriptions.models import Subscription


@admin.register(Subscription)
class SubscriptionAdmin(ModelAdmin):
    list_display = ('project', 'tier')
    list_filter = ('tier',)
    search_fields = ('project__name',)
