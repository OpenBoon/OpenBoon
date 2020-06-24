from django.contrib import admin
from django.contrib.admin import ModelAdmin

from projects.models import Project, Membership
from wallet.utils import get_zmlp_superuser_client


def sync_project_with_zmlp(modeladmin, request, queryset):
    """Admin action that syncs a Wallet project with ZMLP."""
    for project in queryset:
        project.sync_with_zmlp(request.user)
        client = get_zmlp_superuser_client(request.user, project_id=str(project.id))
        for membership in project.membership_set.all():
            membership.sync_with_zmlp(client)


@admin.register(Project)
class ProjectAdmin(ModelAdmin):
    actions = [sync_project_with_zmlp]
    list_display = ('name', 'id', 'tier', 'is_active')
    list_filter = ('is_active',)
    search_fields = ('name', 'id')

    def tier(self, project):
        if project.subscription:
            return project.subscription.tier
        return None

    def save_model(self, request, obj, form, change):
        """Creates a new project in the database as well as ZMLP."""
        obj.save()
        obj.sync_with_zmlp(request.user)


def sync_membership_with_zmlp(modeladmin, request, queryset):
    """Admin action that syncs a Wallet project with ZMLP."""
    for membership in queryset:
        membership.sync_with_zmlp(request.user)


@admin.register(Membership)
class MembershipAdmin(ModelAdmin):
    list_display = ('user', 'project', 'roles')
    list_display_links = ('user', 'project')
    list_filter = ('project',)
    search_fields = ('user__email', 'project__name')

    def save_model(self, request, obj, form, change):
        """When adding a membership if no api key is given then a new one is created."""
        client = get_zmlp_superuser_client(request.user, project_id=str(obj.project.id))
        obj.sync_with_zmlp(client)
        obj.save()

    def delete_model(self, request, obj):
        client = get_zmlp_superuser_client(request.user, project_id=str(obj.project.id))
        obj.delete_and_sync_with_zmlp(client)

