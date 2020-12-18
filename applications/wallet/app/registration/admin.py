from django.contrib import admin
from django.contrib.admin import ModelAdmin

from registration.models import UserRegistrationToken


class UserAdmin(ModelAdmin):
    fieldsets = [
        (None, {'fields': ('username', 'email', 'first_name', 'last_name',
                           'is_active', 'is_staff', 'is_superuser')})
    ]
    list_display = ('username', 'email', 'first_name', 'last_name', 'is_active', 'last_login',
                    'date_joined', 'is_superuser', 'is_staff')
    search_fields = ('email', 'first_name', 'last_name')
    list_filter = ('is_active', 'is_superuser', 'is_staff')
    exclude = ('permissions',)


admin.site.register(UserRegistrationToken)
