from django.contrib import admin
from django.contrib.admin import ModelAdmin

from projects.models import Project, Membership

admin.site.register(Membership)


@admin.register(Project)
class ProjectAdmin(ModelAdmin):

    def save_model(self, request, obj, form, change):
        """Creates a new project in the database as well as ZMLP."""
        obj.sync_with_zmlp(request.user)
        obj.save()
