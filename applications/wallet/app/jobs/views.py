import os

import stringcase
from django.http import StreamingHttpResponse
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.reverse import reverse

from jobs.serializers import JobSerializer, TaskErrorSerializer, TaskSerializer
from projects.viewsets import BaseProjectViewSet
from searches.serializers import SearchAssetSerializer
from searches.views import search_asset_modifier
from wallet.mixins import CamelCaseRendererMixin, BoonAISortArgsMixin
from wallet.paginators import ZMLPFromSizePagination
from wallet.utils import validate_zmlp_data


def set_asset_total_count(asset_counts):
    """Sets the total count on the assetsCounts blob in Job and Task returns.

    Sometimes the ZMLP api does not consistently return a total asset count. To maintain
    consistency for the frontend, this checks each return blob and adds the assetTotalCount
    to the return if it's missing or incorrect.

    Args:
        asset_counts: The full assetsCount JSON blob from the Task or Job returns.

    Returns:
        dict: The assetsCount blob with the assetsTotalCount added or corrected.
    """
    total = (asset_counts.get('assetCreatedCount', 0)
             + asset_counts.get('assetReplacedCount', 0)
             + asset_counts.get('assetWarningCount', 0)
             + asset_counts.get('assetErrorCount', 0))
    if 'assetTotalCount' in asset_counts and asset_counts['assetTotalCount'] >= total:
        return asset_counts
    asset_counts['assetTotalCount'] = total
    return asset_counts


def task_item_modifier(request, task):
    task_path = reverse('task-detail', kwargs={'project_pk': task['projectId'],
                                               'pk': task['id']})
    task_url = request.build_absolute_uri(task_path)
    task['actions'] = {'retry': f'{task_url}retry/',
                       'assets': f'{task_url}assets/',
                       'script': f'{task_url}script/',
                       'errors': f'{task_url}errors/',
                       'logs': f'{task_url}logs/'}
    task['assetCounts'] = set_asset_total_count(task['assetCounts'])


def task_error_item_modifier(request, error):
    error['jobName'] = request.client.get(f'/api/v1/jobs/{error["jobId"]}')['name']


class JobViewSet(BoonAISortArgsMixin,
                 BaseProjectViewSet):
    """CRUD operations for Boon AI processing jobs."""
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/api/v1/jobs/'
    serializer_class = JobSerializer

    def list(self, request, project_pk):
        """Lists all jobs in the job queue.

        Accepts an optional `ordering` query parameter. The value can be a comma-separated list of
        fields to sort on, with each field set to ascending (fieldname on its own) or descending
        (field name prepended with a "-").

        Also accepts a `search` query parameter, which will do a wildcard search against
        all potential job names.

        Example:
            ?ordering=timeCreated,-priority

        Return:
            (Response): Paginated contents of the listed jobs.
        """

        def item_modifier(request, job):
            job['actions'] = self._get_action_links(request, job['link'], detail=True)
            job['tasks'] = f'{job["link"]}tasks/'
            job['assetCounts'] = set_asset_total_count(job['assetCounts'])

        search_filter = {}
        # Add the sort, if any
        sort_args = self.get_boonai_sort_args(request)
        if sort_args:
            search_filter['sort'] = sort_args

        # Add the filter, if any
        filter = request.query_params.get('search')
        if filter:
            search_filter['keywords'] = filter

        # Filter on statuses/states
        states = request.query_params.get('states')
        allowed_states = ['InProgress', 'Cancelled', 'Success', 'Archived', 'Failure']
        bad_states = []
        if states:
            states = states.split(',')
            for state in states:
                if state not in allowed_states:
                    bad_states.append(state)
            if bad_states:
                return Response(status=status.HTTP_400_BAD_REQUEST,
                                data={'detail': [f'These states are not allowed: {bad_states}']})
            search_filter['states'] = states

        return self._zmlp_list_from_search(request, search_filter=search_filter,
                                           item_modifier=item_modifier)

    def retrieve(self, request, project_pk, pk):
        def item_modifier(request, job):
            current_url = request.build_absolute_uri(request.path)
            job['actions'] = self._get_action_links(request, current_url, detail=True)
            job['tasks'] = f'{current_url}tasks/'
            job['assetCounts'] = set_asset_total_count(job['assetCounts'])

        return self._zmlp_retrieve(request, pk, item_modifier=item_modifier)

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
                action_key = stringcase.camelcase(_action.url_path)
                action_map[action_key] = f'{item_url}{_action.url_path}/'
        return action_map

    @action(detail=True, methods=['get'])
    def errors(self, request, project_pk, pk):
        """
        Retrieves all the errors that the tasks of the given job may have triggered.

        """
        base_url = '/api/v1/taskerrors/'
        return self._zmlp_list_from_search(request, search_filter={'jobIds': [pk]},
                                           serializer_class=TaskErrorSerializer,
                                           base_url=base_url)

    @action(detail=True, methods=['put'])
    def pause(self, request, project_pk, pk):
        """
        Pauses the running job.

        The endpoint expects a `PUT` request with an empty body.

        """
        new_values = {'paused': True}
        request_body = self._get_updated_info(request.client, pk, new_values)
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}', request_body)
        return Response(self._get_content(response))

    @action(detail=True, methods=['put'])
    def resume(self, request, project_pk, pk):
        """
        Resumes the paused job.

        The endpoint expects a `PUT` request with an empty body.

        """
        new_values = {'paused': False}
        request_body = self._get_updated_info(request.client, pk, new_values)
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}', request_body)
        return Response(self._get_content(response))

    @action(detail=True, methods=['put'])
    def cancel(self, request, project_pk, pk):
        """
        Cancels the given job.

        The endpoint expects a `PUT` request with an empty body.

        """
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}/_cancel', {})
        return Response(self._get_content(response))

    @action(detail=True, methods=['put'])
    def restart(self, request, project_pk, pk):
        """
        Restarts the cancelled job.

        The endpoint expects a `PUT` request with an empty body.

        """
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}/_restart', {})
        return Response(self._get_content(response))

    @action(detail=True, methods=['put'])
    def priority(self, request, project_pk, pk):
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
        request_body = self._get_updated_info(request.client, pk, new_values)
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}', request_body)
        return Response(self._get_content(response))

    @action(detail=True, methods=['put'], name='Max Running Tasks')
    def max_running_tasks(self, request, project_pk, pk):
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
        request_body = self._get_updated_info(request.client, pk, new_values)
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}', request_body)
        return Response(self._get_content(response))

    @action(detail=True, methods=['put'], name='Retry All Failures')
    def retry_all_failures(self, request, project_pk, pk):
        """
        Finds every failed task in the given job and retries them.

        The endpoint expects a `PUT` request with an empty body.

        """
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}/_retryAllFailures', {})
        return Response(self._get_content(response))

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
        response = client.get(f'{self.zmlp_root_api_path}{pk}')
        body = self._get_content(response)
        job_spec = {
            'name': body['name'],
            'priority': body['priority'],
            'paused': body['paused'],
            'timePauseExpired': body['timePauseExpired'],
            'maxRunningTasks': body['maxRunningTasks']
        }
        job_spec.update(new_values)
        return job_spec


