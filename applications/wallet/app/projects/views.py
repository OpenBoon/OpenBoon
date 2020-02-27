import os

import zmlp
from django.conf import settings
from django.core.exceptions import ObjectDoesNotExist
from django.db import transaction
from django.http import HttpResponseForbidden, Http404
from rest_framework import status
from rest_framework.mixins import RetrieveModelMixin, ListModelMixin
from rest_framework.response import Response
from rest_framework.viewsets import ViewSet, GenericViewSet
from zmlp import ZmlpClient

from projects.clients import ZviClient
from projects.models import Membership, Project
from projects.serializers import ProjectSerializer


class BaseProjectViewSet(ViewSet):
    """Base viewset to inherit from when needing to interact with a ZMLP Archivist in a
    project context. This viewset forces authentication and has convenience methods for
    working with the Archivist.

    The viewset also includes the necessary Serializer helper methods to allow you to
    create and use Serializers for proxied endpoint responses, as you would with a
    GenericAPIView.

    """
    zmlp_root_api_path = ''
    zmlp_only = False

    def dispatch(self, request, *args, **kwargs):
        """Overrides the dispatch method to include an instance of an archivist client
        to the view.

        """
        project = kwargs["project_pk"]
        if self.zmlp_only and settings.PLATFORM != 'zmlp':
            # This is needed to keep from returning terrible stacktraces on endpoints
            # not meant for dual platform usage
            raise Http404()

        try:
            apikey = Membership.objects.get(user=request.user, project=kwargs['project_pk']).apikey
        except ObjectDoesNotExist:
            return HttpResponseForbidden(f'{request.user.username} is not a member of '
                                         f'the project {project}')

        # Attach some useful objects for interacting with ZMLP/ZVI to the request.
        if settings.PLATFORM == 'zmlp':
            request.app = zmlp.ZmlpApp(apikey, settings.ZMLP_API_URL)
            request.client = ZmlpClient(apikey=apikey, server=settings.ZMLP_API_URL,
                                        project_id=project)
        else:
            request.client = ZviClient(apikey=apikey, server=settings.ZMLP_API_URL)

        return super().dispatch(request, *args, **kwargs)

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

    @property
    def paginator(self):
        """
        The paginator instance associated with the view, or `None`.
        """
        if not hasattr(self, '_paginator'):
            if self.pagination_class is None:
                self._paginator = None
            else:
                self._paginator = self.pagination_class()
        return self._paginator

    def paginate_queryset(self, queryset):
        """
        Return a single page of results, or `None` if pagination is disabled.
        """
        if self.paginator is None:
            return None
        return self.paginator.paginate_queryset(queryset, self.request, view=self)

    def get_paginated_response(self, data):
        """
        Return a paginated style `Response` object for the given output data.
        """
        assert self.paginator is not None
        return self.paginator.get_paginated_response(data)

    def _zmlp_list_from_search(self, request, item_modifier=None):
        """The result of this method can be returned for the list method of a concrete
        viewset if it just needs to proxy the results of a ZMLP search endpoint.

        Args:
            request (Request): Request the view method was given.
            item_modifier (function): Each item dictionary returned by the API will be
              passed to this function along with the request. The function is expected to
              modify the item in place. The arguments are passed as (request, item).

        Returns:
            Response: DRF Response that can be used directly by viewset action method.

        """
        payload = {'page': {'from': request.GET.get('from', 0),
                            'size': request.GET.get('size',
                                                    self.pagination_class.default_limit)}}
        path = os.path.join(self.zmlp_root_api_path, '_search')
        response = request.client.post(path, payload)
        content = self._get_content(response)
        current_url = request.build_absolute_uri(request.path)
        for item in content['list']:
            item['url'] = f'{current_url}{item["id"]}/'
            if item_modifier:
                item_modifier(request, item)
        paginator = self.pagination_class()
        paginator.prep_pagination_for_api_response(content, request)
        return paginator.get_paginated_response(content['list'])

    def _zmlp_list_from_es(self, request, item_modifier=None):
        payload = {'from': request.GET.get('from', 0),
                   'size': request.GET.get('size', self.pagination_class.default_limit)}
        path = os.path.join(self.zmlp_root_api_path, '_search')
        response = request.client.post(path, payload)
        content = self._get_content(response)
        results = {'list': content['hits']['hits'],
                   'page': {'from': payload['from'],
                            'size': payload['size'],
                            'totalCount': content['hits']['total']['value']}}
        for item in results['list']:
            if item_modifier:
                item_modifier(request, item)
        paginator = self.pagination_class()
        paginator.prep_pagination_for_api_response(results, request)
        return paginator.get_paginated_response(results['list'])

    def _zmlp_list_from_root(self, request):
        """The result of this method can be returned for the list method of a concrete
        viewset if it just needs to proxy the results of doing a get on the zmlp base url.

        Args:
            request (Request): Request the view method was given.

        Returns:
            Response: DRF Response that can be returned directly by the viewset list method.

        """
        response = request.client.get(self.zmlp_root_api_path)
        serializer = self.get_serializer(data=response, many=True)
        serializer.is_valid()
        return Response({'results': serializer.data})

    def _zmlp_retrieve(self, request, pk):
        """The result of this method can be returned for the retrieve method of a concrete
        viewset. if it just needs to proxy the results of a standard ZMLP endpoint for a single
        object.

        Args:
            request (Request): Request the view method was given.
            pk (str): Primary key of the object to return in the response.

        Returns:
            Response: DRF Response that can be used directly by viewset action method.

        """
        response = request.client.get(os.path.join(self.zmlp_root_api_path, pk))
        content = self._get_content(response)
        return Response(content)

    def _zmlp_destroy(self, request, pk):
        """The result of this method can be returned for the destroy method of a concrete
        viewset. if it just needs to proxy the results of a standard ZMLP endpoint for a single
        object.

        Args:
            request (Request): Request the view method was given.
            pk (str): Primary key of the object to return in the response.

        Returns:
            Response: DRF Response that can be used directly by viewset action method.

        """
        response = request.client.delete(os.path.join(self.zmlp_root_api_path, pk))
        return Response(response)

    def _get_content(self, response):
        """Returns the content of Response from the ZVI or ZMLP and as a dict."""
        if isinstance(response, dict):
            return response
        return response.json()


