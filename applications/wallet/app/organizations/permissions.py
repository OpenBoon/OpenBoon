from rest_framework.permissions import BasePermission

from organizations.models import Organization


class OrganizationOwnerPermissions(BasePermission):
    """Permission class that verifies the user is an owner of the organization."""
    message = {'detail': ['You are not an owner of this project.']}

    def has_permission(self, request, view):
        organization = Organization.objects.get(id=view.kwargs['organization_pk'])
        return organization.owners.filter(id=request.user.id).exists()
