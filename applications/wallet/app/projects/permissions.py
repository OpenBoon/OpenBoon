from django.core.exceptions import ObjectDoesNotExist
from rest_framework.permissions import BasePermission

from projects.utils import is_user_project_organization_owner


class ManagerUserPermissions(BasePermission):
    """Permission class that looks for the User_Admin role for the current project."""
    message = {'detail': ['You do not have permission to manage users.']}

    def has_permission(self, request, view):
        project_id = view.kwargs['project_pk']
        if is_user_project_organization_owner(request.user, project_id):
            return True
        try:
            roles = request.user.memberships.get(project_id=project_id).roles
        except ObjectDoesNotExist:
            return False
        if 'User_Admin' not in roles:
            return False
        return True
