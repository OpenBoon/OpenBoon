from rest_framework import status
from rest_framework.response import Response
from rest_framework.decorators import action

from projects.views import BaseProjectViewSet


class JobsViewSet(BaseProjectViewSet):
    def list(self, request, project_pk, client):
        current_url = request.build_absolute_uri(request.get_full_path())
        payload = {'page': {'from': request.GET.get('from', 0),
                            'size': request.GET.get('size', 25)}}
        # TODO: Need to handle when the server is unreachable, for all these
        response = client.post('/api/v1/jobs/_search', payload)
        content = response.json()
        for item in content['list']:
            item['url'] = f'{current_url}{item["id"]}/'
        return Response(content)

    def retrieve(self, request, project_pk, client, pk):
        response = client.get(f'/api/v1/jobs/{pk}')
        return Response(response.json())

    @action(detail=True, methods=['get'])
    def errors(self, request, project_pk, client, pk):
        request_body = {
            'jobIds': [pk],
        }
        response = client.post(f'/api/v1/taskerrors/_search', request_body)
        return Response(response.json())

    @action(detail=True, methods=['put'])
    def pause(self, request, project_pk, client, pk):
        new_values = {'paused': True}
        request_body = self._get_updated_info(client, pk, new_values)
        response = client.put(f'/api/v1/jobs/{pk}', request_body)
        return Response(response.json())

    @action(detail=True, methods=['put'])
    def resume(self, request, project_pk, client, pk):
        new_values = {'paused': False}
        request_body = self._get_updated_info(client, pk, new_values)
        response = client.put(f'/api/v1/jobs/{pk}', request_body)
        return Response(response.json())

    @action(detail=True, methods=['put'])
    def cancel(self, request, project_pk, client, pk):
        response = client.put(f'/api/v1/jobs/{pk}/_cancel')
        return Response(response.json())

    @action(detail=True, methods=['put'])
    def restart(self, request, project_pk, client, pk):
        response = client.put(f'/api/v1/jobs/{pk}/_restart')
        return Response(response.json())

    @action(detail=True, methods=['put'])
    def priority(self, request, project_pk, client, pk):
        priority = request.data.get('priority', None)
        if priority is None:
            msg = 'Unable to find a valid `priority` value to use.'
            return Response({'msg': msg}, status.HTTP_400_BAD_REQUEST)
        new_values = {'priority': priority}
        request_body = self._get_updated_info(client, pk, new_values)
        response = client.put(f'/api/v1/jobs/{pk}', request_body)
        return Response(response.json())

    @action(detail=True, methods=['put'])
    def max_running_tasks(self, request, project_pk, client, pk):
        max_running_tasks = request.data.get('max_running_tasks', None)
        if max_running_tasks is None:
            msg = 'Unable to find a valid `max_running_tasks` value to use.'
            return Response({'msg': msg}, status.HTTP_400_BAD_REQUEST)
        new_values = {'max_running_tasks': max_running_tasks}
        request_body = self._get_updated_info(client, pk, new_values)
        response = client.put(f'/api/v1/jobs/{pk}', request_body)
        return Response(response.json())

    @action(detail=True, methods=['put'])
    def retry_all_failures(self, request, project_pk, client, pk):
        response = client.put(f'/api/v1/jobs/{pk}/_retryAllFailures')
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
        job_spec = {
            'name': response['name'],
            'priority': response['priority'],
            'paused': response['paused'],
            'timePauseExpired': response['timePauseExpired']
        }
        job_spec.update(new_values)
        return job_spec

