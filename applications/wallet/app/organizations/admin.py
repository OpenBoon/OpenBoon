from django.contrib import admin

from organizations.models import Organization
from projects.models import Project


class ProjectInline(admin.TabularInline):
    model = Project
    show_change_link = True
    fields = ['id', 'name', 'isActive']
    ordering = ['name']

    def has_add_permission(self, request, obj):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False


@admin.register(Organization)
class OrganizationAdmin(admin.ModelAdmin):
    list_display = ['name', 'plan', 'owners', 'isActive']
    list_filter = ['plan', 'owners', 'isActive']
    search_fields = ['name', 'id']
    fields = ['name', 'owners', 'plan', 'isActive']
    inlines = [ProjectInline]

    def owners(self, obj):
        return ','.join([o.username for o in object.owners.all()])
