from django.contrib import admin
from django.contrib.admin import ModelAdmin
from django.db import transaction

from projects.models import Project, Membership
from wallet.utils import get_zmlp_superuser_client


@admin.action(description='Hard delete selected projects.')
def hard_delete_project(modeladmin, request, queryset):
    """Issues a delete request to ZMLP and removes the project from Wallet."""
    for project in queryset:
        with transaction.atomic():
            project.hard_delete()


@admin.action(description='Sync selected projects with ZMLP.')
def sync_project_with_zmlp(modeladmin, request, queryset):
    """Admin action that syncs a Wallet project with ZMLP."""
    for project in queryset:
        client = get_zmlp_superuser_client(project_id=str(project.id))
        project.sync_with_zmlp()
        for membership in project.membership_set.all():
            membership.sync_with_zmlp(client)


@admin.action(description='Cycle API keys for selected projects.')
def cycle_api_key(modeladmin, request, queryset):
    """Admin action that cycles out the api key associated with the project."""
    for project in queryset:
        project.cycle_api_key()


@admin.register(Project)
class ProjectAdmin(ModelAdmin):
    actions = [sync_project_with_zmlp, cycle_api_key, hard_delete_project]
    list_display = ('name', 'id', 'organization', 'isActive')
    list_filter = ('isActive', 'organization')
    search_fields = ('name', 'id')

    def get_actions(self, request):
        actions = super().get_actions(request)
        if 'delete_selected' in actions:
            del actions['delete_selected']
        return actions

    def save_model(self, request, obj, form, change):
        """Creates a new project in the database as well as ZMLP."""
        obj.save()
        obj.sync_with_zmlp(create=True)


@admin.register(Membership)
class MembershipAdmin(ModelAdmin):
    list_display = ('user', 'project', 'roles')
    list_display_links = ('user', 'project')
    list_filter = ('project',)
    search_fields = ('user__email', 'project__name')

    def save_model(self, request, obj, form, change):
        """When adding a membership if no api key is given then a new one is created."""
        obj.sync_with_zmlp(obj.project.get_zmlp_super_client())
        obj.save()

    def delete_model(self, request, obj):
        """When deleting a Membership the ZMLP API key associated with it is deleted too."""
        client = get_zmlp_superuser_client(project_id=str(obj.project.id))
        obj.delete_and_sync_with_zmlp(client)
