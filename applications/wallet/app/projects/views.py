import logging
import os

import zmlp
from django.conf import settings
from django.contrib.auth import get_user_model
from django.db import transaction
from django.http import HttpResponseForbidden, Http404
from rest_framework import status
from rest_framework.mixins import RetrieveModelMixin, ListModelMixin
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.viewsets import ViewSet, GenericViewSet
from zmlp import ZmlpClient
from zmlp.client import ZmlpNotFoundException

from wallet.utils import convert_base64_to_json
from apikeys.utils import create_zmlp_api_key
from projects.clients import ZviClient
from projects.models import Membership, Project
from projects.permissions import ManagerUserPermissions
from projects.serializers import ProjectSerializer, ProjectUserSerializer
from wallet.mixins import ConvertCamelToSnakeViewSetMixin
from wallet.paginators import FromSizePagination

logger = logging.getLogger(__name__)
User = get_user_model()

ES_SEARCH_TERMS = ['query', 'from', 'size', 'timeout',
                   'post_filter', 'minscore', 'suggest',
                   'highlight', 'collapse', '_source',
                   'slice', 'aggs', 'aggregations', 'sort']


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
        except Membership.DoesNotExist:
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

    def _zmlp_list_from_search(self, request, item_modifier=None, search_filter=None,
                               serializer_class=None, base_url=None, item_filter=None):
        """The result of this method can be returned for the list method of a concrete
        viewset if it just needs to proxy the results of a ZMLP search endpoint.

        Args:
            request (Request): Request the view method was given.
            item_modifier (function): Each item dictionary returned by the API will be
              passed to this function along with the request. The function is expected to
              modify the item in place. The arguments are passed as (request, item).
            search_filter (dict): Optional filter to pass to the zmlp search endpoint.
            serializer_class (Serializer): Optional serializer to override the one set on
              the ViewSet.
            item_filter (function): Each item dictionary returned by the API will be
              passed to this function along with the request. If the function returns
              False the item will not returned in the Response. The arguments are passed
              as (request, item).

        Returns:
            Response: DRF Response that can be used directly by viewset action method.

        """
        base_url = base_url or self.zmlp_root_api_path
        payload = {'page': {'from': request.query_params.get('from', 0),
                            'size': request.query_params.get('size',
                                                             self.pagination_class.default_limit)}}
        if search_filter:
            payload.update(search_filter)
        path = os.path.join(base_url, '_search')
        response = request.client.post(path, payload)
        content = self._get_content(response)
        current_url = request.build_absolute_uri(request.path)
        items = content['list']
        items_to_remove = []
        for item in items:
            item['url'] = f'{current_url}{item["id"]}/'
            if item_modifier:
                item_modifier(request, item)
            if item_filter and not item_filter(request, item):
                items_to_remove.append(item)
        for item in items_to_remove:
            content['list'].remove(item)
        if serializer_class:
            serializer = serializer_class(data=content['list'], many=True)
        else:
            serializer = self.get_serializer(data=content['list'], many=True)
        if not serializer.is_valid():
            return Response({'detail': serializer.errors}, status=500)
        content['list'] = serializer.validated_data
        paginator = self.pagination_class()
        paginator.prep_pagination_for_api_response(content, request)
        return paginator.get_paginated_response(content['list'])

    def _zmlp_list_from_es(self, request, item_modifier=None, search_filter=None,
                           serializer_class=None, base_url=None, pagination_class=None):
        """The result of this method can be returned for the list method of a concrete
        viewset if it just needs to proxy the results of a ZMLP search endpoints that
        return raw elasticsearch data.

        Args:
            request (Request): Request the view method was given.

        Keyword Args:
            item_modifier (function): Each item dictionary returned by the API will be
              passed to this function along with the request. The function is expected to
              modify the item in place. The arguments are passed as (request, item).
            base_url (str): The base ZMLP endpoint (minus the _search).
            search_filter (dict<varies>): The json query to search for.
            serializer_class (Serializer): An override to the default serializer for the response.
            pagination_class (Paginator): An override to the default paginator for the response.

        Returns:
            Response: DRF Response that can be used directly by viewset action method.

        """
        # Check for pagination query params first, and then check the post body
        payload = {'from': request.query_params.get('from', request.data.get('from', 0)),
                   'size': request.query_params.get('size', request.data.get('size',
                                                                             self.pagination_class.default_limit))}  # noqa
        content = self._zmlp_get_content_from_es_search(request, base_url=base_url,
                                                        search_filter=search_filter,
                                                        payload=payload)

        items = self._get_modified_items_from_content(request, content, item_modifier=item_modifier)

        if serializer_class:
            serializer = serializer_class(data=items, many=True)
        else:
            serializer = self.get_serializer(data=items, many=True)

        if not serializer.is_valid():
            return Response({'detail': serializer.errors}, status=500)
        results = {'list': serializer.validated_data,
                   'page': {'from': payload['from'],
                            'size': payload['size'],
                            'totalCount': content['hits']['total']['value']}}
        if pagination_class:
            paginator = pagination_class()
        else:
            paginator = self.pagination_class()
        paginator.prep_pagination_for_api_response(results, request)
        return paginator.get_paginated_response(results['list'])

    def _zmlp_get_content_from_es_search(self, request, base_url=None, search_filter=None,
                                         payload=None):
        """Generates and runs the search query against a ZMLP ES endpoint from a request.

        Args:
            request (Request): Request the view method was given.

        Keyword Args:
            base_url (str): The base ZMLP endpoint (minus the _search).
            search_filter (dict<varies>): The json query to search for.
            payload (dict): Optional payload to include with the request

        Returns:
            (dict<varies>): The dict of results from the ES endpoint.
        """
        base_url = base_url or self.zmlp_root_api_path
        payload = payload or {}

        # Whitelist any of the ES specific query related terms
        for term in ES_SEARCH_TERMS:
            value = request.data.get(term)
            if value:
                payload[term] = value
            if search_filter and term in search_filter:
                payload[term] = search_filter[term]

        path = os.path.join(base_url, '_search')
        response = request.client.post(path, payload)
        return self._get_content(response)

    def _get_modified_items_from_content(self, request, content, item_modifier=None):
        """Modifies the structure of each item with the given item modifier and returns them.

        Args:
            request (Request): Request the view method was given.
            content (list): The raw dict items from an ES search endpoint.

        Keyword Args:
            item_modifier (function): The function to run over each individual item.

        Returns:
            (list): The modified items.
        """
        items = content['hits']['hits']
        for item in items:
            if item_modifier:
                item_modifier(request, item)

        return items

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
        if not serializer.is_valid():
            return Response({'detail': serializer.errors}, status=500)
        return Response({'results': serializer.data})

    def _zmlp_retrieve(self, request, pk, item_modifier=None):
        """The result of this method can be returned for the retrieve method of a concrete
        viewset. if it just needs to proxy the results of a standard ZMLP endpoint for a single
        object.

        Args:
            request (Request): Request the view method was given.
            pk (str): Primary key of the object to return in the response.
            return_data: If True the data is returned instead of a Response.

        Returns:
            Response: DRF Response that can be used directly by viewset action method.

        """
        response = request.client.get(os.path.join(self.zmlp_root_api_path, pk))
        content = self._get_content(response)
        if item_modifier:
            item_modifier(request, content)
        serializer = self.get_serializer(data=content)
        if not serializer.is_valid():
            return Response({'detail': serializer.errors}, status=500)
        return Response(serializer.data)

    def _zmlp_destroy(self, request, pk):
        """The result of this method can be returned for the destroy method of a concrete
        viewset if it just needs to proxy the results of a standard ZMLP endpoint for a single
        object.

        Args:
            request (Request): Request the view method was given.
            pk (str): Primary key of the object to return in the response.

        Returns:
            Response: DRF Response that can be used directly by viewset action method.

        """
        response = request.client.delete(os.path.join(self.zmlp_root_api_path, pk))
        return Response(response)

    def _zmlp_update(self, request, pk):
        """The result of this method can be returned for the update method of a concrete
        viewset if it just needs to proxy the results of a standard ZMLP endpoint for a single
        object.

        Args:
            request (Request): Request the view method was given.
            pk (str): Primary key of the object to return in the response.

        Returns:
            Response: DRF Response that can be used directly by viewset action method.

        """
        update_serializer = self.get_serializer(data=request.data)
        update_serializer.is_valid(raise_exception=True)
        zmlp_response = request.client.put(f'{self.zmlp_root_api_path}{pk}',
                                           update_serializer.data)
        response_serializer = self.get_serializer(data=zmlp_response)
        if not response_serializer.is_valid():
            Response({'detail': response_serializer.errors}, status=500)
        return Response(response_serializer.validated_data)

    def _get_content(self, response):
        """Returns the content of Response from the ZVI or ZMLP and as a dict."""
        if isinstance(response, dict):
            return response
        return response.json()


