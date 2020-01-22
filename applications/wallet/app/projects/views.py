from django.db import transaction
from django.conf import settings
from django.core.exceptions import ObjectDoesNotExist
from django.http import HttpResponseForbidden, Http404
from zmlp import ZmlpClient
from zmlp.client import ZmlpDuplicateException

from rest_framework import status
from rest_framework.viewsets import ViewSet, GenericViewSet
from rest_framework.mixins import RetrieveModelMixin, ListModelMixin
from rest_framework.response import Response
from rest_framework.exceptions import PermissionDenied

from projects.clients import ZviClient
from projects.models import Membership
from projects.serializers import ProjectSerializer


class BaseProjectViewSet(ViewSet):
    """Base viewset to inherit from when needing to interact with a ZMLP Archivist in a
    project context. This viewset forces authentication and has convenience methods for
    working with the Archivist.

    The viewset also includes the necessary Serializer helper methods to allow you to
    create and use Serializers for proxied endpoint responses, as you would with a
    GenericAPIView.
    """

    ZMLP_ONLY = False

    def dispatch(self, request, *args, **kwargs):
        """Overrides the dispatch method to include an instance of an archivist client
        to the view.

        """

        if self.ZMLP_ONLY and settings.PLATFORM != 'zmlp':
            # This is needed to keep from returning terrible stacktraces on endpoints
            # not meant for dual platform usage
            return Http404()

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
            return ZmlpClient(apikey=apikey, server=settings.ARCHIVIST_URL)

    def get_serializer(self, *args, **kwargs):
        """
        Return the serializer instance that should be used for validating and
        deserializing input, and for serializing output.
        """
        serializer_class = self.get_serializer_class()
        kwargs['context'] = self.get_serializer_context()
        return serializer_class(*args, **kwargs)

    def get_serializer_class(self):
        """
        Return the class to use for the serializer.
        Defaults to using `self.serializer_class`.

        You may want to override this if you need to provide different
        serializations depending on the incoming request.

        (Eg. admins get full serialization, others get basic serialization)
        """
        assert self.serializer_class is not None, (
            "'%s' should either include a `serializer_class` attribute, "
            "or override the `get_serializer_class()` method."
            % self.__class__.__name__
        )

        return self.serializer_class

    def get_serializer_context(self):
        """
        Extra context provided to the serializer class.
        """
        return {
            'request': self.request,
            'format': self.format_kwarg,
            'view': self
        }


class ProjectViewSet(ListModelMixin,
                     RetrieveModelMixin,
                     GenericViewSet):
    """
    API endpoint that allows Projects to be viewed and created.

    **Note:** The POST to create against this endpoint is not supported for ZVI
    configured instances. In that case, please create projects directly in the Django
    Admin panel.
    """
    serializer_class = ProjectSerializer

    def get_queryset(self):
        return self.request.user.projects.all()

    @transaction.atomic
    def create(self, request):
        """
        Creates a project in both Wallet and ZMLP. Only the SuperUser, who has an
        API Key/Membership to the original Project Zero project can successfully
        create a project through this view.

        If an instance is brand new, use the Django Admin panel to create a Project Zero
        project with ID: `00000000-0000-0000-0000-000000000000`, and then create the
        subsequent membership for that project using the ZMLP Inception Key.

        *Note* This endpoint does not work for ZVI configured instances.

        Args:
            request: DRF request object

        Returns:
            DRF Response object on whether or not the request succeeded.
        """
        # Create it in Django first using the standard DRF pattern
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        serializer.save()

        # Create it in ZMLP now
        client = self._get_zmlp_superuser_client(request)
        body = {'name': serializer.data['name'],
                'projectId': serializer.data['id']}
        try:
            client.post('/api/v1/projects', body)
        except ZmlpDuplicateException:
            # It's ok if it already exists in ZMLP at this point.
            pass

        return Response(serializer.data, status=status.HTTP_201_CREATED)

    def _get_zmlp_superuser_client(self, request):
        """
        Helper method to return the ZMLP client specifically for the SuperUser, who is
        the only person who can create projects through this view.

        Args:
            request: Original Django request

        Returns:
            Initialized ZMLP client
        """
        # This project zero check should eventually go away as Zmlp changes.
        try:
            project = self.request.user.projects.filter(
                id='00000000-0000-0000-0000-000000000000'
            )[0]
        except IndexError:
            raise PermissionDenied(detail=(f'{request.user.username} is either not a member of '
                                           f'Project Zero or the Project has not been '
                                           f'created yet.'))
        try:
            apikey = Membership.objects.get(user=request.user, project=project).apikey
        except Membership.DoesNotExist:
            # Can't think of how this would happen, but seems safe to check
            raise PermissionDenied(detail=(f'{request.user.username} does not have a membership '
                                           f'to {project.name} setup yet. Please create in the '
                                           f'Admin console to continue.'))
        return ZmlpClient(apikey=apikey, server=settings.ARCHIVIST_URL)
