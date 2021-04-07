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


def org_owners(obj):
    users = obj.owners.all()
    if users:
        return ','.join([o.username for o in users])
    else:
        return 'No Owners'
org_owners.short_description = 'Owners'

@admin.register(Organization)
class OrganizationAdmin(admin.ModelAdmin):
    list_display = ['name', 'plan', org_owners, 'isActive']
    list_filter = ['plan', 'owners', 'isActive']
    search_fields = ['name', 'id']
    fields = ['name', 'owners', 'plan', 'isActive']
    inlines = [ProjectInline]


