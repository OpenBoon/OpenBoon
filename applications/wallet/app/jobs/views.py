from django.conf import settings
from rest_framework import status
from rest_framework.response import Response
from rest_framework.decorators import action

from projects.views import BaseProjectViewSet
from wallet.paginators import FromSizePagination


class JobsViewSet(BaseProjectViewSet):

    pagination_class = FromSizePagination

    def list(self, request, project_pk, client):
        payload = {'page': {'from': request.GET.get('from', 0),
                            'size': request.GET.get('size', self.pagination_class.default_limit)}}
        response = client.post('/api/v1/jobs/_search', payload)
        if settings.PLATFORM == 'zvi':
            content = response.json()
        else:
            content = response

        current_url = request.build_absolute_uri(request.path)
        for item in content['list']:
            item['url'] = f'{current_url}{item["id"]}/'
            item['actions'] = self._get_action_links(request, item['url'], detail=True)

        paginator = self.pagination_class()
        paginator.prep_pagination_for_api_response(content, request)
        return paginator.get_paginated_response(content['list'])

    def retrieve(self, request, project_pk, client, pk):
        response = client.get(f'/api/v1/jobs/{pk}')
        content = response.json()
        content['actions'] = self._get_action_links(request)
        return Response(content)

    def _get_action_links(self, request, current_url=None, detail=None):
        """
        Determines the appropriate hyperlinks for all the available actions on a specific
        detailed job view.

        The `current_url` argument is useful when generating the urls for a list of IDs.

        Args:
            request (Request): Incoming request
            current_url (str): Optional URL to use as the base for actions
            detail (bool): Whether to include detail actions or or list actions

        Returns:
            (dict): Hyperlinks to the available actions to include in the Response

        """
        if current_url is not None:
            item_url = current_url
        else:
            item_url = request.build_absolute_uri(request.path)
        actions = self.get_extra_actions()
        action_map = {}
        is_detail = detail if detail is not None else self.detail
        for _action in actions:
            if _action.detail == is_detail:
                action_map[_action.url_name] = f'{item_url}{_action.url_path}/'
        return action_map

    @action(detail=True, methods=['get'])
    def errors(self, request, project_pk, client, pk):
        """
        Retrieves all the errors that the tasks of the given job may have triggered.

        """
        payload = {'jobIds': [pk],
                   'page': {'from': request.GET.get('from', 0),
                            'size': request.GET.get('size', self.pagination_class.default_limit)}}
        response = client.post(f'/api/v1/taskerrors/_search', payload)
        content = response.json()
        paginator = self.pagination_class()
        paginator.prep_pagination_for_api_response(content, request)
        return paginator.get_paginated_response(content['list'])

    @action(detail=True, methods=['put'])
    def pause(self, request, project_pk, client, pk):
        """
        Pauses the running job.

        The endpoint expects a `PUT` request with an empty body.

        """
        new_values = {'paused': True}
        request_body = self._get_updated_info(client, pk, new_values)
        response = client.put(f'/api/v1/jobs/{pk}', request_body)
        return Response(response.json())

    @action(detail=True, methods=['put'])
    def resume(self, request, project_pk, client, pk):
        """
        Resumes the paused job.

        The endpoint expects a `PUT` request with an empty body.

        """
        new_values = {'paused': False}
        request_body = self._get_updated_info(client, pk, new_values)
        response = client.put(f'/api/v1/jobs/{pk}', request_body)
        return Response(response.json())

    @action(detail=True, methods=['put'])
    def cancel(self, request, project_pk, client, pk):
        """
        Cancels the given job.

        The endpoint expects a `PUT` request with an empty body.

        """
        response = client.put(f'/api/v1/jobs/{pk}/_cancel', {})
        return Response(response.json())

    @action(detail=True, methods=['put'])
    def restart(self, request, project_pk, client, pk):
        """
        Restarts the cancelled job.

        The endpoint expects a `PUT` request with an empty body.

        """
        response = client.put(f'/api/v1/jobs/{pk}/_restart', {})
        return Response(response.json())

    @action(detail=True, methods=['put'])
    def priority(self, request, project_pk, client, pk):
        """
        Sets the priority order of the given job in order to control which jobs
        run first.

        The endpoint expects a `PUT` request with a JSON body in the form of:

        `{"priority": 100}`

        With the value of the "priority" key being an integer.

        """
        priority = request.data.get('priority', None)
        if priority is None:
            msg = 'Unable to find a valid `priority` value to use.'
            return Response({'msg': msg}, status.HTTP_400_BAD_REQUEST)
        try:
            priority = int(priority)
        except ValueError:
            msg = 'Invalid `priority` value provided. Expected an integer.'
            return Response({'msg': msg}, status.HTTP_400_BAD_REQUEST)
        new_values = {'priority': priority}
        request_body = self._get_updated_info(client, pk, new_values)
        response = client.put(f'/api/v1/jobs/{pk}', request_body)
        return Response(response.json())

    @action(detail=True, methods=['put'], name='Max Running Tasks')
    def max_running_tasks(self, request, project_pk, client, pk):
        """
        Sets the maximum number of running tasks for the given job.

        The endpoint expects a `PUT` request with a JSON body in the form of:

        `{"max_running_tasks": 2}`

        With the value of the "max_running_tasks" key being an integer.

        """
        max_running_tasks = request.data.get('max_running_tasks', None)
        if max_running_tasks is None:
            msg = 'Unable to find a valid `max_running_tasks` value to use.'
            return Response({'msg': msg}, status.HTTP_400_BAD_REQUEST)
        try:
            max_running_tasks = int(max_running_tasks)
        except ValueError:
            msg = 'Invalid `max_running_tasks` value provided. Expected an integer.'
            return Response({'msg': msg}, status.HTTP_400_BAD_REQUEST)
        new_values = {'maxRunningTasks': max_running_tasks}
        request_body = self._get_updated_info(client, pk, new_values)
        response = client.put(f'/api/v1/jobs/{pk}', request_body)
        return Response(response.json())

    @action(detail=True, methods=['put'], name='Retry All Failures')
    def retry_all_failures(self, request, project_pk, client, pk):
        """
        Finds every failed task in the given job and retries them.

        The endpoint expects a `PUT` request with an empty body.

        """
        response = client.put(f'/api/v1/jobs/{pk}/_retryAllFailures', {})
        return Response(response.json())

    def _get_updated_info(self, client, pk, new_values):
        """
        Pulls the job info for the specified pk, and then updates the job spec
        values with those specified. Used for the various PUT requests to modify jobs.

        Args:
             client: Client to use for the HTTP calls, specific to the platform
             pk (str): The UUID for the job
             new_values (dict): The new values to use

        Returns:
            (dict): Full job spec with updated values
        """
        response = client.get(f'/api/v1/jobs/{pk}')
        body = response.json()
        job_spec = {
            'name': body['name'],
            'priority': body['priority'],
            'paused': body['paused'],
            'timePauseExpired': body['timePauseExpired']
        }
        job_spec.update(new_values)
        return job_spec