class JobTaskViewSet(BaseProjectViewSet):
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/api/v1/tasks/'
    serializer_class = TaskSerializer

    def list(self, request, project_pk, job_pk):
        return self._zmlp_list_from_search(request, item_modifier=task_item_modifier,
                                           search_filter={'jobIds': [job_pk]})


class TaskErrorViewSet(BaseProjectViewSet):
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/api/v1/taskerrors/'
    serializer_class = TaskErrorSerializer

    def list(self, request, project_pk):
        return self._zmlp_list_from_search(request, item_modifier=task_error_item_modifier)

    def retrieve(self, request, project_pk, pk):
        url = os.path.join(self.zmlp_root_api_path, '_findOne')
        error = request.client.post(url, {'ids': [pk]})
        task_error_item_modifier(request, error)
        serializer = self.get_serializer(data=error)
        validate_zmlp_data(serializer)
        return Response(serializer.validated_data)


class TaskViewSet(CamelCaseRendererMixin, BaseProjectViewSet):
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/api/v1/tasks/'
    serializer_class = TaskSerializer

    def list(self, request, project_pk):
        return self._zmlp_list_from_search(request, item_modifier=task_item_modifier)

    def retrieve(self, request, project_pk, pk):
        return self._zmlp_retrieve(request, pk, item_modifier=task_item_modifier)

    @action(detail=True, methods=['put'])
    def retry(self, request, project_pk, pk):
        """Retries a task that has failed. Expects a `PUT` with an empty body."""
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}/_retry', {})
        if response.get('success'):
            return Response({'detail': [f'Task {pk} has been successfully retried.']})
        else:
            message = f'Task {pk} failed to be retried. Message from boonsdk: {response}'
            return Response({'detail': [message]}, status=500)

    @action(detail=True, methods=['get'])
    def assets(self, request, project_pk, pk):
        """Lists all assets associated with a task."""
        script = request.client.get(f'/api/v1/tasks/{pk}/_script')
        asset_ids = []

        # New style for task scripts is a flat list of asset ids.
        if 'assetIds' in script:
            asset_ids = script['assetIds']

        # Older style for task scripts was a list of asset objects.
        elif 'assets' in script:
            asset_ids = [a['id'] for a in script['assets']]

        return self._zmlp_list_from_es(request,
                                       search_filter={'query': {'terms': {'_id': asset_ids}}},
                                       base_url='api/v3/assets',
                                       serializer_class=SearchAssetSerializer,
                                       item_modifier=search_asset_modifier,
                                       pagination_class=ZMLPFromSizePagination)

    @action(detail=True, methods=['get'])
    def script(self, request, project_pk, pk):
        """Returns the script that defines a Task."""
        script = request.client.get(f'/api/v1/tasks/{pk}/_script')
        return Response(script)

    @action(detail=True, methods=['get'])
    def errors(self, request, project_pk, pk):
        """Returns all errors for a Task."""
        return self._zmlp_list_from_search(request,
                                           item_modifier=task_error_item_modifier,
                                           search_filter={'taskIds': [pk]},
                                           serializer_class=TaskErrorViewSet.serializer_class,
                                           base_url=TaskErrorViewSet.zmlp_root_api_path)

    @action(detail=True, methods=['get'])
    def logs(self, request, project_pk, pk):
        """Streams the logs for a task."""
        path = os.path.join(self.zmlp_root_api_path, pk, '_log')
        return StreamingHttpResponse(self.stream_zmlp_endpoint(path),
                                     content_type='text/plain')