class ProjectViewSet(ConvertCamelToSnakeViewSetMixin,
                     ListModelMixin,
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
            except Project.DoesNotExist:
                return Response(data={'detail': 'A project with this id and a different name '
                                                'already exists in Wallet. Send the correct name '
                                                'or edit the Project in the Django Admin.'},
                                status=status.HTTP_400_BAD_REQUEST)

        # Create it in ZMLP now
        project.sync_with_zmlp(request.user)

        if exists_in_wallet:
            return Response(data={'detail': ["A project with this id already "
                                             "exists in Wallet and ZMLP."]},
                            status=status.HTTP_400_BAD_REQUEST)

        return Response(serializer.data, status=status.HTTP_201_CREATED)


class ProjectUserViewSet(ConvertCamelToSnakeViewSetMixin, BaseProjectViewSet):
    """Users who are Members of this Project.

    Available HTTP methods, endpoints, and what they do:

    * **GET** _api/v1/projects/$Project_Id/users/_ - List the Users who are members of $Project_Id
    * **GET** _api/v1/projects/$Project_Id/users/$User_Id/_ - Detail info on $User_Id
    * **POST** _api/v1/projects/$Project_Id/users/_ - Create membership/s to $Project_Id
        - To create one: `{"email": "user@email.com", "roles": ["ML_Tools"]}`
        - To create multiple: `{"batch": [
                                    {"email": "user@email.com", "roles": ["API_Key"]},
                                    {"email": "user2@email.com", "roles": ["User_Admin"]}
                                ]}`
    * **PUT** _api/v1/projects/$Project_Id/users/$User_Id/_ - Replace a Users Roles
        - To modify: `{"roles": ["$NewRoleList"]}`
    * **DELETE** _api/v1/projects/$Project_Id/users/$User_Id/_ - Remove $User_Id from $Project_Id

    """
    zmlp_only = True
    pagination_class = FromSizePagination
    serializer_class = ProjectUserSerializer
    permission_classes = [IsAuthenticated, ManagerUserPermissions]

    def get_object(self, pk, project_pk):
        try:
            return Membership.objects.get(user=pk, project=project_pk)
        except Membership.DoesNotExist:
            raise Http404

    def get_project_object(self, pk):
        try:
            return Project.objects.get(id=pk)
        except Project.DoesNotExist:
            raise Http404

    def list(self, request, project_pk):
        # Need to handle pagination
        # If the project doesn't exist or user is not a member a 403 is returned
        project = self.get_project_object(project_pk)
        users = project.users.all()
        paginated_users = self.paginate_queryset(users)
        if paginated_users is not None:
            serializer = self.get_serializer(paginated_users, many=True)
            return self.get_paginated_response(serializer.data)
        serializer = self.get_serializer(users, many=True)
        return Response(data={'results': serializer.data}, status=status.HTTP_200_OK)

    def retrieve(self, request, project_pk, pk):
        # List details about the current project User
        user = self.get_object(pk, project_pk).user
        serializer = self.get_serializer(user, context={'request': request})
        return Response(data=serializer.data, status=status.HTTP_200_OK)

    def create(self, request, project_pk):
        batch = request.data.get('batch')
        if batch and request.data.get('email'):
            return Response(data={'detail': 'Batch argument provided with single creation arguments.'},  # noqa
                            status=status.HTTP_400_BAD_REQUEST)
        elif batch:
            response_body = {'results': {'succeeded': [], 'failed': []}}
            for entry in batch:
                response = self._create_project_user(request, project_pk, entry)
                content = {'email': entry.get('email'),
                           'roles': entry.get('roles'),
                           'status_code': response.status_code,
                           'body': response.data}
                if response.status_code in [status.HTTP_201_CREATED, status.HTTP_200_OK]:
                    response_body['results']['succeeded'].append(content)
                else:
                    response_body['results']['failed'].append(content)
            return Response(data=response_body, status=status.HTTP_207_MULTI_STATUS)
        else:
            return self._create_project_user(request, project_pk, request.data)

    def update(self, request, project_pk, pk):
        # Modify the permissions of the given user
        try:
            new_roles = request.data['roles']
        except KeyError:
            return Response(data={'detail': 'Roles must be supplied.'},
                            status=status.HTTP_400_BAD_REQUEST)
        membership = self.get_object(pk, project_pk)
        email = membership.user.username
        apikey = convert_base64_to_json(membership.apikey)
        apikey_id = apikey['id']
        apikey_name = apikey.get('name')
        if apikey_name == 'admin-key':
            return Response(data={'detail': 'Unable to modify the admin key.'},
                            status=status.HTTP_400_BAD_REQUEST)

        # TODO: Replace Delete/Create logic when Auth Server supports PUT
        # Create new Key first and append epoch time (milli) to get a readable unique name
        new_permissions = self._get_permissions_for_roles(new_roles)
        name = self._get_api_key_name(email, project_pk)
        new_apikey = create_zmlp_api_key(request.client, name, new_permissions, internal=True)

        # Delete old key on success
        try:
            response = request.client.delete(f'/auth/v1/apikey/{apikey_id}')
            if not response.status_code == 200:
                return Response(data={'detail': 'Error deleting apikey.'},
                                status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        except ZmlpNotFoundException:
            logger.warning(f'Tried to delete API Key {apikey_id} for user f{request.user.id} '
                           f'while updating permissions. The API key could not be found.')

        membership.apikey = new_apikey
        membership.roles = new_roles
        membership.save()
        serializer = self.get_serializer(membership.user, context={'request': request})
        return Response(data=serializer.data, status=status.HTTP_200_OK)

    @transaction.atomic
    def destroy(self, request, project_pk, pk):
        # Remove the User's Membership and delete the associated apikey
        membership = self.get_object(pk, project_pk)
        apikey = membership.apikey

        # Don't allow a User to remove themselves
        if request.user == membership.user:
            return Response(data={'detail': 'Cannot remove yourself from a project.'},
                            status=status.HTTP_403_FORBIDDEN)

        # Delete Users Apikey
        apikey_readable = True
        try:
            key_data = convert_base64_to_json(apikey)
            apikey_id = key_data['id']
        except (ValueError, KeyError):
            logger.warning(f'Unable to decode apikey during delete for user {membership.user.id}.')
            apikey_readable = False

        if apikey_readable:
            response = request.client.delete(f'/auth/v1/apikey/{apikey_id}')
            if not response.status_code == 200:
                return Response(data={'detail': 'Error deleting apikey.'},
                                status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        membership.delete()
        return Response(status=status.HTTP_200_OK)

    def _create_project_user(self, request, project_pk, data):
        """Creates project user by generating an api key in zmlp and creating a new
        Membership.

        """
        # Get the User and add the appropriate Membership & ApiKey
        try:
            email = data['email']
            requested_roles = data['roles']
        except KeyError:
            return Response(data={'detail': 'Email and Roles are required.'},
                            status=status.HTTP_400_BAD_REQUEST)

        # Determine appropriate permissions for the roles entered
        permissions = self._get_permissions_for_roles(requested_roles)

        # Get the current project and User
        project = self.get_project_object(project_pk)
        try:
            user = User.objects.get(username=email)
        except User.DoesNotExist:
            return Response(data={'detail': 'No user with the given email.'},
                            status=status.HTTP_404_NOT_FOUND)

        # Create an apikey with the given permissions
        name = self._get_api_key_name(email, project_pk)
        encoded_apikey = create_zmlp_api_key(request.client, name, permissions, internal=True)

        # If the membership already exists return the correct status code.
        try:
            membership = user.memberships.get(project=project)
            if membership.roles == requested_roles:
                serializer = self.get_serializer(user, context={'request': request})
                return Response(data=serializer.data, status=status.HTTP_200_OK)
            else:
                return Response({'detail': 'This user already exists in this project '
                                           'with different permissions.'}, status=409)
        except Membership.DoesNotExist:
            pass

        # Create a membership for given user
        Membership.objects.create(user=user, project=project, apikey=encoded_apikey,
                                  roles=requested_roles)

        # Serialize the Resulting user like the Detail endpoint
        serializer = self.get_serializer(user, context={'request': request})
        return Response(data=serializer.data, status=status.HTTP_201_CREATED)

    def _get_permissions_for_roles(self, requested_roles):
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

    def _get_api_key_name(self, email, project_pk):
        """Generate a unique name to user for the api key."""
        return f'{email}_{project_pk}'
