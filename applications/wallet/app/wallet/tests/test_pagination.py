import json
from rest_framework.request import Request

from wallet.paginators import ZMLPFromSizePagination


def test_prep_and_paginate_an_api_response(api_factory):
    request = Request(api_factory.get('api/v1/projects/cf8f0b13-37bf-411f-b78e-a9d05513cbc9/jobs/?from=0&size=1'))  # noqa
    content = """{"list":[{"id":"82d5308b-67c2-1433-8fef-0a580a000955","organizationId":"00000000-9998-8888-7777-666666666666","name":"test-whitespace.json","type":"Import","state":"Active","assetCounts":{"assetCreatedCount":0,"assetReplacedCount":0,"assetWarningCount":0,"assetErrorCount":4},"taskCounts":{"tasksTotal":1,"tasksWaiting":0,"tasksRunning":0,"tasksSuccess":0,"tasksFailure":1,"tasksSkipped":0,"tasksQueued":0},"createdUser":{"id":"00000000-7b0b-480e-8c36-f06f04aed2f1","username":"admin","email":"admin@zorroa.com","permissionId":"00000000-fc08-4e4a-aa7a-a183f42c9fa0","homeFolderId":"00000000-2395-4e71-9e4c-dacceef6ad53","organizationId":"00000000-9998-8888-7777-666666666666"},"timeStarted":1573090717162,"timeUpdated":1573090716083,"timeCreated":1573090716083,"priority":100,"paused":false,"timePauseExpired":-1,"maxRunningTasks":1024,"jobId":"82d5308b-67c2-1433-8fef-0a580a000955"}],"page":{"from":0,"size":1,"totalCount":10}}"""  # noqa
    content = json.loads(content)
    paginator = ZMLPFromSizePagination()
    paginator.prep_pagination_for_api_response(content, request)
    response = paginator.get_paginated_response(content['list'])
    assert len(response.data['results']) == 1
    assert response.data['previous'] is None
    assert '?from=1&size=1' in response.data['next']
    assert response.data['count'] == 10


def test_prep_and_paginate_an_api_response_default_size(api_factory):
    request = Request(api_factory.get('api/v1/projects/cf8f0b13-37bf-411f-b78e-a9d05513cbc9/jobs/?from=0'))  # noqa
    content = """{"list":[{"id":"82d5308b-67c2-1433-8fef-0a580a000955","organizationId":"00000000-9998-8888-7777-666666666666","name":"test-whitespace.json","type":"Import","state":"Active","assetCounts":{"assetCreatedCount":0,"assetReplacedCount":0,"assetWarningCount":0,"assetErrorCount":4},"taskCounts":{"tasksTotal":1,"tasksWaiting":0,"tasksRunning":0,"tasksSuccess":0,"tasksFailure":1,"tasksSkipped":0,"tasksQueued":0},"createdUser":{"id":"00000000-7b0b-480e-8c36-f06f04aed2f1","username":"admin","email":"admin@zorroa.com","permissionId":"00000000-fc08-4e4a-aa7a-a183f42c9fa0","homeFolderId":"00000000-2395-4e71-9e4c-dacceef6ad53","organizationId":"00000000-9998-8888-7777-666666666666"},"timeStarted":1573090717162,"timeUpdated":1573090716083,"timeCreated":1573090716083,"priority":100,"paused":false,"timePauseExpired":-1,"maxRunningTasks":1024,"jobId":"82d5308b-67c2-1433-8fef-0a580a000955"}],"page":{"from":0,"size":1,"totalCount":30}}"""  # noqa
    content = json.loads(content)
    paginator = ZMLPFromSizePagination()
    paginator.prep_pagination_for_api_response(content, request)
    response = paginator.get_paginated_response(content['list'])
    assert len(response.data['results']) == 1
    assert response.data['previous'] is None
    assert '?from=20&size=20' in response.data['next']
    assert response.data['count'] == 30
