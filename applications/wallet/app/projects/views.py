import logging
import os
import math
from datetime import datetime

import requests
from rest_framework.decorators import action

import boonsdk
from django.conf import settings
from django.contrib.auth import get_user_model
from django.db import transaction
from django.http import Http404, JsonResponse
from rest_framework import status
from rest_framework.mixins import RetrieveModelMixin, ListModelMixin
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.viewsets import ViewSet, GenericViewSet
from boonsdk import BoonClient
from boonsdk.client import BoonSdkConnectionException

from projects.clients import ZviClient
from projects.models import Membership, Project
from projects.permissions import ManagerUserPermissions
from projects.serializers import ProjectSerializer, ProjectUserSerializer
from wallet.paginators import FromSizePagination
from wallet.utils import validate_zmlp_data

logger = logging.getLogger(__name__)
User = get_user_model()

ES_SEARCH_TERMS = ['query', 'from', 'size', 'timeout',
                   'post_filter', 'minscore', 'suggest',
                   'highlight', 'collapse', '_source',
                   'slice', 'aggs', 'aggregations', 'sort',
                   'track_total_hits']


class BaseProjectViewSet(ViewSet):
    """Base viewset to inherit from when needing to interact with a Boon AI Archivist in a
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
            raise Http404

        try:
            apikey = Membership.objects.get(user=request.user, project=kwargs['project_pk']).apikey
        except Membership.DoesNotExist:
            return JsonResponse(data={'detail': [f'{request.user.username} is not a member of '
                                                 f'the project {project}']}, status=403)
        except TypeError:
            return JsonResponse(data={'detail': ['Unauthorized.']}, status=403)

        # Attach some useful objects for interacting with ZMLP/ZVI to the request.
        if settings.PLATFORM == 'zmlp':
            request.app = boonsdk.BoonApp(apikey, settings.BOONAI_API_URL)
            request.client = BoonClient(apikey=apikey, server=settings.BOONAI_API_URL,
                                        project_id=project)
        else:
            request.client = ZviClient(apikey=apikey, server=settings.BOONAI_API_URL)

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

    def stream_zmlp_endpoint(self, path):
        """Requests a streaming ZMLP endpoint and returns the response as an iterator."""
        response = requests.get(self.request.client.get_url(path), verify=False,
                                headers=self.request.client.headers(), stream=True)
        for block in response.iter_content(1024):
            yield block

    def _zmlp_list_from_search(self, request, item_modifier=None, search_filter=None,
                               serializer_class=None, base_url=None):
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
        for item in items:
            item['url'] = f'{current_url}{item["id"]}/'
            if item_modifier:
                item_modifier(request, item)

        if serializer_class:
            serializer = serializer_class(data=items, many=True)
        else:
            serializer = self.get_serializer(data=items, many=True)
        validate_zmlp_data(serializer)
        content['list'] = serializer.validated_data
        paginator = self.pagination_class()
        paginator.prep_pagination_for_api_response(content, request)
        return paginator.get_paginated_response(content['list'])

    def _zmlp_list_from_search_all_pages(self, request, item_modifier=None, search_filter=None,
                                         serializer_class=None, base_url=None):
        """Uses the default list endpoint logic and consumes all returned pages in one response.

        Args:
            request (Request): Request the view method was given.
            item_modifier (function): Each item dictionary returned by the API will be
              passed to this function along with the request. The function is expected to
              modify the item in place. The arguments are passed as (request, item).
            search_filter (dict): Optional filter to pass to the zmlp search endpoint.
            serializer_class (Serializer): Optional serializer to override the one set on
              the ViewSet.

        Returns:
            Response: DRF Response that can be used directly by viewset action method.

        """
        base_url = base_url or self.zmlp_root_api_path
        size = request.query_params.get('size', settings.REST_FRAMEWORK['PAGE_SIZE'])
        payload = {'page': {'from': 0, 'size': size}}

        if search_filter:
            payload.update(search_filter)
        path = os.path.join(base_url, '_search')

        additional_pages = True
        aggregated_content = {'list': []}
        while additional_pages:
            response = request.client.post(path, payload)
            content = self._get_content(response)
            aggregated_content['list'].extend(content['list'])

            _total = content['page']['totalCount']
            _next = (int(payload['page']['from']) + int(payload['page']['size']))
            if _next < _total:
                payload['page']['from'] = _next
            else:
                additional_pages = False

        current_url = request.build_absolute_uri(request.path)
        items = aggregated_content['list']
        for item in items:
            item['url'] = f'{current_url}{item["id"]}/'
            if item_modifier:
                item_modifier(request, item)

        if serializer_class:
            serializer = serializer_class(data=items, many=True)
        else:
            serializer = self.get_serializer(data=items, many=True)
        validate_zmlp_data(serializer)
        return Response({'results': serializer.validated_data})

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
                   'size': request.query_params.get('size', request.data.get('size', self.pagination_class.default_limit))}  # noqa
        content = self._zmlp_get_content_from_es_search(request, base_url=base_url,
                                                        search_filter=search_filter,
                                                        payload=payload)

        items = self._get_modified_items_from_content(request, content, item_modifier=item_modifier)

        if serializer_class:
            serializer = serializer_class(data=items, many=True)
        else:
            serializer = self.get_serializer(data=items, many=True)

        validate_zmlp_data(serializer)
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

    def _zmlp_get_all_content_from_es_search(self, request, search_filter=None, base_url=None,
                                             default_page_size=None):
        """Generates and runs the search query against a ZMLP ES search endpoint and gets all pages.

        Args:
            request (Request): Request the view method was given.
            search_filter (dict): Optional filter to pass to the zmlp search endpoint.
            base_url (str): The base zmlp api url to use.
            default_page_size (int): Paging size to use

        Returns:
            Response: DRF Response that can be used directly by viewset action method.

        """
        base_url = base_url or self.zmlp_root_api_path
        page_size = default_page_size or settings.REST_FRAMEWORK['PAGE_SIZE']
        size = request.query_params.get('size', page_size)
        payload = {'from': 0, 'size': size}

        if search_filter:
            payload.update(search_filter)
        path = os.path.join(base_url, '_search')

        additional_pages = True
        items = []
        while additional_pages:
            response = request.client.post(path, payload)
            content = self._get_content(response)
            items.extend(content['hits']['hits'])

            _total = content['hits']['total']['value']
            _next = (int(payload['from']) + int(payload['size']))
            if _next < _total:
                payload['from'] = _next
            else:
                additional_pages = False

        return items

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

    def _zmlp_list_from_root(self, request, base_url=None, serializer_class=None):
        """The result of this method can be returned for the list method of a concrete
        viewset if it just needs to proxy the results of doing a get on the zmlp base url.

        Args:
            request (Request): Request the view method was given.
            base_url (str): Overrides the url to query.
            serializer_class (Serializer): Serializer class to use on the results.

        Returns:
            Response: DRF Response that can be returned directly by the viewset list method.

        """
        if not base_url:
            base_url = self.zmlp_root_api_path
        response = request.client.get(base_url)

        if serializer_class:
            serializer = serializer_class(data=response, many=True,
                                          context=self.get_serializer_context())
        else:
            serializer = self.get_serializer(data=response, many=True)

        validate_zmlp_data(serializer)
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
        validate_zmlp_data(serializer)
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

    def dispatch(self, request, *args, **kwargs):
        """Overrides the dispatch method to include an instance of an archivist client
        to the view.

        Since this ProjectViewSet cannot inherit from the BaseProjectViewSet, this
        replicates the often used functionality around the ZMLP App and Client.

        """
        if 'pk' in kwargs:
            project = kwargs["pk"]

            try:
                apikey = Membership.objects.get(user=request.user, project=project).apikey
            except Membership.DoesNotExist:
                return JsonResponse(data={'detail': [f'{request.user.username} is not a member of '
                                                     f'the project {project}']}, status=403)
            except TypeError:
                return JsonResponse(data={'detail': ['Unauthorized.']}, status=403)

            # Attach some useful objects for interacting with ZMLP/ZVI to the request.
            if settings.PLATFORM == 'zmlp':
                request.app = boonsdk.BoonApp(apikey, settings.BOONAI_API_URL)
                request.client = BoonClient(apikey=apikey, server=settings.BOONAI_API_URL,
                                            project_id=project)
            else:
                request.client = ZviClient(apikey=apikey, server=settings.BOONAI_API_URL)

        return super().dispatch(request, *args, **kwargs)

    def get_queryset(self):
        return self.request.user.projects.filter(isActive=True)

    @action(methods=['get'], detail=True)
    def ml_usage_this_month(self, request, pk):
        """Returns the ml module usage for the current month."""
        today = datetime.today()
        first_of_the_month = f'{today.year:04d}-{today.month:02d}-01'
        path = os.path.join(settings.METRICS_API_URL, 'api/v1/apicalls/tiered_usage')
        try:
            response = requests.get(path, {'after': first_of_the_month, 'project': pk})
            response.raise_for_status()
        except (requests.exceptions.ConnectionError, requests.exceptions.HTTPError):
            return Response({})
        return Response(response.json())

    @action(methods=['get'], detail=True)
    def total_storage_usage(self, request, pk):
        """Returns the video and image/document usage of currently live assets."""
        path = 'api/v3/assets/_search'
        response_body = {}

        # Get Image/Document count
        query = {
            'track_total_hits': True,
            'query': {
                'bool': {
                    'filter': [
                        {'terms': {
                            'media.type': ['image', 'document']
                        }}
                    ]
                }
            }
        }
        try:
            response = request.client.post(path, query)
        except (requests.exceptions.ConnectionError, BoonSdkConnectionException):
            msg = (f'Unable to retrieve image/document count query for project {pk}.')
            logger.warning(msg)
        else:
            response_body['image_count'] = response['hits']['total']['value']

        # Get Aggregation for video minutes
        query = {
            'track_total_hits': True,
            'query': {
                'bool': {
                    'filter': [
                        {'terms': {
                            'media.type': ['video']
                        }}
                    ]
                }
            },
            'aggs': {
                'video_seconds': {
                    'sum': {
                        'field': 'media.length'
                    }
                }
            }
        }
        try:
            response = request.client.post(path, query)
        except (requests.exceptions.ConnectionError, BoonSdkConnectionException):
            msg = (f'Unable to retrieve video seconds sum for project {pk}.')
            logger.warning(msg)
        else:
            video_seconds = response['aggregations']['sum#video_seconds']['value']
            response_body['video_hours'] = self._get_usage_hours_from_seconds(video_seconds)

        return Response(response_body)

    def _get_usage_hours_from_seconds(self, seconds):
        """Converts seconds to hours and always rounds up."""
        return math.ceil(seconds / 60 / 60)


class ProjectUserViewSet(BaseProjectViewSet):
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
            return Response(data={'detail': ['Batch argument provided with single creation arguments.']},  # noqa
                            status=status.HTTP_400_BAD_REQUEST)
        elif batch:
            response_body = {'results': {'succeeded': [], 'failed': []}}
            for entry in batch:
                response = self._create_project_user(request, project_pk, entry)
                content = {'email': entry.get('email'),
                           'roles': entry.get('roles'),
                           'statusCode': response.status_code,
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
            return Response(data={'detail': ['Roles must be supplied.']},
                            status=status.HTTP_400_BAD_REQUEST)
        membership = self.get_object(pk, project_pk)
        if membership.roles != new_roles:
            membership.roles = new_roles
            try:
                membership.sync_with_zmlp(request.client)
            except IOError:
                return Response(data={'detail': ['Error deleting apikey.']},
                                status=status.HTTP_500_INTERNAL_SERVER_ERROR)
            except ValueError:
                return Response(data={'detail': ['Unable to modify the admin key.']},
                                status=status.HTTP_400_BAD_REQUEST)
        serializer = self.get_serializer(membership.user, context={'request': request})
        return Response(data=serializer.data, status=status.HTTP_200_OK)

    @transaction.atomic
    def destroy(self, request, project_pk, pk):
        # Remove the User's Membership and delete the associated apikey
        membership = self.get_object(pk, project_pk)

        # Don't allow a User to remove themselves
        if request.user == membership.user:
            return Response(data={'detail': ['Cannot remove yourself from a project.']},
                            status=status.HTTP_403_FORBIDDEN)

        try:
            membership.delete_and_sync_with_zmlp(request.client)
        except IOError:
            return Response(data={'detail': ['Error deleting apikey.']},
                            status=status.HTTP_500_INTERNAL_SERVER_ERROR)
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
            return Response(data={'detail': ['Email and Roles are required.']},
                            status=status.HTTP_400_BAD_REQUEST)

        # Get the current project and User
        project = self.get_project_object(project_pk)
        try:
            user = User.objects.get(username=email)
        except User.DoesNotExist:
            return Response(data={'detail': ['No user with the given email.']},
                            status=status.HTTP_404_NOT_FOUND)

        # If the membership already exists return the correct status code.
        try:
            membership = user.memberships.get(project=project)
            if membership.roles == requested_roles:
                serializer = self.get_serializer(user, context={'request': request})
                return Response(data=serializer.data, status=status.HTTP_200_OK)
            else:
                return Response({'detail': ['This user already exists in this project '
                                            'with different permissions.']}, status=409)
        except Membership.DoesNotExist:
            pass

        # Create a membership for given user
        membership = Membership(user=user, project=project, roles=requested_roles)
        membership.sync_with_zmlp(client=request.client)

        # Serialize the Resulting user like the Detail endpoint
        serializer = self.get_serializer(user, context={'request': request})
        return Response(data=serializer.data, status=status.HTTP_201_CREATED)
