from django.conf import settings
from rest_framework.exceptions import PermissionDenied
from zmlp import ZmlpClient


def get_zmlp_superuser_client(user):
    """
    Helper method to return the ZMLP client specifically for the SuperUser, who is
    the only person who can create projects.

    Args:
        user: User to try getting a superuser level client for.

    Returns:
        Initialized ZMLP client

    """
    from projects.models import Membership
    # This project zero check should eventually go away as Zmlp changes.
    try:
        project = user.projects.filter(
            id='00000000-0000-0000-0000-000000000000'
        )[0]
    except IndexError:
        raise PermissionDenied(detail=(f'{user.username} is either not a member of '
                                       f'Project Zero or the Project has not been '
                                       f'created yet.'))
    try:
        apikey = Membership.objects.get(user=user, project=project).apikey
    except Membership.DoesNotExist:
        # Can't think of how this would happen, but seems safe to check
        raise PermissionDenied(detail=(f'{user.username} does not have a membership '
                                       f'to {project.name} setup yet. Please create in the '
                                       f'Admin console to continue.'))
    return ZmlpClient(apikey=apikey, server=settings.ZMLP_API_URL)
