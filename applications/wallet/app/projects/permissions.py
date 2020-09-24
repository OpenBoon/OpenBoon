from django.core.exceptions import ObjectDoesNotExist
from rest_framework.permissions import BasePermission


class ManagerUserPermissions(BasePermission):
    """Permission class that looks for the User_Admin role for the current project."""
    message = {'detail': ['You do not have permission to manage users.']}

    def has_permission(self, request, view):
        try:
            roles = request.user.memberships.get(project_id=view.kwargs['project_pk']).roles
        except ObjectDoesNotExist:
            return False
        if 'User_Admin' not in roles:
            return False
        return True
