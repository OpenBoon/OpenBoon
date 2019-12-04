from django.conf import settings
from django.core.exceptions import ObjectDoesNotExist
from django.http import HttpResponseForbidden
from pixml import PixmlClient
from rest_framework import viewsets
from rest_framework.viewsets import ViewSet

from projects.clients import ZviClient
from projects.models import Membership
from projects.serializers import ProjectSerializer


class BaseProjectViewSet(ViewSet):
    """Base viewset to inherit from when needing to interact with a ZMLP Archivist in a
    project context. This viewset forces authentication and has convenience methods for
    working with the Archivist.

    """
    def dispatch(self, request, *args, **kwargs):
        """Overrides the dispatch method to include an instance of an archivist client
        to the view.

        """
        try:
            kwargs['client'] = self._get_archivist_client(request, kwargs['project_pk'])
        except ObjectDoesNotExist:
            return HttpResponseForbidden(f'{request.user.username} is not a member of '
                                         f'the project {kwargs["project_pk"]}')
        except TypeError:
            # This catches when the user is not authed and the token validation fails.
            # This allows the raised error to return properly, although may not be the
            # best place to handles this
            pass
        return super().dispatch(request, *args, **kwargs)

    def _get_archivist_client(self, request, project):
        """Returns a client that can be used to interact with the ZMLP Archivist.

        Args:
            request (HttpRequest): HTTP Request to get an authenticated user from.
            project (str): Project to configure the client to use.

        Returns (ZmlpClient, ZviClient): Archivist client.

        """
        apikey = Membership.objects.get(user=request.user, project=project).apikey
        if settings.PLATFORM == 'zvi':
            return ZviClient(apikey=apikey, server=settings.ARCHIVIST_URL)
        else:
            return PixmlClient(apikey=apikey, server=settings.ARCHIVIST_URL)


class ProjectViewSet(viewsets.ReadOnlyModelViewSet):
    """API endpoint that allows Projects to be viewed."""
    serializer_class = ProjectSerializer

    def get_queryset(self):
        return self.request.user.projects.all()
