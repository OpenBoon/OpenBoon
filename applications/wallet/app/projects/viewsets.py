import os
from enum import Enum

import boonsdk
import requests
from boonsdk import BoonClient
from django.conf import settings
from django.http import JsonResponse
from rest_framework import status
from rest_framework.response import Response
from rest_framework.viewsets import ViewSet

from projects.models import Project, Membership
from projects.utils import is_user_project_organization_owner
from wallet.utils import validate_zmlp_data


ES_SEARCH_TERMS = ['query', 'from', 'size', 'timeout',
                   'post_filter', 'minscore', 'suggest',
                   'highlight', 'collapse', '_source',
                   'slice', 'aggs', 'aggregations', 'sort',
                   'track_total_hits']


class ZmlpCreateMixin(object):
    def create(self, request, project_pk):
        SerializerClass = self.get_serializer_class()
        request_serializer = SerializerClass(data=request.data)
        request_serializer.is_valid(raise_exception=True)
        response = request.client.post(self.zmlp_root_api_path, request_serializer.data)
        response_serializer = SerializerClass(data=response)
        response_serializer.is_valid(raise_exception=True)
        return Response(response_serializer.validated_data, status=status.HTTP_201_CREATED)


class ListViewType(Enum):
    ES = 'es'
    ROOT = 'root'
    SEARCH = 'search'


class ZmlpListMixin(object):
    list_type = None
    list_filter = None

    @staticmethod
    def list_modifier(request, item):
        """This method is passed as the item_modifier to zmlp list functions. Override this
        in the concrete class to modify the results from ZMLP."""
        pass

    def list(self, request, project_pk):
        if self.list_type == ListViewType.SEARCH:
            return self._zmlp_list_from_search(request, search_filter=self.list_filter,
                                               item_modifier=self.list_modifier)
        elif self.list_type == ListViewType.ES:
            return self._zmlp_list_from_es(request, search_filter=self.list_filter,
                                           item_modifier=self.list_modifier)
        elif self.list_type == ListViewType.ROOT:
            return self._zmlp_list_from_root(request)
        else:
            raise NotImplementedError


class ZmlpRetrieveMixin(object):
    @staticmethod
    def retrieve_modifier(request, item):
        """This method is passed as the item_modifier to _zmlp_retrieve. Override this
        in the concrete class to modify the results from ZMLP."""
        pass

    def retrieve(self, request, project_pk, pk):
        return self._zmlp_retrieve(request, pk, item_modifier=self.retrieve_modifier)


class ZmlpDestroyMixin(object):
    def destroy(self, request, project_pk, pk):
        return self._zmlp_destroy(request, pk)


class BaseProjectViewSet(ViewSet):
    """Base viewset to inherit from when needing to interact with a Boon AI Archivist in a
    project context. This viewset forces authentication and has convenience methods for
    working with the Archivist.

    The viewset also includes the necessary Serializer helper methods to allow you to
    create and use Serializers for proxied endpoint responses, as you would with a
    GenericAPIView.

    """
    zmlp_root_api_path = ''
    project_pk_kwarg = 'project_pk'

    def dispatch(self, request, *args, **kwargs):
        """Overrides the dispatch method to include an instance of an archivist client
        to the view.

        """
        if self.project_pk_kwarg in kwargs:
            project = kwargs[self.project_pk_kwarg]
            try:
                if is_user_project_organization_owner(request.user, project):
                    apikey = Project.objects.get(id=project).apikey
                else:
                    apikey = Membership.objects.get(user=request.user, project=project).apikey
            except Membership.DoesNotExist:
                return JsonResponse(data={'detail': [f'{request.user.username} is not a member of '
                                                     f'the project {project}']}, status=403)
            except TypeError:
                return JsonResponse(data={'detail': ['Unauthorized.']}, status=403)
            request.app = boonsdk.BoonApp(apikey, settings.BOONAI_API_URL)
            request.client = BoonClient(apikey=apikey, server=settings.BOONAI_API_URL,
                                        project_id=project)
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
        if 'success' in response and not response['success']:
            return Response({'detail': ['Resource deletion failed.']}, status=500)
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
