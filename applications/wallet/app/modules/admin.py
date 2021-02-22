from django.contrib import admin
from django.contrib.admin import ModelAdmin

from modules.models import Provider


@admin.register(Provider)
class ProviderAdmin(ModelAdmin):
    list_display = ('name', 'sort_index', 'description')
