from django.conf import settings


def get_permissions_for_roles(requested_roles):
    """Helper method to convert roles to permissions.

    Pulls the appropriate roles from the Settings file and gathers all permissions
    needed to satisfy the desired roles.

    Args:
        requested_roles: The roles to look up permissions for.

    Returns:
        list: The permissions needed for the given roles.
    """
    permissions = []
    for role in settings.ROLES:
        if role['name'] in requested_roles:
            permissions.extend(role['permissions'])
    return list(set(permissions))
