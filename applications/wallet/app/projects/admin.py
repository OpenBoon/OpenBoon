from django.contrib import admin
from django.contrib.admin import ModelAdmin

from projects.models import Project, Membership

admin.site.register(Membership)


@admin.register(Project)
class ProjectAdmin(ModelAdmin):

    def save_model(self, request, obj, form, change):
        """
        Given a model instance save it to the database.
        """
        obj.sync_project_with_zmlp(obj, request.user)
        obj.save()
