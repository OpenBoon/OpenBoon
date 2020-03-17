from django.contrib import admin
from django.contrib.admin import ModelAdmin

from apikeys.utils import create_zmlp_api_key
from projects.models import Project, Membership
from wallet.utils import get_zmlp_superuser_client


@admin.register(Project)
class ProjectAdmin(ModelAdmin):

    def save_model(self, request, obj, form, change):
        """Creates a new project in the database as well as ZMLP."""
        obj.sync_with_zmlp(request.user)
        obj.save()


@admin.register(Membership)
class MembershipAdmin(ModelAdmin):
    actions = None  # Removed since deleting a membership in the DB

    def save_model(self, request, obj, form, change):
        """When adding a membership if no api key is given then a new one is created."""
        if not obj.apikey:
            client = get_zmlp_superuser_client(request.user, project_id=str(obj.project.id))
            permissions = ["AssetsImport", "ProjectManage", "AssetsRead", "AssetsDelete"]
            obj.apikey = create_zmlp_api_key(client, str(obj), permissions)
        obj.save()
