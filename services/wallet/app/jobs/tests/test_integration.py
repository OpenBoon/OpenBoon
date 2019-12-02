import pytest
from django.urls import reverse
from requests import Response
from pixml import PixmlClient

pytestmark = pytest.mark.django_db


class TestJobViewSet:

    def test_get_list(self, user, project, pixml_project_membership, api_client, monkeypatch):
        def mock_api_response(request, project, client):
            response = Response()
            response._content = b'{"list": [{"id": "82d53089-67c2-1433-8fef-0a580a000955", "organizationId": "00000000-9998-8888-7777-666666666666", "name": "test-whitespace.json", "type": "Import", "state": "Active", "assetCounts": {"assetCreatedCount": 0, "assetReplacedCount": 0, "assetWarningCount": 0, "assetErrorCount": 4}, "taskCounts": {"tasksTotal": 1, "tasksWaiting": 0, "tasksRunning": 0, "tasksSuccess": 0, "tasksFailure": 1, "tasksSkipped": 0, "tasksQueued": 0}, "createdUser": {"id": "00000000-7b0b-480e-8c36-f06f04aed2f1", "username": "admin", "email": "admin@zorroa.com", "permissionId": "00000000-fc08-4e4a-aa7a-a183f42c9fa0", "homeFolderId": "00000000-2395-4e71-9e4c-dacceef6ad53", "organizationId": "00000000-9998-8888-7777-666666666666"}, "timeStarted": 1573090540886, "timeUpdated": 1573090536003, "timeCreated": 1573090536003, "priority": 100, "paused": false, "timePauseExpired": -1, "maxRunningTasks": 1024, "jobId": "82d53089-67c2-1433-8fef-0a580a000955"}], "page": {"from": 0, "size": 10, "totalCount": 1}}'  # noqa
            return response

        monkeypatch.setattr(PixmlClient, 'post', mock_api_response)
        api_client.force_authenticate(user)
        api_client.force_login(user)
        response = api_client.get(reverse('job-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        content = response.json()
        assert len(content['list']) == 1

    def test_get_detail(self, user, project, pixml_project_membership, api_client, monkeypatch):
        def mock_api_response(*args, **kwargs):
            response = Response()
            response._content = b'{"id":"b8ec649d-67bc-1ab4-a0ae-0242ac120007","organizationId":"00000000-9998-8888-7777-666666666666","name":"import-test-data-all.json","type":"Import","state":"Finished","assetCounts":{"assetCreatedCount":246,"assetReplacedCount":54,"assetWarningCount":0,"assetErrorCount":1},"taskCounts":{"tasksTotal":8,"tasksWaiting":0,"tasksRunning":0,"tasksSuccess":8,"tasksFailure":0,"tasksSkipped":0,"tasksQueued":0},"createdUser":{"id":"00000000-7b0b-480e-8c36-f06f04aed2f1","username":"admin","email":"admin@zorroa.com","permissionId":"00000000-fc08-4e4a-aa7a-a183f42c9fa0","homeFolderId":"00000000-2395-4e71-9e4c-dacceef6ad53","organizationId":"00000000-9998-8888-7777-666666666666"},"timeStarted":1574891251035,"timeUpdated":1574891738399,"timeCreated":1574891249308,"priority":100,"paused":false,"timePauseExpired":-1,"maxRunningTasks":1024,"jobId":"b8ec649d-67bc-1ab4-a0ae-0242ac120007"}'
            return response

        monkeypatch.setattr(PixmlClient, 'get', mock_api_response())
        api_client.force_authenticate(user)
        api_client.force_login(user)
        response = api_client.get(reverse('job-retrieve',
                                          kwargs={'project_pk': project.id,
                                                  'pk': 'b8ec649d-67bc-1ab4-a0ae-0242ac120007'}))
        assert response.status_code == 200
        content = response.json()
        assert True
