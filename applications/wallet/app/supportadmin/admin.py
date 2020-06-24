from django.contrib import admin
from django.contrib.admin import AdminSite, ModelAdmin
from django.contrib.auth import get_user_model

from modules.models import Provider
from projects.admin import ProjectAdmin
from projects.models import Project, Membership
from subscriptions.models import Subscription


class NoDeleteMixin():
    def get_actions(self, request):
        actions = super().get_actions(request)
        if 'delete_selected' in actions:
            del actions['delete_selected']
        return actions


class MembershipInline(admin.TabularInline):
    model = Membership
    min_num = 1


class SubscriptionInline(admin.StackedInline):
    model = Subscription


class SupportUserAdmin(NoDeleteMixin, ModelAdmin):
    fieldsets = [
        (None, {'fields': ('email', 'username', 'first_name', 'last_name',
                           'is_active', 'is_staff')})
    ]
    list_display = ('email', 'first_name', 'last_name', 'is_active', 'last_login', 'date_joined')
    search_fields = ('email', 'first_name', 'last_name')
    list_filter = ('is_active',)
    exclude = ('permissions',)
    inlines = [MembershipInline]


class SupportProjectAdmin(NoDeleteMixin, ProjectAdmin):
    inlines = [SubscriptionInline, MembershipInline]


class SupportAdminSite(AdminSite):
    """Stripped down admin site that is safe for the support team to use."""
    site_header = "ZVI Administration Console"
    site_title = "ZVI Administration Console"
    index_title = "Welcome to the ZVI Administration Console."


support_admin_site = SupportAdminSite(name='support_admin')
support_admin_site.register(Project, SupportProjectAdmin)
support_admin_site.register(get_user_model(), SupportUserAdmin)
support_admin_site.register(Provider)
