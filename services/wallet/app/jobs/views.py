from rest_framework.response import Response

from projects.views import BaseProjectViewSet


class JobsViewSet(BaseProjectViewSet):
    def list(self, request, project_pk, client):
        payload = {}
        current_url = request.build_absolute_uri(request.get_full_path())
        if 'from' in request.GET and 'size' in request.GET:
            payload['page'] = {'from': request.GET['from'], 'size': request.GET['size']}
        response = client.post('/api/v1/jobs/_search', payload)
        content = response.json()
        for item in content['list']:
            item['url'] = f'{current_url}{item["id"]}/'
        return Response(content)

    def retrieve(self, request, project_pk, client, pk):
        response = client.get(f'/api/v1/jobs/{pk}')
        return Response(response.json())