class ProjectViewSet(ListModelMixin,
                     RetrieveModelMixin,
                     GenericViewSet):
    """
    API endpoint that allows Projects to be viewed and created.

    If a fresh project is being created, only the `name` argument needs to be sent. The ID
    will be auto-generated.

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

        If a requested Project does not exist in either ZMLP or Wallet it will be created.
        If the Project exists in both locations a 400 response will be returned. This
        allows for the projects between both platforms to be synced.

        *Note* This endpoint does not work for ZVI configured instances.

        Args:
            request: DRF request object

        Returns:
            DRF Response object on whether or not the request succeeded.
        """
        # Create it in Django first using the standard DRF pattern
        exists_in_wallet = False
        serializer = self.get_serializer(data=request.data)
        if serializer.is_valid():
            project = serializer.save()
        else:
            try:
                if serializer.errors['id'][0].code == 'unique':
                    project = Project.objects.get(id=serializer.data['id'],
                                                  name=serializer.data['name'])
                    if project:
                        # If it already exists in Wallet let's wait and check if it
                        # needs to be created in ZMLP
                        exists_in_wallet = True
                else:
                    # If it's not an expected error let's immediately raise
                    serializer.is_valid(raise_exception=True)
            except (KeyError, IndexError):
                # If the errors didn't match what we were expecting let's also raise
                serializer.is_valid(raise_exception=True)

        # Create it in ZMLP now
        project.sync_with_zmlp(request.user)

        if exists_in_wallet:
            return Response(data={'detail': ["A project with this id already "
                                             "exists in Wallet and ZMLP."]},
                            status=status.HTTP_400_BAD_REQUEST)

        return Response(serializer.data, status=status.HTTP_201_CREATED)
