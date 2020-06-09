import pytest
from django.test import override_settings
from django.urls import reverse
from requests import Response
from zmlp import ZmlpClient

from jobs.views import JobViewSet
from projects.clients import ZviClient

pytestmark = pytest.mark.django_db


@pytest.fixture
def job_pk():
    return 'b8ec649d-67bc-1ab4-a0ae-0242ac120007'


class TestJobViewSet:

    @override_settings(PLATFORM='zvi')
    def test_get_list_zvi(self, zvi_project_user, project, api_client, monkeypatch):

        def mock_api_response(*args, **kwargs):
            response = Response()
            response._content = b'{"list": [{"id": "82d53089-67c2-1433-8fef-0a580a000955", "organizationId": "00000000-9998-8888-7777-666666666666", "name": "test-whitespace.json", "type": "Import", "state": "Active", "assetCounts": {"assetCreatedCount": 0, "assetReplacedCount": 0, "assetWarningCount": 0, "assetErrorCount": 4}, "taskCounts": {"tasksTotal": 1, "tasksWaiting": 0, "tasksRunning": 0, "tasksSuccess": 0, "tasksFailure": 1, "tasksSkipped": 0, "tasksQueued": 0}, "createdUser": {"id": "00000000-7b0b-480e-8c36-f06f04aed2f1", "username": "admin", "email": "admin@zorroa.com", "permissionId": "00000000-fc08-4e4a-aa7a-a183f42c9fa0", "homeFolderId": "00000000-2395-4e71-9e4c-dacceef6ad53", "organizationId": "00000000-9998-8888-7777-666666666666"}, "timeStarted": 1573090540886, "timeUpdated": 1573090536003, "timeCreated": 1573090536003, "priority": 100, "paused": false, "timePauseExpired": -1, "maxRunningTasks": 1024, "jobId": "82d53089-67c2-1433-8fef-0a580a000955"}], "page": {"from": 0, "size": 10, "totalCount": 1}}'  # noqa
            return response

        monkeypatch.setattr(ZviClient, 'post', mock_api_response)
        api_client.force_authenticate(zvi_project_user)
        api_client.force_login(zvi_project_user)
        response = api_client.get(reverse('job-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        content = response.json()
        assert len(content['results']) == 1
        assert len(content['results'][0]) > 0

    def test_get_list_zmlp(self, zmlp_project_user, project, api_client, monkeypatch):

        def mock_api_response(*args, **kwargs):
            response = {"list": [{"id": "82d53089-67c2-1433-8fef-0a580a000955", "organizationId": "00000000-9998-8888-7777-666666666666", "name": "test-whitespace.json", "type": "Import", "state": "Active", "assetCounts": {"assetCreatedCount": 0, "assetReplacedCount": 0, "assetWarningCount": 0, "assetErrorCount": 4}, "taskCounts": {"tasksTotal": 1, "tasksWaiting": 0, "tasksRunning": 0, "tasksSuccess": 0, "tasksFailure": 1, "tasksSkipped": 0, "tasksQueued": 0}, "createdUser": {"id": "00000000-7b0b-480e-8c36-f06f04aed2f1", "username": "admin", "email": "admin@zorroa.com", "permissionId": "00000000-fc08-4e4a-aa7a-a183f42c9fa0", "homeFolderId": "00000000-2395-4e71-9e4c-dacceef6ad53", "organizationId": "00000000-9998-8888-7777-666666666666"}, "timeStarted": 1573090540886, "timeUpdated": 1573090536003, "timeCreated": 1573090536003, "priority": 100, "paused": False, "timePauseExpired": -1, "maxRunningTasks": 1024, "jobId": "82d53089-67c2-1433-8fef-0a580a000955"}, {"id": "82d53089-67c2-1433-8fef-0a580a000955", "organizationId": "00000000-9998-8888-7777-666666666666", "name": "test-whitespace.json", "type": "Import", "state": "Active", "assetCounts": {"assetCreatedCount": 0, "assetReplacedCount": 0, "assetWarningCount": 0, "assetErrorCount": 4, "assetTotalCount": 19}, "taskCounts": {"tasksTotal": 1, "tasksWaiting": 0, "tasksRunning": 0, "tasksSuccess": 0, "tasksFailure": 1, "tasksSkipped": 0, "tasksQueued": 0}, "createdUser": {"id": "00000000-7b0b-480e-8c36-f06f04aed2f1", "username": "admin", "email": "admin@zorroa.com", "permissionId": "00000000-fc08-4e4a-aa7a-a183f42c9fa0", "homeFolderId": "00000000-2395-4e71-9e4c-dacceef6ad53", "organizationId": "00000000-9998-8888-7777-666666666666"}, "timeStarted": 1573090540886, "timeUpdated": 1573090536003, "timeCreated": 1573090536003, "priority": 100, "paused": False, "timePauseExpired": -1, "maxRunningTasks": 1024, "jobId": "82d53089-67c2-1433-8fef-0a580a000955"}], "page": {"from": 0, "size": 50, "totalCount": 0}}  # noqa
            return response

        monkeypatch.setattr(ZmlpClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('job-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        content = response.json()
        assert len(content['results']) == 2
        assert len(content['results'][0]) > 0
        assert content['results'][0]['assetCounts']['assetTotalCount'] == 4
        assert content['results'][1]['assetCounts']['assetTotalCount'] == 19

    def test_get_detail_zmlp(self, zmlp_project_user, project, api_client, monkeypatch, job_pk):

        def mock_api_response(*args, **kwargs):
            response = {'id': 'b8ec649d-67bc-1ab4-a0ae-0242ac120007',
                        'organizationId': '00000000-9998-8888-7777-666666666666',
                        'name': 'import-test-data-all.json', 'type': 'Import',
                        'state': 'Finished', 'assetCounts': {'assetCreatedCount': 246,
                                                             'assetReplacedCount': 54,
                                                             'assetWarningCount': 0,
                                                             'assetErrorCount': 1},
                        'taskCounts': {'tasksTotal': 8, 'tasksWaiting': 0,
                                       'tasksRunning': 0, 'tasksSuccess': 8,
                                       'tasksFailure': 0, 'tasksSkipped': 0,
                                       'tasksQueued': 0},
                        'createdUser': {'id': '00000000-7b0b-480e-8c36-f06f04aed2f1',
                                        'username': 'admin', 'email': 'admin@zorroa.com',
                                        'permissionId': '00000000-fc08-4e4a-aa7a-a183f42c9fa0',
                                        'homeFolderId': '00000000-2395-4e71-9e4c-dacceef6ad53',
                                        'organizationId': '00000000-9998-8888-7777-666666666666'},
                        'timeStarted': 1574891251035, 'timeUpdated': 1574891738399,
                        'timeCreated': 1574891249308, 'priority': 100, 'paused': False,
                        'timePauseExpired': -1, 'maxRunningTasks': 1024,
                        'jobId': 'b8ec649d-67bc-1ab4-a0ae-0242ac120007'}  # noqa
            return response

        monkeypatch.setattr(ZmlpClient, 'get', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('job-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': job_pk}))
        assert response.status_code == 200
        content = response.json()
        assert content['id'] == 'b8ec649d-67bc-1ab4-a0ae-0242ac120007'
        assert content['assetCounts']['assetTotalCount'] == 301

    def test_get_detail_zvi_response(self, zmlp_project_user, project, api_client,
                                     monkeypatch, job_pk):

        def mock_api_response(*args, **kwargs):
            response = Response()
            response._content = b'{"id":"b8ec649d-67bc-1ab4-a0ae-0242ac120007","organizationId":"00000000-9998-8888-7777-666666666666","name":"import-test-data-all.json","type":"Import","state":"Finished","assetCounts":{"assetCreatedCount":246,"assetReplacedCount":54,"assetWarningCount":0,"assetErrorCount":1},"taskCounts":{"tasksTotal":8,"tasksWaiting":0,"tasksRunning":0,"tasksSuccess":8,"tasksFailure":0,"tasksSkipped":0,"tasksQueued":0},"createdUser":{"id":"00000000-7b0b-480e-8c36-f06f04aed2f1","username":"admin","email":"admin@zorroa.com","permissionId":"00000000-fc08-4e4a-aa7a-a183f42c9fa0","homeFolderId":"00000000-2395-4e71-9e4c-dacceef6ad53","organizationId":"00000000-9998-8888-7777-666666666666"},"timeStarted":1574891251035,"timeUpdated":1574891738399,"timeCreated":1574891249308,"priority":100,"paused":false,"timePauseExpired":-1,"maxRunningTasks":1024,"jobId":"b8ec649d-67bc-1ab4-a0ae-0242ac120007"}'  # noqa
            return response

        monkeypatch.setattr(ZmlpClient, 'get', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('job-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': job_pk}))
        assert response.status_code == 200
        content = response.json()
        assert content['id'] == 'b8ec649d-67bc-1ab4-a0ae-0242ac120007'

    def test_get_detail_actions(self, zmlp_project_user, project, api_client, monkeypatch, job_pk):

        def mock_api_response(*args, **kwargs):
            response = Response()
            response._content = b'{"id":"b8ec649d-67bc-1ab4-a0ae-0242ac120007","organizationId":"00000000-9998-8888-7777-666666666666","name":"import-test-data-all.json","type":"Import","state":"Finished","assetCounts":{"assetCreatedCount":246,"assetReplacedCount":54,"assetWarningCount":0,"assetErrorCount":1},"taskCounts":{"tasksTotal":8,"tasksWaiting":0,"tasksRunning":0,"tasksSuccess":8,"tasksFailure":0,"tasksSkipped":0,"tasksQueued":0},"createdUser":{"id":"00000000-7b0b-480e-8c36-f06f04aed2f1","username":"admin","email":"admin@zorroa.com","permissionId":"00000000-fc08-4e4a-aa7a-a183f42c9fa0","homeFolderId":"00000000-2395-4e71-9e4c-dacceef6ad53","organizationId":"00000000-9998-8888-7777-666666666666"},"timeStarted":1574891251035,"timeUpdated":1574891738399,"timeCreated":1574891249308,"priority":100,"paused":false,"timePauseExpired":-1,"maxRunningTasks":1024,"jobId":"b8ec649d-67bc-1ab4-a0ae-0242ac120007"}'  # noqa
            return response

        monkeypatch.setattr(ZmlpClient, 'get', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('job-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': job_pk}))
        content = response.json()
        assert 'actions' in content
        uri = reverse('job-detail', kwargs={'project_pk': project.id, 'pk': job_pk})
        resume_url = f'{uri}resume/'
        assert content['actions']['resume'].endswith(resume_url)

    def test_get_errors(self, zmlp_project_user, project, api_client, monkeypatch, job_pk):

        def mock_api_response(*args, **kwargs):
            assert args[1] == '/api/v1/taskerrors/_search'
            response = Response()
            response._content = b'{"list": [{"id": "2b9619f9-6943-1ab4-8b95-0242ac120007", "taskId": "d4752304-68f5-1ab4-976d-0242ac120007", "jobId": "b8ec649d-67bc-1ab4-a0ae-0242ac120007", "assetId": "1b2f7537-3bcc-50b6-bd88-77f269b2394b", "path": "/zorroa-test-data/video/Search.mp4", "message": "CalledProcessError: Command \'[\'ffprobe\', \'-v\', \'quiet\', \'-print_format\', \'json\', \'-show_streams\', \'-show_format\', \'/zorroa-test-data/video/Search.mp4\']\' returned non-zero exit status 1.", "processor": "VideoImporter", "fatal": false, "analyst": "https://2d068888aadd:5000", "phase": "execute", "timeCreated": 1574891430542, "stackTrace": [{"file": "/opt/app-root/lib/python2.7/site-packages/zsdk/zpsgo/executor.py", "lineNumber": 284, "className": "process", "methodName": "proc.process(frame)"}, {"file": "/opt/app-root/lib/python2.7/site-packages/zsdk/processor.py", "lineNumber": 746, "className": "process", "methodName": "self._process(frame)"}, {"file": "/opt/app-root/src/analyst/pylib/zplugins/video/importers.py", "lineNumber": 73, "className": "_process", "methodName": "self._set_media_metadata(asset)"}, {"file": "/opt/app-root/src/analyst/pylib/zplugins/video/importers.py", "lineNumber": 92, "className": "_set_media_metadata", "methodName": "ffprobe_info = ffprobe(path)"}, {"file": "/opt/app-root/src/analyst/pylib/zplugins/util/media.py", "lineNumber": 158, "className": "ffprobe", "methodName": "ffprobe_result = check_output(cmd, shell=False)"}, {"file": "/opt/app-root/lib/python2.7/site-packages/subprocess32.py", "lineNumber": 343, "className": "check_output", "methodName": "raise CalledProcessError(retcode, process.args, output=output)"}]}], "page": {"from": 0, "size": 50, "totalCount": 1}}'  # noqa
            return response

        monkeypatch.setattr(ZmlpClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('job-errors',
                                          kwargs={'project_pk': project.id, 'pk': job_pk}))
        assert response.status_code == 200
        content = response.json()
        assert len(content['results']) == 1

    def test_put_pause(self, zmlp_project_user, project, api_client, monkeypatch, job_pk):
        updated_info_return = {
            'name': 'Test',
            'priority': 100,
            'paused': True,
            'timePauseExpired': -1
        }

        def get_updated_info_mock(*args, **kwargs):
            body = args[-1]
            assert body == {'paused': True}
            return updated_info_return

        def mock_api_response(*args, **kawargs):
            body = args[-1]
            assert body == updated_info_return
            response = Response()
            response._content = b'{"id": "b8ec649d-67bc-1ab4-a0ae-0242ac120007", "organizationId": "00000000-9998-8888-7777-666666666666", "name": "import-test-data-all.json", "type": "Import", "state": "Finished", "assetCounts": {"assetCreatedCount": 246, "assetReplacedCount": 54, "assetWarningCount": 0, "assetErrorCount": 1}, "taskCounts": {"tasksTotal": 8, "tasksWaiting": 0, "tasksRunning": 0, "tasksSuccess": 8, "tasksFailure": 0, "tasksSkipped": 0, "tasksQueued": 0}, "createdUser": {"id": "00000000-7b0b-480e-8c36-f06f04aed2f1", "username": "admin", "email": "admin@zorroa.com", "permissionId": "00000000-fc08-4e4a-aa7a-a183f42c9fa0", "homeFolderId": "00000000-2395-4e71-9e4c-dacceef6ad53", "organizationId": "00000000-9998-8888-7777-666666666666"}, "timeStarted": 1574891251035, "timeUpdated": 1574891738399, "timeCreated": 1574891249308, "priority": 100, "paused": true, "timePauseExpired": -1, "maxRunningTasks": 0, "jobId": "b8ec649d-67bc-1ab4-a0ae-0242ac120007"}'  # noqa
            return response

        monkeypatch.setattr(JobViewSet, '_get_updated_info', get_updated_info_mock)
        monkeypatch.setattr(ZmlpClient, 'put', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.put(reverse('job-pause',
                                          kwargs={'project_pk': project.id, 'pk': job_pk}))
        assert response.status_code == 200
        content = response.json()
        assert content['paused'] is True

    def test_put_resume(self, zmlp_project_user, project, api_client, monkeypatch, job_pk):
        updated_info_return = {
            'name': 'Test',
            'priority': 100,
            'paused': False,
            'timePauseExpired': -1
        }

        def get_updated_info_mock(*args, **kwargs):
            body = args[-1]
            assert body == {'paused': False}
            return updated_info_return

        def mock_api_response(*args, **kawargs):
            body = args[-1]
            assert body == updated_info_return
            response = Response()
            response._content = b'{"id": "b8ec649d-67bc-1ab4-a0ae-0242ac120007", "organizationId": "00000000-9998-8888-7777-666666666666", "name": "import-test-data-all.json", "type": "Import", "state": "Finished", "assetCounts": {"assetCreatedCount": 246, "assetReplacedCount": 54, "assetWarningCount": 0, "assetErrorCount": 1}, "taskCounts": {"tasksTotal": 8, "tasksWaiting": 0, "tasksRunning": 0, "tasksSuccess": 8, "tasksFailure": 0, "tasksSkipped": 0, "tasksQueued": 0}, "createdUser": {"id": "00000000-7b0b-480e-8c36-f06f04aed2f1", "username": "admin", "email": "admin@zorroa.com", "permissionId": "00000000-fc08-4e4a-aa7a-a183f42c9fa0", "homeFolderId": "00000000-2395-4e71-9e4c-dacceef6ad53", "organizationId": "00000000-9998-8888-7777-666666666666"}, "timeStarted": 1574891251035, "timeUpdated": 1574891738399, "timeCreated": 1574891249308, "priority": 100, "paused": false, "timePauseExpired": -1, "maxRunningTasks": 0, "jobId": "b8ec649d-67bc-1ab4-a0ae-0242ac120007"}'  # noqa
            return response

        monkeypatch.setattr(JobViewSet, '_get_updated_info', get_updated_info_mock)
        monkeypatch.setattr(ZmlpClient, 'put', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.put(reverse('job-resume',
                                          kwargs={'project_pk': project.id, 'pk': job_pk}))
        assert response.status_code == 200
        content = response.json()
        assert content['paused'] is False

    def test_put_cancel(self, zmlp_project_user, project, api_client, monkeypatch, job_pk):

        def mock_api_response(*args, **kwargs):
            response = Response()
            response._content = b'{"type":"Job","id":"b8ec649d-67bc-1ab4-a0ae-0242ac120007","op":"cancel","success":false}'  # noqa
            return response

        monkeypatch.setattr(ZmlpClient, 'put', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.put(reverse('job-cancel',
                                          kwargs={'project_pk': project.id, 'pk': job_pk}))
        assert response.status_code == 200
        content = response.json()
        assert content['op'] == 'cancel'

    def test_put_restart(self, zmlp_project_user, project, api_client, monkeypatch, job_pk):

        def mock_api_response(*args, **kwargs):
            response = Response()
            response._content = b'{"type":"Job","id":"b8ec649d-67bc-1ab4-a0ae-0242ac120007","op":"restart","success":true}'  # noqa
            return response

        monkeypatch.setattr(ZmlpClient, 'put', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.put(reverse('job-restart',
                                          kwargs={'project_pk': project.id, 'pk': job_pk}))
        assert response.status_code == 200
        content = response.json()
        assert content['op'] == 'restart'

    def test_put_priority_no_body(self, zmlp_project_user, project, api_client, job_pk):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.put(reverse('job-priority',
                                          kwargs={'project_pk': project.id, 'pk': job_pk}))
        assert response.status_code == 400
        assert response.json()['msg'] == 'Unable to find a valid `priority` value to use.'

    def test_put_priority_bad_body(self, zmlp_project_user, project, api_client, job_pk):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.put(reverse('job-priority',
                                          kwargs={'project_pk': project.id, 'pk': job_pk}),
                                  {'priority': 'asdf'})
        assert response.status_code == 400
        assert response.json()['msg'] == 'Invalid `priority` value provided. Expected an integer.'

    def test_put_priority(self, zmlp_project_user, project, api_client, monkeypatch, job_pk):
        updated_info_return = {
            'name': 'Test',
            'priority': 12,
            'paused': False,
            'timePauseExpired': -1
        }

        def get_updated_info_mock(*args, **kwargs):
            body = args[-1]
            assert body['priority'] == 12
            return updated_info_return

        def mock_api_response(*args, **kwargs):
            body = args[-1]
            assert body['priority'] == 12
            response = Response()
            response._content = b'{"id":"b8ec649d-67bc-1ab4-a0ae-0242ac120007","organizationId":"00000000-9998-8888-7777-666666666666","name":"import-test-data-all.json","type":"Import","state":"Active","assetCounts":{"assetCreatedCount":246,"assetReplacedCount":54,"assetWarningCount":0,"assetErrorCount":1},"taskCounts":{"tasksTotal":8,"tasksWaiting":0,"tasksRunning":0,"tasksSuccess":8,"tasksFailure":0,"tasksSkipped":0,"tasksQueued":0},"createdUser":{"id":"00000000-7b0b-480e-8c36-f06f04aed2f1","username":"admin","email":"admin@zorroa.com","permissionId":"00000000-fc08-4e4a-aa7a-a183f42c9fa0","homeFolderId":"00000000-2395-4e71-9e4c-dacceef6ad53","organizationId":"00000000-9998-8888-7777-666666666666"},"timeStarted":1574891251035,"timeUpdated":1575398506659,"timeCreated":1574891249308,"priority":12,"paused":true,"timePauseExpired":-1,"maxRunningTasks":0,"jobId":"b8ec649d-67bc-1ab4-a0ae-0242ac120007"}'  # noqa
            return response

        monkeypatch.setattr(JobViewSet, '_get_updated_info', get_updated_info_mock)
        monkeypatch.setattr(ZmlpClient, 'put', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.put(reverse('job-priority',
                                          kwargs={'project_pk': project.id, 'pk': job_pk}),
                                  {'priority': '12'})
        assert response.status_code == 200
        content = response.json()
        assert content['priority'] == 12

    def test_put_max_running_tasks_no_body(self, zmlp_project_user, project, api_client, job_pk):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.put(reverse('job-max-running-tasks',
                                          kwargs={'project_pk': project.id, 'pk': job_pk}))
        assert response.status_code == 400
        assert response.json()['msg'] == 'Unable to find a valid `max_running_tasks` value to use.'

    def test_put_max_running_tasks_bad_body(self, zmlp_project_user, project, api_client, job_pk):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.put(reverse('job-max-running-tasks',
                                          kwargs={'project_pk': project.id, 'pk': job_pk}),
                                  {'max_running_tasks': 'asdf'})
        assert response.status_code == 400
        assert response.json()['msg'] == ('Invalid `max_running_tasks` value provided. '
                                          'Expected an integer.')

    def test_put_max_running_tasks(self, zmlp_project_user, project, api_client,
                                   monkeypatch, job_pk):
        updated_info_return = {
            'name': 'Test',
            'priority': 0,
            'paused': False,
            'timePauseExpired': -1,
            'maxRunningTasks': 10
        }

        def get_updated_info_mock(*args, **kwargs):
            body = args[-1]
            assert body['maxRunningTasks'] == 10
            return updated_info_return

        def mock_api_response(*args, **kwargs):
            body = args[-1]
            assert body['maxRunningTasks'] == 10
            response = Response()
            response._content = b'{"id":"b8ec649d-67bc-1ab4-a0ae-0242ac120007","organizationId":"00000000-9998-8888-7777-666666666666","name":"import-test-data-all.json","type":"Import","state":"Active","assetCounts":{"assetCreatedCount":246,"assetReplacedCount":54,"assetWarningCount":0,"assetErrorCount":1},"taskCounts":{"tasksTotal":8,"tasksWaiting":0,"tasksRunning":0,"tasksSuccess":8,"tasksFailure":0,"tasksSkipped":0,"tasksQueued":0},"createdUser":{"id":"00000000-7b0b-480e-8c36-f06f04aed2f1","username":"admin","email":"admin@zorroa.com","permissionId":"00000000-fc08-4e4a-aa7a-a183f42c9fa0","homeFolderId":"00000000-2395-4e71-9e4c-dacceef6ad53","organizationId":"00000000-9998-8888-7777-666666666666"},"timeStarted":1574891251035,"timeUpdated":1575405463175,"timeCreated":1574891249308,"priority":12,"paused":false,"timePauseExpired":-1,"maxRunningTasks":10,"jobId":"b8ec649d-67bc-1ab4-a0ae-0242ac120007"}'  # noqa
            return response

        monkeypatch.setattr(JobViewSet, '_get_updated_info', get_updated_info_mock)
        monkeypatch.setattr(ZmlpClient, 'put', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.put(reverse('job-max-running-tasks',
                                          kwargs={'project_pk': project.id,
                                                  'pk': job_pk}),
                                  {'max_running_tasks': '10'})
        assert response.status_code == 200
        content = response.json()
        assert content['maxRunningTasks'] == 10

    def test_put_retry_all_failures(self, zmlp_project_user, project, api_client,
                                    monkeypatch, job_pk):

        def mock_api_response(*args, **kwargs):
            response = Response()
            response._content = b'{"type":"Job","id":"b8ec649d-67bc-1ab4-a0ae-0242ac120007","op":"retryAllFailures","success":false}'  # noqa
            return response

        monkeypatch.setattr(ZmlpClient, 'put', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.put(reverse('job-retry-all-failures',
                                          kwargs={'project_pk': project.id,
                                                  'pk': job_pk}))
        assert response.status_code == 200
        content = response.json()
        assert content['op'] == 'retryAllFailures'

    def test_get_updated_info(self, api_client, monkeypatch, job_pk):

        def mock_api_response(*args, **kwargs):
            response = Response()
            response._content = b'{"id":"b8ec649d-67bc-1ab4-a0ae-0242ac120007","organizationId":"00000000-9998-8888-7777-666666666666","name":"import-test-data-all.json","type":"Import","state":"Active","assetCounts":{"assetCreatedCount":246,"assetReplacedCount":54,"assetWarningCount":0,"assetErrorCount":1},"taskCounts":{"tasksTotal":8,"tasksWaiting":0,"tasksRunning":0,"tasksSuccess":8,"tasksFailure":0,"tasksSkipped":0,"tasksQueued":0},"createdUser":{"id":"00000000-7b0b-480e-8c36-f06f04aed2f1","username":"admin","email":"admin@zorroa.com","permissionId":"00000000-fc08-4e4a-aa7a-a183f42c9fa0","homeFolderId":"00000000-2395-4e71-9e4c-dacceef6ad53","organizationId":"00000000-9998-8888-7777-666666666666"},"timeStarted":1574891251035,"timeUpdated":1575405463175,"timeCreated":1574891249308,"priority":12,"paused":false,"timePauseExpired":-1,"maxRunningTasks":10,"jobId":"b8ec649d-67bc-1ab4-a0ae-0242ac120007"}'  # noqa
            return response

        monkeypatch.setattr(api_client, 'get', mock_api_response)
        new_values = {
            'name': 'Test',
            'priority': 5,
            'paused': True,
            'arbitraryKey': 'NewValue',
            'maxRunningTasks': 10
        }
        viewset = JobViewSet()
        new_job_spec = viewset._get_updated_info(api_client, job_pk, new_values)
        assert new_job_spec['name'] == new_values['name']
        assert new_job_spec['priority'] == new_values['priority']
        assert new_job_spec['paused'] == new_values['paused']
        assert new_job_spec['arbitraryKey'] == new_values['arbitraryKey']
        assert new_job_spec['maxRunningTasks'] == new_values['maxRunningTasks']


class TestTaskViewSet:
    def test_retry(self, monkeypatch, api_client, zmlp_project_user, project):
        def mock_put_response(*args, **kwargs):
            return {'type': 'Task', 'id': '59527630-57f2-11ea-b3c8-0242ac120004', 'op': 'retry', 'success': True}  # noqa

        monkeypatch.setattr(ZmlpClient, 'put', mock_put_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.put(reverse('task-retry', kwargs={'project_pk': project.id, 'pk': 1}))
        assert response.status_code == 200
        assert response.json()['detail'] == 'Task 1 has been successfully retried.'

    def test_retry_failure(self, monkeypatch, api_client, zmlp_project_user, project):
        def mock_put_response(*args, **kwargs):
            return {'type': 'Task', 'id': '59527630-57f2-11ea-b3c8-0242ac120004', 'op': 'retry', 'success': False}  # noqa

        monkeypatch.setattr(ZmlpClient, 'put', mock_put_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.put(reverse('task-retry', kwargs={'project_pk': project.id, 'pk': 1}))
        assert response.status_code == 500

    def test_list(self, monkeypatch, api_client, zmlp_project_user, project):
        def mock_post_response(*args, **kwargs):
            return {'list': [{'id': '59527630-57f2-11ea-b3c8-0242ac120004', 'jobId': '5950534f-57f2-11ea-b3c8-0242ac120004', 'projectId': 'f7411da2-6573-4b1a-8e18-15af9bded45b', 'dataSourceId': '593689be-57f2-11ea-b3c8-0242ac120004', 'name': "Crawling files in 'gs://zorroa-dev-data'", 'state': 'Success', 'host': 'http://0945d0cfea37:5000', 'timeStarted': 1582650906688, 'timeStopped': 1582650915962, 'timeCreated': 1582650898050, 'timePing': 1582650898050, 'assetCounts': {'assetCreatedCount': 7, 'assetReplacedCount': 0, 'assetWarningCount': 0, 'assetErrorCount': 0, 'assetTotalCount': 0}, 'taskId': '59527630-57f2-11ea-b3c8-0242ac120004'}, {'id': '63bf1241-57f2-11ea-b3c8-0242ac120004', 'jobId': '5950534f-57f2-11ea-b3c8-0242ac120004', 'projectId': 'f7411da2-6573-4b1a-8e18-15af9bded45b', 'dataSourceId': '593689be-57f2-11ea-b3c8-0242ac120004', 'name': 'Expand with 7 assets, 8 processors.', 'state': 'Success', 'host': 'http://0945d0cfea37:5000', 'timeStarted': 1582650916053, 'timeStopped': 1582650958777, 'timeCreated': 1582650915539, 'timePing': 1582650930794, 'assetCounts': {'assetCreatedCount': 0, 'assetReplacedCount': 7, 'assetWarningCount': 0, 'assetErrorCount': 0, 'assetTotalCount': 7}, 'taskId': '63bf1241-57f2-11ea-b3c8-0242ac120004'}], 'page': {'from': 0, 'size': 50, 'disabled': False, 'totalCount': 2}}  # noqa

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('task-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        _json = response.json()
        assert _json['count'] == 2
        assert _json['results'][0]['actions']['retry'] == 'http://testserver/api/v1/projects/6abc33f0-4acf-4196-95ff-4cbb7f640a06/tasks/59527630-57f2-11ea-b3c8-0242ac120004/retry/'  # noqa

    def test_retrieve(self, monkeypatch, api_client, zmlp_project_user, project):
        def mock_get_response(*args, **kwargs):
            return {'id': '59527630-57f2-11ea-b3c8-0242ac120004', 'jobId': '5950534f-57f2-11ea-b3c8-0242ac120004', 'projectId': 'f7411da2-6573-4b1a-8e18-15af9bded45b', 'dataSourceId': '593689be-57f2-11ea-b3c8-0242ac120004', 'name': "Crawling files in 'gs://zorroa-dev-data'", 'state': 'Success', 'host': 'http://0945d0cfea37:5000', 'timeStarted': 1582652666857, 'timeStopped': 1582652672906, 'timeCreated': 1582650898050, 'timePing': 1582650898050, 'assetCounts': {'assetCreatedCount': 0, 'assetReplacedCount': 0, 'assetWarningCount': 0, 'assetErrorCount': 0, 'assetTotalCount': 0}, 'taskId': '59527630-57f2-11ea-b3c8-0242ac120004'}  # noqa

        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('task-detail', kwargs={'project_pk': project.id, 'pk': '59527630-57f2-11ea-b3c8-0242ac120004'}))  # noqa
        assert response.status_code == 200
        assert response.json()['state'] == 'Success'

    def test_assets(self, monkeypatch, api_client, zmlp_project_user, project, login):
        def mock_post_response(*args, **kwargs):
            return {'took': 10, 'timed_out': False, '_shards': {'total': 2, 'successful': 2, 'skipped': 0, 'failed': 0}, 'hits': {'total': {'value': 357, 'relation': 'eq'}, 'max_score': 0.0, 'hits': [{'_index': 'imx29yosuvoa7grd', '_type': '_doc', '_id': 'JY4HNCtoOpEH0q7pfh3snWSBH6bm-8WK', '_score': 0.0, '_source': {'system': {'jobId': '13b61d1e-5f71-12a6-9398-7eb3d1ce02c4', 'dataSourceId': 'ba231dbf-dd21-12aa-9c3a-765f5c657424', 'timeCreated': '2020-06-02T18:00:01.381568Z', 'state': 'Analyzed', 'projectId': '6a72b6c1-aa96-415b-8c2d-df355ba9c8f5', 'taskId': '13b61d1f-5f71-12a6-9398-7eb3d1ce02c4', 'timeModified': '2020-06-02T18:24:58.173011Z'}, 'source': {'path': 'gs://zvi-qa-private-test-data/zmlp/video/Pexels Videos 2257001.mp4', 'extension': 'mp4', 'filename': 'Pexels Videos 2257001.mp4', 'mimetype': 'video/mp4', 'filesize': 23187004, 'checksum': 9095702}, 'metrics': {'pipeline': [{'processor': 'zmlp_core.core.PreCacheSourceFileProcessor', 'module': 'standard', 'checksum': 2178814325, 'executionTime': 2.03, 'executionDate': '2020-06-02T18:19:28.992819'}, {'processor': 'zmlp_core.core.FileImportProcessor', 'module': 'standard', 'checksum': 117837444, 'executionTime': 4.02, 'executionDate': '2020-06-02T18:19:44.352441'}, {'processor': 'zmlp_core.proxy.ImageProxyProcessor', 'module': 'standard', 'checksum': 457707303, 'executionTime': 5.1, 'executionDate': '2020-06-02T18:20:15.128371'}, {'processor': 'zmlp_core.proxy.VideoProxyProcessor', 'module': 'standard', 'checksum': 482873147, 'executionTime': 11.22, 'executionDate': '2020-06-02T18:20:51.742224'}, {'processor': 'zmlp_analysis.zvi.ZviSimilarityProcessor', 'module': 'standard', 'checksum': 1879445844, 'executionTime': 0.51, 'executionDate': '2020-06-02T18:24:02.606951'}, {'processor': 'zmlp_analysis.zvi.ZviObjectDetectionProcessor', 'module': 'zvi-object-detection', 'checksum': 3329037091, 'executionTime': 0.72, 'executionDate': '2020-06-02T18:24:16.454150'}, {'processor': 'zmlp_analysis.zvi.ZviFaceDetectionProcessor', 'module': 'zvi-face-detection', 'checksum': 2666795579, 'executionTime': 0.33, 'executionDate': '2020-06-02T18:24:39.242582'}, {'processor': 'zmlp_analysis.zvi.ZviLabelDetectionProcessor', 'module': 'zvi-label-detection', 'checksum': 2989691564, 'executionTime': 2.91, 'executionDate': '2020-06-02T18:24:53.169698'}]}, 'media': {'width': 3840, 'height': 2160, 'length': 8.466792, 'timeCreated': '2019-05-01T16:34:45.000000Z', 'aspect': 1.78, 'orientation': 'landscape', 'type': 'video'}, 'clip': {'type': 'scene', 'start': 0.0, 'stop': 8.466792, 'track': 'full', 'length': 8.47, 'pile': 'NoveswjkGQ7wBQzuApQqDwnYvbk', 'sourceAssetId': 'JY4HNCtoOpEH0q7pfh3snWSBH6bm-8WK'}, 'files': [{'id': 'assets/JY4HNCtoOpEH0q7pfh3snWSBH6bm-8WK/proxy/image_1024x576.jpg', 'name': 'image_1024x576.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 218344, 'attrs': {'time_offset': 4.23, 'width': 1024, 'height': 576}}, {'id': 'assets/JY4HNCtoOpEH0q7pfh3snWSBH6bm-8WK/proxy/image_512x288.jpg', 'name': 'image_512x288.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 62263, 'attrs': {'time_offset': 4.23, 'width': 512, 'height': 288}}, {'id': 'assets/JY4HNCtoOpEH0q7pfh3snWSBH6bm-8WK/web-proxy/web-proxy.jpg', 'name': 'web-proxy.jpg', 'category': 'web-proxy', 'mimetype': 'image/jpeg', 'size': 26318, 'attrs': {'width': 1024, 'height': 576}}, {'id': 'assets/JY4HNCtoOpEH0q7pfh3snWSBH6bm-8WK/proxy/video_1024x576.mp4', 'name': 'video_1024x576.mp4', 'category': 'proxy', 'mimetype': 'video/mp4', 'size': 770057, 'attrs': {'width': 1024, 'height': 576, 'frames': 203, 'frameRate': 23.98}}], 'analysis': {'zvi-image-similarity': {'type': 'similarity', 'simhash': 'PHPEAPPEKKAGDKGMLPPBPAAPOPLPBIPPIJDHPOPPPPCPDPKPPBKPPPPPKENCLAIPCGOPPPPPPPKDAPPOAGPAHBDPEMLDCAMPEJJCELPAKFHHJHAHKMGGNOMPPPHBKPFPPPMPPDNOEAPPPBFPPOPCPFNPEAJEEPCCKPPPPJEPADBAMMPAPOEPPLPGIPEPBJMPAPJGJPHFAKALHPFGPCEMPNNPOHCFPAPJCBMBCBCGEPAKNIFAOPEPNMEPPPPPNPPHCGPIPPPPDDEPHCAAGAPPPDELBCCPPPIOPPCAAPJBJPCPPIABPIEBPBGNPPBFPEPPPCPBLPPPPPOPPPPPIKJEPPPOBMCPPPPIPPPPOAJPIJKGFPPPGIBHAPEPPINIGPPDBPPGPGPADOLBNPCBPFALFPIPNGPPBPPOBDPIDPPPKEMLCBJCAHAPPMPKBPGAEOPKHFIPMLPHPPPAMIPGPGDNCAPPPPFEEGOPPPDIPPPIPKPPNBAANMPPKBPPPBCAAPHLGOFCADPDPCEPPPPJKOMPPDPPCCPJHCJPPJDJPPAPPONPEPPPPPPHKLPPABPGPPJIPGPJPFPAOPPGPPIPBPDPBMHFPIAPJCCECGPEPGKGENBEPPJFBPIDDPLPAPPPACEPPIFBDMAABPPPPAPPDHJPNPPIPCPPPBAGAPJPFAPDFNPPAPEHAMKBOACJLKGPICKFDPPPPAPBKPIAFOPPPGGPNPPPPPGPAMPPPKPPNAHEPPFOMHIPCABPEPBPCNMAIEPCKOFHPHPIPPLCEBAILAHPBBFPPMAEOPBAFBHPPOCBAJKMCAHCPAKFPOPPPNPPCPKFPPPLLPPJDHOAGGDPBPPPCPCPPPDBGJKBPPFPNFPDJPLBBPDCCPCJPPDPAKBDPPBEPBPKGACPPBJPIEPMLPMNENLPPJPMGPPEPIEPOCGFPPPHEPCLHDHGOHAMANEENPDGDAEPOEPPBFGLDPPAPCPDGPFAKEGPAJPPNPKPPBFFIPPKNAAIEPAAPKMPPKCPCPIPNNJPPGPPPEOPHJOAPCPPMCPCKCBMPAPGDPJCPPDNCDPPGKPEPFCJJABPPPLBPPOIBLKLGPPAPGKNGKPABPPCKPBDFIPBCPAFPABBPBFPDFPHPHFNLBMEBPPMCHPLILOOJFPPCBDAFEBPBBDPBPIPHANIOKPEPLPPCPPNPOPJCJNONPEPEHCHMAPPPPMCPALCPDJAKFPBPOLPAPAFCAMNDKAAACPLGOPBBJPOEPPLDPFJCPCGOGPDGPPPGDPBMAPEAPAIDPOMPCANCDFPDIHPDPIBMDDNFGCOPOGEPFPFEPBHPPPCAPBPPBCAPBPCLPIPPMPOFDPPBBBHBPEPGPGPEMOEPPFPPPGPMPPLLAPMPPPPGDAGAAAKNBFPOACCCEAPEEHFPANKPPPNPPPPPNPFKEKNPPHFJPPEPPPPIIPPMFHPPBLBPELPLBHKPPPPPPFAPCPDPGPCIMMHICPFADHLDPHPKFCPLPGGANIPPHPPHINPPNPAEGHKAPPPDKCBPPAPEPPPPOEFHPPBFDHPMPMNMAMMIPGPAAMPPAPPPJKPPGPBPPPPPNPLPPABNDPPNCGEBPNPPANNPCMPEPBDNKDAAPLGPBPPAPPEDHEPCPEPPFCPPPCPHAIAFPLPACPHAPCGPOKPPJPPPPBPPCPMACPAECKPOPPPCPBPBPPPBPMGIDPPPFPAPALPAPPGPPACAPBPHLPHPPKDPAPAAIPPPPPMHEPJPPPMPLHEPPEPPBDPPLPPPIPPPBNCAJPDPPPEJPGKPBPEPNELPFDPANMOPCPEPJAPPNHIPPPPFHDAKEKIPLJFPAPPAAPIPLPEPHPPIPENJKHPAPCACPMDELHFBNCKEKPNBNPJBPLAPPPCFPEKPPDIPEPPHPLPPFAPBLPCKPODPLEPFPFCGPMPPMDGPPPMPBPPPPEPPDPPDPBAEPOLCFIEDBOMPDPFAPAPPEPEPPPOPPPBPPPPHGPIOPNBMFDBLNKCPGBAHFPJPPLOPANLDCPFCEAPMPLCPPPDOPBEDPJL'}, 'zvi-object-detection': {'type': 'labels', 'count': 1, 'predictions': [{'label': 'kite', 'score': 0.566, 'bbox': [0.556, 0.536, 0.59, 0.609]}]}, 'zvi-label-detection': {'type': 'labels', 'count': 0, 'predictions': []}}}}, {'_index': 'imx29yosuvoa7grd', '_type': '_doc', '_id': '2Oe9Qbku8nbGxybm5hE4_PdgzTReVdsa', '_score': 0.0, '_source': {'system': {'jobId': '13b61d1e-5f71-12a6-9398-7eb3d1ce02c4', 'dataSourceId': 'ba231dbf-dd21-12aa-9c3a-765f5c657424', 'timeCreated': '2020-06-02T18:00:01.383087Z', 'state': 'Analyzed', 'projectId': '6a72b6c1-aa96-415b-8c2d-df355ba9c8f5', 'taskId': '13b61d1f-5f71-12a6-9398-7eb3d1ce02c4', 'timeModified': '2020-06-02T18:24:58.173818Z'}, 'source': {'path': 'gs://zvi-qa-private-test-data/zmlp/video/Pexels Videos 2360537.mp4', 'extension': 'mp4', 'filename': 'Pexels Videos 2360537.mp4', 'mimetype': 'video/mp4', 'filesize': 2157469, 'checksum': 3107171640}, 'metrics': {'pipeline': [{'processor': 'zmlp_core.core.PreCacheSourceFileProcessor', 'module': 'standard', 'checksum': 2178814325, 'executionTime': 0.28, 'executionDate': '2020-06-02T18:19:32.774020'}, {'processor': 'zmlp_core.core.FileImportProcessor', 'module': 'standard', 'checksum': 117837444, 'executionTime': 0.98, 'executionDate': '2020-06-02T18:19:45.329583'}, {'processor': 'zmlp_core.proxy.ImageProxyProcessor', 'module': 'standard', 'checksum': 457707303, 'executionTime': 4.52, 'executionDate': '2020-06-02T18:20:19.145286'}, {'processor': 'zmlp_core.proxy.VideoProxyProcessor', 'module': 'standard', 'checksum': 482873147, 'executionTime': 2.19, 'executionDate': '2020-06-02T18:20:53.943025'}, {'processor': 'zmlp_analysis.zvi.ZviSimilarityProcessor', 'module': 'standard', 'checksum': 1879445844, 'executionTime': 0.54, 'executionDate': '2020-06-02T18:24:03.655909'}, {'processor': 'zmlp_analysis.zvi.ZviObjectDetectionProcessor', 'module': 'zvi-object-detection', 'checksum': 3329037091, 'executionTime': 0.73, 'executionDate': '2020-06-02T18:24:17.903256'}, {'processor': 'zmlp_analysis.zvi.ZviFaceDetectionProcessor', 'module': 'zvi-face-detection', 'checksum': 2666795579, 'executionTime': 0.52, 'executionDate': '2020-06-02T18:24:39.434687'}, {'processor': 'zmlp_analysis.zvi.ZviLabelDetectionProcessor', 'module': 'zvi-label-detection', 'checksum': 2989691564, 'executionTime': 2.93, 'executionDate': '2020-06-02T18:24:53.190085'}]}, 'media': {'width': 1920, 'height': 1080, 'length': 4.33, 'timeCreated': '2019-05-23T20:32:17.000000Z', 'aspect': 1.78, 'orientation': 'landscape', 'type': 'video'}, 'clip': {'type': 'scene', 'start': 0.0, 'stop': 4.33, 'track': 'full', 'length': 4.33, 'pile': 'OFUlxWxUDTuK--O80cDdPtslg8g', 'sourceAssetId': '2Oe9Qbku8nbGxybm5hE4_PdgzTReVdsa'}, 'files': [{'id': 'assets/2Oe9Qbku8nbGxybm5hE4_PdgzTReVdsa/proxy/image_1024x576.jpg', 'name': 'image_1024x576.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 320871, 'attrs': {'time_offset': 2.17, 'width': 1024, 'height': 576}}, {'id': 'assets/2Oe9Qbku8nbGxybm5hE4_PdgzTReVdsa/proxy/image_512x288.jpg', 'name': 'image_512x288.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 106479, 'attrs': {'time_offset': 2.17, 'width': 512, 'height': 288}}, {'id': 'assets/2Oe9Qbku8nbGxybm5hE4_PdgzTReVdsa/web-proxy/web-proxy.jpg', 'name': 'web-proxy.jpg', 'category': 'web-proxy', 'mimetype': 'image/jpeg', 'size': 84012, 'attrs': {'width': 1024, 'height': 576}}, {'id': 'assets/2Oe9Qbku8nbGxybm5hE4_PdgzTReVdsa/proxy/video_1024x576.mp4', 'name': 'video_1024x576.mp4', 'category': 'proxy', 'mimetype': 'video/mp4', 'size': 477075, 'attrs': {'width': 1024, 'height': 576, 'frames': 108, 'frameRate': 25.0}}], 'analysis': {'zvi-image-similarity': {'type': 'similarity', 'simhash': 'PPFPCCPHPKDHPHPGCGEPBLDPPPPPDHPPPPAPDLPPLFALPKINKKPPPBDPPPPPPNPPPENPPOLBPPIINPCPDPMJKKPPPPLDCPMIAPHFPPPCPFPIGFPJPDPBNPAPPPBPGCPPPPPBPPOCPPPPPPHPNGLDPIJINKICPBPIPPCIHEPMPPPJPAPMFIPPPEOJELGBPOPEJPHDPFGGPHPPLMOEGNDICPPPPPIMPDMIPIJPPPPPBHPEPPPPPOPOFPLPPPPFPBEPIPGPJOPPPPPCPPPOBMNPPCPBIPPMDFKPPFCFHPPOPPFHOICPPPPPPCPMLPJGBPPDPKHDJCPPPPCJJPOPPMCPPPPPPPPGIDPPAPDPPNPPLPHPGCGPHPCOJFPHMPAMDPBKPKKGJLCPDPHBNJPPLMPPPPLOIPKGBPEPFPEPGBJPLPAPPOPOKGHIPPOGAKPAHCFEPPNPPPIIJPPDPGJPPBPPPKGOABPMPBPGPOLPLPPPNLPBPPPPPPBJBAPNFPPEGPPBPDHGLGPCPPPDPPPPPPPPPOOICMPFPAPLIPPPPPFHGPNCFHPAHDKFHPEGAPPHPPGIFFPPKPOEPIFPPPHPPPPPPLFEPPHPMPFLIPOPBPFPLNPIPNPDPPOPPPBGPEAMPPDPOPPDPPPPPDPMPPGDGPPPPOPOPPPCPPCFPPPKPPNPPKPDPKOPPAPPPPLPHKMPEPDPPPPPPPCLPPPPAPPBPPKPHPDPPPBPCPPPAMPBLKIFPHCPOIKGHOPJBPMEPPPABMLPLKENPPEPPNKPMKPEPHPPIPPPPEFCHPPFKPPKPPPPPGMGLBPPPHPCPPPPLPJLPNPLPPGPEPPPBOHPAPPPBPPDPGBPPGPPPNPJFALPFPKAPCDPDDBGPFLPNOGPPOPPKPDPINEPGPPPCLHJPJPKFPPDPCPHCPPLKPICPFPNPPFPAEPPBPADKMPPPPPPIPFHGPPBPPPIPFDPNPCPPPDPEFPPCCOGMPPPPPJPFPPPKPBPPPPPKJPDPLNFPPPPPPPFNPJFJPGCPIPPNPPOPBPPPPPEKJPAEPPPPOAPPLPPAAPIPHFNKANPPMPHFDEPCPPOAPPPPPPPLFEDPPJPBPMALKPPGPPPMNPKDNHEJMPHPPFKJGBMMPPPECPFPOPMPPLAPDPPDPPOMKCPNPPPKEMIECPDFCPPEPGNPPPPCFPPPIKFPDPAOPFJPLPPOPEPEPIPJMEGPPCBINEPHFKPPHPOPPBPFPPPPPPKPMJOPLIPPKEDPJPPFPPIAMFPPGJPGPCPPPMELGJNPGCBPGBLBPPPGDKGNPPPPMNJPPOIOPCIPPDPKAPFPAPGPBJPFCKPPPPLPPPPONNPMCAPPPPMPGPPPPDJPPLBFOKKCPPPKJPPPBKPPKPDPDHHCKPPOPPPIPPPCPJPOPPPPNDDBPHPBJBCLFDPPHPCEEOCPPJKOFCMKEAPPPPPCMPPPLPPPPLPHPPOPPBPPKAGPPHPMPPKPPGPLFNPKAPMPGPNNCAPBNKPPPPEPPPPIPKLMMPPCLOIPPPGFPNPPGCDPPLOLOPPKIPCFPPLPPPGMCPMCPPMOPPHPPPJFDPLJPCJPGPOEPIPPPJPIKNJPPPPPPPPPPAPPPPBPPNPOPHPHNALMDPLBBPMPGPHEEKGINHPFLEFPCOPPPFPBEGPPPPPPDKPDPPPEBPPPIPNPPPJFOPJPPHPIOEJHPNPPPEOJPEIPPPDPPPPPPAPPGPPPFPPDPIPPBAHPFPDFPAPOLDPEPPDLPPMKPLFHPJPDIPMDLPPBJHPPAPMPPIPPPGGPPPMPCGPPJPMLGPPBPPPGDPMPJBPNHPPEGPLPGPPEPJPPPCHNOPPPDHPCPJPPPBPPPBLLGPFNKPLFFFODPJPPPPGIEPPMPPPAPKPBPJPPPPDPFHAPCPHOLMHPEPPEPPPBDFIAGPPPPEPPPBKMPPIPPPPPLBIPHPPPGPPDPILGPPPDMPNAPPPHDPHPPDPFONPAPPPJFKPLBPPAPPIIPMCPJKPPPAOPOPPPKGPLPGIPDILNLBIFGHPMPPGIIBPGPAP'}, 'zvi-object-detection': {'type': 'labels', 'count': 1, 'predictions': [{'label': 'cup', 'score': 0.556, 'bbox': [0.031, 0.2, 0.244, 0.967]}]}, 'zvi-label-detection': {'type': 'labels', 'count': 1, 'predictions': [{'label': 'beaker', 'score': 0.399}]}}}}, {'_index': 'imx29yosuvoa7grd', '_type': '_doc', '_id': 'zHJrhq734A_gG-PfCMRZKmyCVpqpl3pF', '_score': 0.0, '_source': {'system': {'jobId': '13b61d1e-5f71-12a6-9398-7eb3d1ce02c4', 'dataSourceId': 'ba231dbf-dd21-12aa-9c3a-765f5c657424', 'timeCreated': '2020-06-02T18:00:01.381850Z', 'state': 'Analyzed', 'projectId': '6a72b6c1-aa96-415b-8c2d-df355ba9c8f5', 'taskId': '13b61d1f-5f71-12a6-9398-7eb3d1ce02c4', 'timeModified': '2020-06-02T18:24:58.174108Z'}, 'source': {'path': 'gs://zvi-qa-private-test-data/zmlp/video/Pexels Videos 2288346.mp4', 'extension': 'mp4', 'filename': 'Pexels Videos 2288346.mp4', 'mimetype': 'video/mp4', 'filesize': 10965831, 'checksum': 1322489117}, 'metrics': {'pipeline': [{'processor': 'zmlp_core.core.PreCacheSourceFileProcessor', 'module': 'standard', 'checksum': 2178814325, 'executionTime': 1.1, 'executionDate': '2020-06-02T18:19:30.092736'}, {'processor': 'zmlp_core.core.FileImportProcessor', 'module': 'standard', 'checksum': 117837444, 'executionTime': 1.53, 'executionDate': '2020-06-02T18:19:41.858667'}, {'processor': 'zmlp_core.proxy.ImageProxyProcessor', 'module': 'standard', 'checksum': 457707303, 'executionTime': 4.61, 'executionDate': '2020-06-02T18:20:14.627756'}, {'processor': 'zmlp_core.proxy.VideoProxyProcessor', 'module': 'standard', 'checksum': 482873147, 'executionTime': 8.93, 'executionDate': '2020-06-02T18:20:49.452620'}, {'processor': 'zmlp_analysis.zvi.ZviSimilarityProcessor', 'module': 'standard', 'checksum': 1879445844, 'executionTime': 0.95, 'executionDate': '2020-06-02T18:24:02.099818'}, {'processor': 'zmlp_analysis.zvi.ZviObjectDetectionProcessor', 'module': 'zvi-object-detection', 'checksum': 3329037091, 'executionTime': 1.13, 'executionDate': '2020-06-02T18:24:15.731930'}, {'processor': 'zmlp_analysis.zvi.ZviFaceDetectionProcessor', 'module': 'zvi-face-detection', 'checksum': 2666795579, 'executionTime': 0.33, 'executionDate': '2020-06-02T18:24:39.237220'}, {'processor': 'zmlp_analysis.zvi.ZviLabelDetectionProcessor', 'module': 'zvi-label-detection', 'checksum': 2989691564, 'executionTime': 3.59, 'executionDate': '2020-06-02T18:24:53.849793'}]}, 'media': {'width': 1920, 'height': 1080, 'length': 18.09, 'timeCreated': '2019-05-08T07:55:32.000000Z', 'aspect': 1.78, 'orientation': 'landscape', 'type': 'video'}, 'clip': {'type': 'scene', 'start': 0.0, 'stop': 18.09, 'track': 'full', 'length': 18.09, 'pile': 'VEXfN7nU61ZfHmVYUmu2oBbwxUI', 'sourceAssetId': 'zHJrhq734A_gG-PfCMRZKmyCVpqpl3pF'}, 'files': [{'id': 'assets/zHJrhq734A_gG-PfCMRZKmyCVpqpl3pF/proxy/image_1024x576.jpg', 'name': 'image_1024x576.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 116420, 'attrs': {'time_offset': 9.04, 'width': 1024, 'height': 576}}, {'id': 'assets/zHJrhq734A_gG-PfCMRZKmyCVpqpl3pF/proxy/image_512x288.jpg', 'name': 'image_512x288.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 44675, 'attrs': {'time_offset': 9.04, 'width': 512, 'height': 288}}, {'id': 'assets/zHJrhq734A_gG-PfCMRZKmyCVpqpl3pF/web-proxy/web-proxy.jpg', 'name': 'web-proxy.jpg', 'category': 'web-proxy', 'mimetype': 'image/jpeg', 'size': 15100, 'attrs': {'width': 1024, 'height': 576}}, {'id': 'assets/zHJrhq734A_gG-PfCMRZKmyCVpqpl3pF/proxy/video_1024x576.mp4', 'name': 'video_1024x576.mp4', 'category': 'proxy', 'mimetype': 'video/mp4', 'size': 1270699, 'attrs': {'width': 1024, 'height': 576, 'frames': 452, 'frameRate': 25.0}}], 'analysis': {'zvi-image-similarity': {'type': 'similarity', 'simhash': 'HCPDAPOFIPAIPPADPDPBPPIPPPIPJPPEPAMPPDHPJPBFHPPOMCFPFDPDFPPIBBDOEAGOOPPJPEPAGEPPAGJEAAPPFPOAPFPGOCPAPKKJPAGFOBELPOIKHEAOPDPPPPNLHPBDFHBDEAPPPPBNPCPPFCPPPAPACDAINPCIPHAPDBANPPKAAMGPPPPPNPPHAPPMFNCPBDBJGLCKPPHNMPCPPAPPPBDKPDFBMNMDBFLPIHAPPPGACBNMFBHPPPPDPHENFAPAPOPPPGEIHLPEAAPCHGCPGAFPPFLEPDJAAPPDPPADHFAILICDPDBAPPKOPPPBACKINLPPPFJPJCBPKMDMPKALPJEPPAPCPCAPJDJKDEPFFAMPMFKJALPDPOBABNCBDPMJPAAPDFPHPPGMPPDFOPKPPGPONPPPBCPPMAPJLCLFKBJDAHMPNECPCNIAEAPLPPPBMDPLPPPDBPPANPBPCAFPPPPGPDMPDPBGPJPAPAFPPBPKGPNPPAPCPPHMAPJPLPMIKBPBNPDPPPHLBCEPPPPGJGPCLPPPPCNDGPLEAHPPMHCGIPPPNFPFCMBCPIFBFAPGILPPMPAHNPEPIDPPFPPPEGEPBKPCDPPFPDFMAPECPPPGCPFBHPPPNPPPDKPMBPAMBDGPCMKFPPBGLNPLKGPEPAPPMPFEANFFPECACPPDAPGDAHCLPNNPPACEKEPEDLPJLPEPCDPOBOPMIKPPPKPPDNFGEBBPPANNGCPPPPBAPDPPHCEIMEOPCPEDKEBJPOAAPPOJPPKFIAMDKCAIFMKMPAPDCNAKDBIKPGANAHPIPCPPMAPEDPPPHEPNPJJBFJPGBPAPFDNBGKEPHPPHPDBPPFFDPPPECDDPPPIABPPDHBPEOPFPPPPFPIPEPLBBPPEDHHBPNJBPOJPPBAPBPCGPCPPFAPPHPPKPPPPAHPPBAACPJPPPFNPPAPACPLIBCFACDFPHDPPEPOBDEAPPPICKPADPGFPPCPPPDPACPDPCDEPPHPACBLBPMODNAPBHPJCPPBJBJIKPKMHIPPPJPBPADPAPAPBPNPAPPOAIGHPPEPDPPOMJABCPPCPCJFPABBFPPPBPEGPAEAPGJNPGKPAAGPPGPPPAPBCAHFFLLIPPPKGGFFPEAPPEPLPPPJPCABPPPJLMNFDKPPJPDPMPDAHCPMCCBIPPPPLPPPPJLPOPPPBPEEKPJBBIHPKDJPFBHAPFDMPGNFPPPCABPGNOMNBCAOPPCPHKBEPHPICPPDEMGPPFCDPPBMAPGLPJEGOGMPPPAPPPJMMKEOAPPOCPBPEGJFLAPDPAPHJCPFJCHEENHEPOAPOOLBACIPPDDPEPAMJPCPOPDHNGCPPOPKPCIOBPFPEAKPDPJDFHDCHPPEPPDAEAAIAAPMALPAPKCPBPPABHDDAPACPFPAPHPPPKBDLPPPPPJPBBPPPPDHPPCIPPHCKGBPPGGBDDPPACHDBBEFDCPCDAKCJMPAHKDCBPMEPPCLAPIPCMHBGPPJPGAJPPPPHEPGHMBPPKEPBPPPIBDPFPALKPPCPNBEPIPPNPPKPPKKAHAENPILPIPLPPPMPCPPPCPPPEPPPCPGNECPGLGGCMPAPBLKFPDPEAPDPAPPGPJDPPAPPLGJNLPEPEDCAPJOEMGMIAGPBEIGGLMPBEKMAPDPAPHDAPCCMPAPPPLKPPAAGPKPPNBPDPPPAPKIPPPJHAPCPGAPPEPPABINGAHMHPLGCCLAPPCAPHNIEEPCHOLPGEPEBNPGMPPCEPPAIOPHPPNACOLMPPPPFDPLGDPFMAPPPPPDCDPPOFPBPBOBANHPBNPAKDAPPBAPANPPEFPOPNEHHAPBFPCKPHPNLFADAPAPCPHKGAPPKHAPPJEDPOHLPAKONCPLDDFPPLAABBIPBNEPIPPBOPBFPEBNPPHDGPAIAPPJFPEPPACPKPDEOPPPAPPCPJMPPCCPDPMDDABLFPPCPJPNANPDGGHKOAPMOJCJPNPCPFPPCDCPCAEGBBIKBAFFOPCAPIPCPCJPOEACADPPABPPPANFPAPABM'}, 'zvi-label-detection': {'type': 'labels', 'count': 2, 'predictions': [{'label': 'candle', 'score': 0.567}, {'label': 'table_lamp', 'score': 0.188}]}}}}, {'_index': 'imx29yosuvoa7grd', '_type': '_doc', '_id': 'V8Ic8d1ispq6PzR9kGD7HMo4Ue9vqGfh', '_score': 0.0, '_source': {'system': {'jobId': '13b61d1e-5f71-12a6-9398-7eb3d1ce02c4', 'dataSourceId': 'ba231dbf-dd21-12aa-9c3a-765f5c657424', 'timeCreated': '2020-06-02T18:00:01.382379Z', 'state': 'Analyzed', 'projectId': '6a72b6c1-aa96-415b-8c2d-df355ba9c8f5', 'taskId': '13b61d1f-5f71-12a6-9398-7eb3d1ce02c4', 'timeModified': '2020-06-02T18:24:58.174290Z'}, 'source': {'path': 'gs://zvi-qa-private-test-data/zmlp/video/Pexels Videos 2317858.mp4', 'extension': 'mp4', 'filename': 'Pexels Videos 2317858.mp4', 'mimetype': 'video/mp4', 'filesize': 10660880, 'checksum': 698700530}, 'metrics': {'pipeline': [{'processor': 'zmlp_core.core.PreCacheSourceFileProcessor', 'module': 'standard', 'checksum': 2178814325, 'executionTime': 1.17, 'executionDate': '2020-06-02T18:19:32.225949'}, {'processor': 'zmlp_core.core.FileImportProcessor', 'module': 'standard', 'checksum': 117837444, 'executionTime': 2.02, 'executionDate': '2020-06-02T18:19:43.883737'}, {'processor': 'zmlp_core.proxy.ImageProxyProcessor', 'module': 'standard', 'checksum': 457707303, 'executionTime': 4.75, 'executionDate': '2020-06-02T18:20:14.773015'}, {'processor': 'zmlp_core.proxy.VideoProxyProcessor', 'module': 'standard', 'checksum': 482873147, 'executionTime': 12.91, 'executionDate': '2020-06-02T18:20:53.435872'}, {'processor': 'zmlp_analysis.zvi.ZviSimilarityProcessor', 'module': 'standard', 'checksum': 1879445844, 'executionTime': 0.51, 'executionDate': '2020-06-02T18:24:03.116103'}, {'processor': 'zmlp_analysis.zvi.ZviObjectDetectionProcessor', 'module': 'zvi-object-detection', 'checksum': 3329037091, 'executionTime': 0.72, 'executionDate': '2020-06-02T18:24:17.176605'}, {'processor': 'zmlp_analysis.zvi.ZviFaceDetectionProcessor', 'module': 'zvi-face-detection', 'checksum': 2666795579, 'executionTime': 0.4, 'executionDate': '2020-06-02T18:24:39.315105'}, {'processor': 'zmlp_analysis.zvi.ZviLabelDetectionProcessor', 'module': 'zvi-label-detection', 'checksum': 2989691564, 'executionTime': 3.61, 'executionDate': '2020-06-02T18:24:53.866831'}]}, 'media': {'width': 1920, 'height': 1080, 'length': 30.238542, 'timeCreated': '2019-05-14T20:12:03.000000Z', 'aspect': 1.78, 'orientation': 'landscape', 'type': 'video'}, 'clip': {'type': 'scene', 'start': 0.0, 'stop': 30.238542, 'track': 'full', 'length': 30.240000000000002, 'pile': 'K4tLSI6oXLXwzyKgRVNyzdM7zXA', 'sourceAssetId': 'V8Ic8d1ispq6PzR9kGD7HMo4Ue9vqGfh'}, 'files': [{'id': 'assets/V8Ic8d1ispq6PzR9kGD7HMo4Ue9vqGfh/proxy/image_1024x576.jpg', 'name': 'image_1024x576.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 462986, 'attrs': {'time_offset': 15.12, 'width': 1024, 'height': 576}}, {'id': 'assets/V8Ic8d1ispq6PzR9kGD7HMo4Ue9vqGfh/proxy/image_512x288.jpg', 'name': 'image_512x288.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 140468, 'attrs': {'time_offset': 15.12, 'width': 512, 'height': 288}}, {'id': 'assets/V8Ic8d1ispq6PzR9kGD7HMo4Ue9vqGfh/web-proxy/web-proxy.jpg', 'name': 'web-proxy.jpg', 'category': 'web-proxy', 'mimetype': 'image/jpeg', 'size': 123695, 'attrs': {'width': 1024, 'height': 576}}, {'id': 'assets/V8Ic8d1ispq6PzR9kGD7HMo4Ue9vqGfh/proxy/video_1024x576.mp4', 'name': 'video_1024x576.mp4', 'category': 'proxy', 'mimetype': 'video/mp4', 'size': 2530082, 'attrs': {'width': 1024, 'height': 576, 'frames': 725, 'frameRate': 23.98}}], 'analysis': {'zvi-image-similarity': {'type': 'similarity', 'simhash': 'DPPPPJEPPPDCPIPPCBECPIJPPPPOJPKPLPDPLOPOPPPPPEPPPHPOPDCOJIPPLNPEKMPPOPFPPCPJOLPIFPIAEIKPPPPAPPCPCPNPMDKDDBPPPPAHPCOIPNPPPPKKPFMPKPAPPPFMPPMPPPAPFPHPPPJMKPOINPPBPPPPCMPPHPNLPHPPPPPPIIHLHPPEIPPPOPDBNCPPPPPPGPJGFPIPPPPPPPBBCOPFPNLEPPPKAPILMHEPLPOGEPPPPJPBPPNCACPPIFJPPPDPPPFEPCPNEGBKKPOEEPPPPNEIHJPPEPAPPNBPPIMPPGKPHPGPKPNAPHPBKIPPAPPJDGPPKAIGLPPPHPGFPPHMGPPPPPFPNJKPALIKPKPJAPNPPIBNPHFPPPMPHCIPPJPNCMPCPCPMNPFDJNPPPPKPFPPKAPPHAGPPBCPLCPPPPJPAAJLDPCPLGLPPNPPPPPPFGPPEOPMPPFHPPDPIPMAMPMJPPPPFCPPPPFHHJAOPPPLPGAPKPPEPEGEPMMPPMCEJPKPPBPPPOFPPELPJPPEJPIFPPGJHPIPHGPACJPPPPPKNCPPJPBANPDPPPPJNPPJAPPAPDPPPPPEFPJIKPPHGFEPMLPHBHMCPPPPEJPEMPPGEPOCPBPFIPEPLPPEKPIPPHPLJPOPNPPPHPDPLPFPKEPHAGPPPPAPPLFPPPGPPPEMPPPBPIPGPJPPPPLNGPPPPGLDPCDABPPPPPPPPAPPGBMPLFPPPPPANNDHCFPOPDPGDPPPINLNFENPIMNHPPPKPHBPPJJPOPDNPKPOBPMPOPPPPFKPNODPPPAPLPFPPPBPHPPPPPNEHLPFPAPPPPBIKBPHCHPPANPPPMPBPPHPKPIPPEPPELPBJHMDGFOPPJLCPMPPJPKKENPPLBFJPEGPMBJPFPCACPJDFLCNBGPEINPPPPPPGPPPPPPPPJPPPPPPPAPAPFIPPPIPECPPPBPNPPIHDPPMPGINJOFMPDPNPPAIPLPEPPPPMPNPEGMFBPKPPPPEPPPPPPPOPPAPDKHJPBPPPPOLIDEPPCPEPPJBPLPPIJKPPPBKGPCPPPDAPGIDPCIKFLPPPPPPPLDMPPPPPEPHKEGIFIPPPJGPGPPPICNFCPPCBLFJPPFKPABHLDJPKPJPDPEPLHPPPIFJPJAJPGGGGDPPPGJPHDBPPLPPPMPPPPEMIKMPPCPPLPLFGKEDPPPPPPGPOPHLGMDBBNLPPFPPPAEFPCJPPGMPOPIPPPPAPDEEPPPPPFPPPPDCPPNPAPMCPIPPFOPPHFPPPEDALCDJNPEPPHBGPPDHCPKNPPPFKPPNPIMMPPLGEMHCKKBFMPPPIPGPJEPPKPHFODPGPAPPPMJFPMPCBCOCLCEKPPGPPFJPPPPPHJPLDPOEJPEPPDJPPCPJPOPMPPPGACILPJPPPCPLPPEJDPPLPOPAKPBPFPGAPPPDPAFIKPPPPFDPPPPPPPJBFJBJPPPPPOOPOFHHKAPPPFPPNPPPPPIPPKIHPFGPPPPPPPPJANGPPBPPPIPPIGPAHNPPKPPDJPPPPIGGPPPPFPPPPPCCDKBPPPJPCPHDPILPPPPFJPHMDLPDGGPLPCLCPPPBLPPPPCPGGPBCOPPPGJPIPGLNPGDPPCAPPPFPPPKBHMPEPPPPPPCPPPNFPBPDNPPAPPGMLCPPPPIFBHIKAPPEBKPPCPODPOPPPKOEFBPJPJPPPPPPNBPPPMMCPLGBGPPPPLPLHGBIMPLPBPPEPLPBPFPKOPPPPBPPCINPPPBPDMJPPPPJPPEPIPFHBPPPPPPPDPBPNPOIPHAPIPPIPDCPNGGPPPPPPAHLAIPOPMIDPMPKPGJPPPPPDLMPPPPPPDPKJAEPCJHPPPJFGPDPKNPPGHPPPEPKHPPIPPBPIPPPHPPPPPPALPPPPPOPPGDPNFFDPDEJPIPDPPPOPPPPCHNPKPJPEFMPPPDPPDPFNPPAEAPHMPPIPHPKCPONPLPPMPPPLPPPPHDPEPPEPPMAPPMPPPMBCIKPBLCLKPKJDPBBJPFAPFEIPHPIFNPDKFPCP'}, 'zvi-object-detection': {'type': 'labels', 'count': 1, 'predictions': [{'label': 'vase', 'score': 0.739, 'bbox': [0.451, 0.46, 0.554, 0.938]}]}, 'zvi-label-detection': {'type': 'labels', 'count': 1, 'predictions': [{'label': 'combination_lock', 'score': 0.84}]}}}}, {'_index': 'imx29yosuvoa7grd', '_type': '_doc', '_id': 'bWY9MNwX8J_P2zvh4v0fA-fgi5jcuFbC', '_score': 0.0, '_source': {'system': {'jobId': '13b61d1e-5f71-12a6-9398-7eb3d1ce02c4', 'dataSourceId': 'ba231dbf-dd21-12aa-9c3a-765f5c657424', 'timeCreated': '2020-06-02T18:00:01.382003Z', 'state': 'Analyzed', 'projectId': '6a72b6c1-aa96-415b-8c2d-df355ba9c8f5', 'taskId': '13b61d1f-5f71-12a6-9398-7eb3d1ce02c4', 'timeModified': '2020-06-02T18:24:58.174504Z'}, 'source': {'path': 'gs://zvi-qa-private-test-data/zmlp/video/Pexels Videos 2301284.mp4', 'extension': 'mp4', 'filename': 'Pexels Videos 2301284.mp4', 'mimetype': 'video/mp4', 'filesize': 6794833, 'checksum': 4081942716}, 'metrics': {'pipeline': [{'processor': 'zmlp_core.core.PreCacheSourceFileProcessor', 'module': 'standard', 'checksum': 2178814325, 'executionTime': 0.79, 'executionDate': '2020-06-02T18:19:30.884863'}, {'processor': 'zmlp_core.core.FileImportProcessor', 'module': 'standard', 'checksum': 117837444, 'executionTime': 1.35, 'executionDate': '2020-06-02T18:19:41.679173'}, {'processor': 'zmlp_core.proxy.ImageProxyProcessor', 'module': 'standard', 'checksum': 457707303, 'executionTime': 4.65, 'executionDate': '2020-06-02T18:20:14.668074'}, {'processor': 'zmlp_core.proxy.VideoProxyProcessor', 'module': 'standard', 'checksum': 482873147, 'executionTime': 20.64, 'executionDate': '2020-06-02T18:21:01.161740'}, {'processor': 'zmlp_analysis.zvi.ZviSimilarityProcessor', 'module': 'standard', 'checksum': 1879445844, 'executionTime': 0.53, 'executionDate': '2020-06-02T18:24:04.184282'}, {'processor': 'zmlp_analysis.zvi.ZviObjectDetectionProcessor', 'module': 'zvi-object-detection', 'checksum': 3329037091, 'executionTime': 0.73, 'executionDate': '2020-06-02T18:24:18.634950'}, {'processor': 'zmlp_analysis.zvi.ZviFaceDetectionProcessor', 'module': 'zvi-face-detection', 'checksum': 2666795579, 'executionTime': 0.38, 'executionDate': '2020-06-02T18:24:39.619419'}, {'processor': 'zmlp_analysis.zvi.ZviLabelDetectionProcessor', 'module': 'zvi-label-detection', 'checksum': 2989691564, 'executionTime': 0.71, 'executionDate': '2020-06-02T18:24:53.907141'}]}, 'media': {'width': 1080, 'height': 1920, 'length': 14.015, 'timeCreated': '2019-05-10T23:02:26.000000Z', 'aspect': 0.56, 'orientation': 'portrait', 'type': 'video'}, 'clip': {'type': 'scene', 'start': 0.0, 'stop': 14.015, 'track': 'full', 'length': 14.02, 'pile': 'p-5jorA5DdWSC__ck7iCLZE0yrA', 'sourceAssetId': 'bWY9MNwX8J_P2zvh4v0fA-fgi5jcuFbC'}, 'files': [{'id': 'assets/bWY9MNwX8J_P2zvh4v0fA-fgi5jcuFbC/proxy/image_576x1024.jpg', 'name': 'image_576x1024.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 241029, 'attrs': {'time_offset': 7.01, 'width': 576, 'height': 1024}}, {'id': 'assets/bWY9MNwX8J_P2zvh4v0fA-fgi5jcuFbC/proxy/image_288x512.jpg', 'name': 'image_288x512.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 86664, 'attrs': {'time_offset': 7.01, 'width': 288, 'height': 512}}, {'id': 'assets/bWY9MNwX8J_P2zvh4v0fA-fgi5jcuFbC/web-proxy/web-proxy.jpg', 'name': 'web-proxy.jpg', 'category': 'web-proxy', 'mimetype': 'image/jpeg', 'size': 58655, 'attrs': {'width': 576, 'height': 1024}}, {'id': 'assets/bWY9MNwX8J_P2zvh4v0fA-fgi5jcuFbC/proxy/video_1024x1820.mp4', 'name': 'video_1024x1820.mp4', 'category': 'proxy', 'mimetype': 'video/mp4', 'size': 12712119, 'attrs': {'width': 1024, 'height': 1820, 'frames': 842, 'frameRate': 60.0}}], 'analysis': {'zvi-image-similarity': {'type': 'similarity', 'simhash': 'KPIEFPJPDPBPMHEPPAAAFHPPLPBAPPPPPPFHBCPFJIPPPPPPPBPPHBPPFAIPBJPDBAPPPPHPMINPKPEPHPGCAPJPPBKGFBHPKOHCFAPAPELPFJGEPAIMJIIPPNPFGFHDCPDLPPOCDPPPPPJPPPPAHFNPHPAJDJNHPPPPPPIENHPBPMDKEPPPPPPDPPCFPPPPCBBEPFFAPDBMFCPCDPABPCPPGPIPCPMOGGBNMMGCABKFPPFPGOAPCLFPPOPPPDCPBAECPJKAEPBDPEOBHHILPPCEEPEPPPPIFFIKHOPIPLCKACPPNPCBPAFFPPPBIPOODPEPPMMPDPKPPKPPPNJNPAIPLPLPCNDHPBPPOPEPBDPPBKPPPHBPAPFPPPHGLPPPECGPKBGMKPNPAENCPLPHFPPPCPNAIPCBEPPPPAIPECKOBBDCEJPPDPJPDPEHHPPIIPBPGPPIPPPEEPPIGDOFPLHPPPAEBBPKFLLFHOPKKPPHPPPHPPHNPPLFPPHAMNJEKPKHAAPEPPPPMODHAPPPHFPPKKPKPPPPFBPPFCBPHJPPPBPCAPAPNPPOBCPCPAPCPAPDBPJPHPGIDDEEGFPPOLPHLEDDPOHPAPIPPEFJPEPPPPBOAPJPIMAFNPPLPPHPEHPCLPPMPAACDPMAPHKGDPPAPGPPPPKFAPHFIIICPAPHMPPPKBJOPPEPPPBPKPHFCPPDPPGLPKPDBPPAOPFEPPLPPINMBPPHPPPEAMBBPHEJCFHPEOJPPPAPCPFAAHHAMPBPGPPPDOFAOPIGAHPGPPKKPGPGBOFAPPPMPPPPDBPBDPPPABPLPPPCPKPPGPELPPCPHBHPPPPNLDJPBPPEMKFDGPFPPPPGPPCPPHPHPDPPDPIADKCPBAGAEGPNPPGPBPFMCJDPAPPPBPJBEIAAPCPEMAPPAPDDPPDPEEPPAIPGMLDNFNFPPKPPBAMPCEAACGFFAMMPEMHPDEPCPGPPGDCGPPPGHPBOPPPPFDFLPPPBAPPPPPBBPPJPFHPADPPPMLFPPFPLPPGPPBPMPKCPCLPCPNPJEALLEEGOCDOBFBPPPIPIPCPFPLGNAPCGFLPPBPPPDFPPAPPOHJPCJPPPIPAPFDPCEFGBDNAPPPLPPPPPBCBAGNBEAPPKLPPPPAPBCAPAIBJHEPHBCKEPAHKPMBFPDIPMPBMPFEPPICPGPEDBPPACMDHNCPFFPPPHHDJIPPCDKHHPEIPPPLAEFGCPPDIPPPPIDCAPPLAPPPBJKPPFPPHPEDCBPAPJBPHPPHPHPAMBBJKPPGCPFJGKPEPPDCKPCPIFDONNLPALIBBNIHHPPNPFIOCJCKAKPKPPCFIBHHPAPBEPOECEFJPPPMKPPPPEPOPEONFPGIGPPPJLPLIPPCIOFPJHLHGJPPPPDGPPLPDPENNABPLPAPPPPBHDPNGJPPBJGPBPPBBEPPPPGLPPPCJFPPPPFCPCPPCDEAFLCBBPPFKCPPDCPJHKPPPAPJPPNFPBKDDGFLOPNPBPPMPFPPPGELGIPEAPJPPDBDAPKHJFJKPPIPPPDCHDMPBFJHFIIPDGCBOCPEPHNBMPAGPLPGCPFFFIPPPILGGPACPLBFBPPPHAMPNPCAKBPCPBDPNPMIJPPPMPOHPPEPGAEGEPDPPLKIBPPAPPBKKPPPGJKLBCPCPPBPPAPNPPPPPAFPPFDAJPPFFPGKKNEBPPJNEAPEPNJBPPPPPKKLLPLECPKLMCHPPADIHPNJPHOGPPMCBADPMBDPPBFBCFAPHLPJPPHMPOCECPPKPPPPMHPNPDFPPCPAPDPPGLPKPAPIPGNPFPPPDIAMPLPOPBAEHANHPDPJLPKACPPMCPGMALPPDPGDGPGPPPPCHDPPPIMJBAPDIHBPICEPBIMPCPHPBPPPAGOPGAJPAPOAHKKOPPPIDOINPPCPPBPPHDPIODEPIBLNOPPPPAPBPAJNPPPPPPAFCBHMPIOPOPPPPJPPPLMPPJBMDAAGPKOPPGIPAOEPHNIPPHNOBPIFEBPPBGGPDIDFADJCBDPPCPBPCACNFPHPAP'}, 'zvi-label-detection': {'type': 'labels', 'count': 1, 'predictions': [{'label': 'torch', 'score': 0.152}]}}}}, {'_index': 'imx29yosuvoa7grd', '_type': '_doc', '_id': '_dxQehYp1RIXVSizjHw3Qmzw908YV_IV', '_score': 0.0, '_source': {'system': {'jobId': '13b61d1e-5f71-12a6-9398-7eb3d1ce02c4', 'dataSourceId': 'ba231dbf-dd21-12aa-9c3a-765f5c657424', 'timeCreated': '2020-06-02T18:00:01.382119Z', 'state': 'Analyzed', 'projectId': '6a72b6c1-aa96-415b-8c2d-df355ba9c8f5', 'taskId': '13b61d1f-5f71-12a6-9398-7eb3d1ce02c4', 'timeModified': '2020-06-02T18:24:58.174910Z'}, 'source': {'path': 'gs://zvi-qa-private-test-data/zmlp/video/Pexels Videos 2311965.mp4', 'extension': 'mp4', 'filename': 'Pexels Videos 2311965.mp4', 'mimetype': 'video/mp4', 'filesize': 14913404, 'checksum': 3162315020}, 'metrics': {'pipeline': [{'processor': 'zmlp_core.core.PreCacheSourceFileProcessor', 'module': 'standard', 'checksum': 2178814325, 'executionTime': 1.61, 'executionDate': '2020-06-02T18:19:32.491931'}, {'processor': 'zmlp_core.core.FileImportProcessor', 'module': 'standard', 'checksum': 117837444, 'executionTime': 1.57, 'executionDate': '2020-06-02T18:19:45.451311'}, {'processor': 'zmlp_core.proxy.ImageProxyProcessor', 'module': 'standard', 'checksum': 457707303, 'executionTime': 4.2, 'executionDate': '2020-06-02T18:20:18.974291'}, {'processor': 'zmlp_core.proxy.VideoProxyProcessor', 'module': 'standard', 'checksum': 482873147, 'executionTime': 27.3, 'executionDate': '2020-06-02T18:21:16.755892'}, {'processor': 'zmlp_analysis.zvi.ZviSimilarityProcessor', 'module': 'standard', 'checksum': 1879445844, 'executionTime': 0.54, 'executionDate': '2020-06-02T18:24:06.247066'}, {'processor': 'zmlp_analysis.zvi.ZviObjectDetectionProcessor', 'module': 'zvi-object-detection', 'checksum': 3329037091, 'executionTime': 0.72, 'executionDate': '2020-06-02T18:24:21.556338'}, {'processor': 'zmlp_analysis.zvi.ZviFaceDetectionProcessor', 'module': 'zvi-face-detection', 'checksum': 2666795579, 'executionTime': 0.4, 'executionDate': '2020-06-02T18:24:39.951734'}, {'processor': 'zmlp_analysis.zvi.ZviLabelDetectionProcessor', 'module': 'zvi-label-detection', 'checksum': 2989691564, 'executionTime': 0.76, 'executionDate': '2020-06-02T18:24:54.626670'}]}, 'media': {'width': 1280, 'height': 720, 'length': 57.9, 'timeCreated': '2019-05-13T12:29:11.000000Z', 'aspect': 1.78, 'orientation': 'landscape', 'type': 'video'}, 'clip': {'type': 'scene', 'start': 0.0, 'stop': 57.9, 'track': 'full', 'length': 57.9, 'pile': '1UlthUdjDHKD-KtSvSzbkX4HwP8', 'sourceAssetId': '_dxQehYp1RIXVSizjHw3Qmzw908YV_IV'}, 'files': [{'id': 'assets/_dxQehYp1RIXVSizjHw3Qmzw908YV_IV/proxy/image_1024x576.jpg', 'name': 'image_1024x576.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 200822, 'attrs': {'time_offset': 28.95, 'width': 1024, 'height': 576}}, {'id': 'assets/_dxQehYp1RIXVSizjHw3Qmzw908YV_IV/proxy/image_512x288.jpg', 'name': 'image_512x288.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 70988, 'attrs': {'time_offset': 28.95, 'width': 512, 'height': 288}}, {'id': 'assets/_dxQehYp1RIXVSizjHw3Qmzw908YV_IV/web-proxy/web-proxy.jpg', 'name': 'web-proxy.jpg', 'category': 'web-proxy', 'mimetype': 'image/jpeg', 'size': 55920, 'attrs': {'width': 1024, 'height': 576}}, {'id': 'assets/_dxQehYp1RIXVSizjHw3Qmzw908YV_IV/proxy/video_1024x576.mp4', 'name': 'video_1024x576.mp4', 'category': 'proxy', 'mimetype': 'video/mp4', 'size': 15655241, 'attrs': {'width': 1024, 'height': 576, 'frames': 2896, 'frameRate': 50.0}}], 'analysis': {'zvi-image-similarity': {'type': 'similarity', 'simhash': 'DPHPBJPJCPEBPEPDIAFPCFNLIELLJIPPPPAPAPFHPFGPHPAPPEJPOOKPPBPKPBJEJFPPPPBFLPEPAHCPCDADDGPPPPPFPPGFBLEPMEPAPHPBPPMDPKGACDHPKEBPPBPOPPKPPPCPPCHOPDDGPAPPMBLIPPGCPDPHPPLEFOPPMPJPECPPPDPPHKEPLPNEBPPJPPNGJPFBPJPPPPEFPNHGPPPPPLPKBBPCPMBGPIGGBPFEPOPPPIFPAPJPPPNDPKCGACCPELPPPIPLPEACAJPPBJAPEPPNPMAPMJCBPPPCLPCBICALIPAPPBPPBPJIDPPCCBNAIDFPPPOBEPMPGGCOPAPPCPPPGLCFGCLPEFCPAFEMAHAHHAIDOIFHIEBGLPEONPDPPIBPIPGCPBPPPMMPPHPGNPPPIOCCPPIPHHBDHPAPPEPMKBAOPPPFCHFBGANBOPPPCPPBIPPDIBPPGBPPPPJHGJPBHBNLPIPFFPPCBPPOPHOPEGPPCHBPCIPHBPPGPEJPPBIEPBKCPFPPKPGPPDPEHKHFOCAEBGIOMMBPIPHDGGPMLCPKPPODDOGIPEPAPLPPHJFDPLHPPPPPNPAPKPBIPPJPMPPKKILEPDAGAAIPPPPAPPKPKLFOOGJHKPDPPMIEPHDMPEHFPPAEBIPPPHPAPLPNPDEEPPHHMPDPPPPFEKGPPCPPPPCPPDPPDPGHPPPPPPPCIPLPDGPAPFBOHPCPPPBPBPPNEPLHOPIAIPCPGOCBIPPFFPGPPPPDHKDPJPPAPFMPDPBPGPPDPAPPFJBPPPJBLPPPBPDPPPPOILLLBCPLINPFPHPCPPPEJPANPPKKPPNPICDPDPCPJPPEMIPPGPMMPPPAOEGCPPPAPODLJBLEFIJGPAEJMHPMPPDCGPGDPPPPAPILPAMPPDPPPAGACMPGOPCEPHPPPPPEGEPPGPBPPPPCPLPPILGEJIPAPHJDGBOPAPDPPBPPIHPPNJPDLPHPAIEPFOPPCHEPPPPPNPJJPGIPLPPPPPPFPPPBPHEIPNPOMGPPECPJPPPFEEPPAPLPPOFPPPOPDEPBPGDPCIPPLGMMCMBPBPPPGPPPPPHPBPPCPKHJLOAAAEPNDFAPMCPAOPCCIAPKFPEJBDBPHKLNLBMECPPBFJFHNCPEACPIFEBPPLPPKIMJCGPPEAPPDKDPPGPLDCPPPGPDIMPCEPJCJIGLPPPPGPJKDGDFPPNCCHOJEPDOLMPPHBGPLPPPIPPKKPIFFPAPCEEPPPFMIPPEMBBPPAEPBPAPPDHAPPAEOPKBKCBPCPOOEECFPPPPHKEGPPPPPBGKPPPFHBPDPHPLPCHMEEFPPPPPPBCGKAIPNDIMKKAGPJPPPPNKPPBCAPBCAPPPNPPPPJPPOPPCPDPAMNGPPPPMJPIPIHHPLPLPPOJDBPNPHPHFJPACOPJJPLPGJPFPPAPMAFEPPPJPAEPPEFPDNPPHPPPIPPBLPHEPPKJJPFPDPFPPCOFPDPPPPGPPHJDPFGPPPPPDPHPPPPDJNPPGBPLLPPFDFPKPANPGPCAPPIJPFPMEPJNKPPPMGCOKDDPKPPPKPGPOMAPDOPIPPHPPEPAPPNPPEJPOPPPPPOPPPPFPOEGCPPGPAPPPHPLPBAHMAELPPBMPFDPMPPPKPBDPDCMPALOPFIEAPNPPPBAKBHPPGABBHPPCGOJBKPJLPPJPIBCEPPFPPPNNFPPGPPPDFPPMPGAICCIPDPPICLOPPBBPLBPNKPAPPLBGBPPPJPCDPAIDDCNPKPNGBBPPCHMILCPCPBPFPPPKDANPPBGPPDOPLAPPLPPPMFPPPFCGNNPPAPPGPEPPPPCPMPPPKLPKPGPPDOPPPPKPPBPCBGPADGKGJKCCPKPFPAPJKPNPFPPPGIDPCPJAPPAHPEKBKGDBPLFFPBIOEFPIHBAPBDPGPMPPGPDCPPLBPJPPPPBCPDPPOPIPBPAFBLPOMMHEFPPPBBPPPCPGKLPPJAHGKPICBMPPEJJJMJEPPCPPPPAKPHPDPBPFPJIELBIAIPBEPADPLPPFAPBPPPAP'}, 'zvi-object-detection': {'type': 'labels', 'count': 2, 'predictions': [{'label': 'cup', 'score': 0.995, 'bbox': [0.531, 0.333, 0.763, 0.96]}, {'label': 'dining table', 'score': 0.855, 'bbox': [0.023, 0.759, 0.98, 0.983]}]}, 'zvi-label-detection': {'type': 'labels', 'count': 1, 'predictions': [{'label': 'beer_glass', 'score': 0.998}]}}}}, {'_index': 'imx29yosuvoa7grd', '_type': '_doc', '_id': 'dZvrvlkYTwLgIA3nuCin8igHnR3DMHuh', '_score': 0.0, '_source': {'system': {'jobId': '13b61d1e-5f71-12a6-9398-7eb3d1ce02c4', 'dataSourceId': 'ba231dbf-dd21-12aa-9c3a-765f5c657424', 'timeCreated': '2020-06-02T18:00:01.383262Z', 'state': 'Analyzed', 'projectId': '6a72b6c1-aa96-415b-8c2d-df355ba9c8f5', 'taskId': '13b61d1f-5f71-12a6-9398-7eb3d1ce02c4', 'timeModified': '2020-06-02T18:24:58.175105Z'}, 'source': {'path': 'gs://zvi-qa-private-test-data/zmlp/video/Pexels Videos 2364297.mp4', 'extension': 'mp4', 'filename': 'Pexels Videos 2364297.mp4', 'mimetype': 'video/mp4', 'filesize': 20501616, 'checksum': 2684497715}, 'metrics': {'pipeline': [{'processor': 'zmlp_core.core.PreCacheSourceFileProcessor', 'module': 'standard', 'checksum': 2178814325, 'executionTime': 1.47, 'executionDate': '2020-06-02T18:19:34.245636'}, {'processor': 'zmlp_core.core.FileImportProcessor', 'module': 'standard', 'checksum': 117837444, 'executionTime': 3.42, 'executionDate': '2020-06-02T18:19:48.867421'}, {'processor': 'zmlp_core.proxy.ImageProxyProcessor', 'module': 'standard', 'checksum': 457707303, 'executionTime': 4.32, 'executionDate': '2020-06-02T18:20:23.293059'}, {'processor': 'zmlp_core.proxy.VideoProxyProcessor', 'module': 'standard', 'checksum': 482873147, 'executionTime': 18.93, 'executionDate': '2020-06-02T18:21:20.091918'}, {'processor': 'zmlp_analysis.zvi.ZviSimilarityProcessor', 'module': 'standard', 'checksum': 1879445844, 'executionTime': 0.49, 'executionDate': '2020-06-02T18:24:06.739424'}, {'processor': 'zmlp_analysis.zvi.ZviObjectDetectionProcessor', 'module': 'zvi-object-detection', 'checksum': 3329037091, 'executionTime': 0.72, 'executionDate': '2020-06-02T18:24:22.280599'}, {'processor': 'zmlp_analysis.zvi.ZviFaceDetectionProcessor', 'module': 'zvi-face-detection', 'checksum': 2666795579, 'executionTime': 0.33, 'executionDate': '2020-06-02T18:24:39.953826'}, {'processor': 'zmlp_analysis.zvi.ZviLabelDetectionProcessor', 'module': 'zvi-label-detection', 'checksum': 2989691564, 'executionTime': 0.72, 'executionDate': '2020-06-02T18:24:54.627792'}]}, 'media': {'width': 1920, 'height': 1080, 'length': 37.801667, 'timeCreated': '2019-05-24T19:42:55.000000Z', 'aspect': 1.78, 'orientation': 'landscape', 'type': 'video'}, 'clip': {'type': 'scene', 'start': 0.0, 'stop': 37.801667, 'track': 'full', 'length': 37.800000000000004, 'pile': 'p0j72kREM7YYCO1TWvcVfbCYJVA', 'sourceAssetId': 'dZvrvlkYTwLgIA3nuCin8igHnR3DMHuh'}, 'files': [{'id': 'assets/dZvrvlkYTwLgIA3nuCin8igHnR3DMHuh/proxy/image_1024x576.jpg', 'name': 'image_1024x576.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 221949, 'attrs': {'time_offset': 18.9, 'width': 1024, 'height': 576}}, {'id': 'assets/dZvrvlkYTwLgIA3nuCin8igHnR3DMHuh/proxy/image_512x288.jpg', 'name': 'image_512x288.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 78114, 'attrs': {'time_offset': 18.9, 'width': 512, 'height': 288}}, {'id': 'assets/dZvrvlkYTwLgIA3nuCin8igHnR3DMHuh/web-proxy/web-proxy.jpg', 'name': 'web-proxy.jpg', 'category': 'web-proxy', 'mimetype': 'image/jpeg', 'size': 51652, 'attrs': {'width': 1024, 'height': 576}}, {'id': 'assets/dZvrvlkYTwLgIA3nuCin8igHnR3DMHuh/proxy/video_1024x576.mp4', 'name': 'video_1024x576.mp4', 'category': 'proxy', 'mimetype': 'video/mp4', 'size': 9882283, 'attrs': {'width': 1024, 'height': 576, 'frames': 1134, 'frameRate': 30.0}}], 'analysis': {'zvi-image-similarity': {'type': 'similarity', 'simhash': 'PPPPBGHPEFCBPPOPPDPJPJPCHCPJAPPPLIBPHBHMPBPPPPNGPPPPGDPPCBCPJPPGENOEEPGPIPFPJGJJPPHMHADPPBBGAPNJPCDFDPPLAPNJPCPPPPCCPFBPPCPLPPDKHPAPHPDCEPPKCJIPPGPENMPCHPPPBPDOPMPGPIPIGKHIPPEMPPPCPJPIFHBJPPJPJBPGMHKPNEMEPGGPPPAOIPKPIPEPDPOOPFOGPPBPPJPOPKPPPPJAPPAPCPIAPPBPIADFBPLNPOEMIPLBJPOPFEBAPBHPPNPFOFPPMKAPEKGPPFBPHPPAPOLEJIMBEPPBPPIPPPNPPAPHALINOMBAPADPPPPCPKDGMPPPPBPPPHGPPPOPPFCBAPPPOPHLPPHPPAPDFDDPHPKLACBPPBPPPPPPPPPCPPDJBPPPPGJPKPHPBJBLGPPAPKPJGOACBHMBHPFPPPPPMPPCPPMFFJFMFPPKPPPEGGHPLPPAJPPHIAPFPPPCPHEPGDJNAPPHIPEMEPACACPEPPPMOCDPCLJNPPPOPPAPPGGNFOPPBHELBPKDPPMPAEGPPGEBMMCPIPPPPIPEGPPHPHFPPPPJLDNKPPEBIOGEHEEPLPNPGOPPAPEGPPEOHPPMPDAPLMBFCLCIPMPCPPPGPBMAPGPEPPPCDBMCPCPILFADAPBKPEKGNHLBPPPPKAOPGKCIDGPEJPDDPPPGPAPBPGPIEPJPDPPHENCPPJPPOOHKLPPPAENAKPGNABJNDHPPGPJPHPHPMFMCPDHPBDGPOPADKPIEABPPPJDNCIPAIIJLPMPPBJPHEAPPPPLDHPPAPKPKPCKIPNPNPEAOPPGPPPPPNFPPDBKJEABCKPFPIPPBPGGPJPHBBNPKJLCDIGGMJAIADAPFIPLPNPBPJPLPBPDBGPCIBJBBLFNPGPAIEBGAIHPPIPPNBJDHPPEGCPAMPPGPJCFPLPLBPPJMEPMPBPEGMCPDPGPPMJAIPILPDPKOGPPPICDPDPPGJNPEPPAJHPLCDPKNDLJPLPFAGPKMPOPPGPPPCBNPCPOKPPHJFPAPLLAKJBLAMMDPPGPEMENOPCPPJPLPCPNABKPPPPPLCHPOPABAPAPPPGFPPLPAPHCLPCPPBGFEPPPDBCHLOJDMDPOPPPPPPAPENPKNJNEGDCIKGPPGCDFHACMCPPPDEPLIPPMBPLIHKMPGKPDEPPBGEPBJDPEOPFMDKPBHGPPBPDMGPCCHHFMLOCDPPPPPAOJLPFEPPPPFKPINPPJPPHPDPPLLPBJPPDGDGCPOPOBHNMIBEECOAKDMPELEPPBJFDKPPDBIPKMGGPMPLPDPEEBPBPBPPOPAPDDBPGKMHPNPPIAGHIPMPPGPPPLHEPPPBAMOPKHOMPPPPEPPPCBDPJPEAPCOFJNPLPOPKHPGCLCGMCLPHPHBDBPBPPCBPPFCAPDPPAALPAIPPBAPBEPGPPBMPNGKOFACPAPCMKAPPPPPIPADPPFHPEFFEKPLPEPGEECJPPBPPPPDPPIMPKNEPMPMCPAPPJIMLPFPPPCPPPPALIPPEPPGGPPEPPPCPOGPBMJMAFPPFPPPLEGNPFCPPHEECFIAPPPPCIHAIPDPPPBPDHCCEBPPCFPFPPIKKPAPANPPCJPPAAAPPPPEMPNDLPDEOINPBPIAPPMPJHJFJPGPEGHBEMPFPGNPOPMPDBPPPNBGPCCGEFKPPKAMPPBKPPPLPPBBPNLJCCCPPPHPPPCCPPGCMINGLPNHBPBIPBPPPPPFPPPPFFAHCJPPDCPGBPPEPFPPKPHPIMPDAIMMPBJGMDCPEAPCPJMECPGKPPAKIMPFHCNBDPLAPHPPPHJPMAHPGHFPAPJCPPAAJPBINPPNPPELPNBBPPPPPIIPDDPBPPAPOPEPEPPOPPCDPLCAIPMPAAPAMNGDFIPPDJDDEGJPPPPNPDNFHPDPHPPPPNPKCCFBPFPPPGPOANPCPHEBEEJCPLCEFAELFCGPPPKEOGBPEGGLPDFEHPCPHKHPJNCPPPPPLPPPPPIPBBKPJOAELPEPJPAKPPPGPCHP'}, 'zvi-object-detection': {'type': 'labels', 'count': 3, 'predictions': [{'label': 'person', 'score': 0.998, 'bbox': [0.188, 0.271, 0.762, 0.995]}, {'label': 'person', 'score': 0.99, 'bbox': [0.725, 0.366, 0.992, 0.981]}, {'label': 'person', 'score': 0.933, 'bbox': [-0.002, 0.311, 0.205, 1.003]}]}, 'zvi-label-detection': {'type': 'labels', 'count': 0, 'predictions': []}}}}, {'_index': 'imx29yosuvoa7grd', '_type': '_doc', '_id': 'lehxxRoIXViucqX41IntDzcGdnhzga-i', '_score': 0.0, '_source': {'system': {'jobId': '13b61d1e-5f71-12a6-9398-7eb3d1ce02c4', 'dataSourceId': 'ba231dbf-dd21-12aa-9c3a-765f5c657424', 'timeCreated': '2020-06-02T18:00:01.379971Z', 'state': 'Analyzed', 'projectId': '6a72b6c1-aa96-415b-8c2d-df355ba9c8f5', 'taskId': '13b61d1f-5f71-12a6-9398-7eb3d1ce02c4', 'timeModified': '2020-06-02T18:24:58.175519Z'}, 'source': {'path': 'gs://zvi-qa-private-test-data/zmlp/video/Pexels Videos 2126081.mp4', 'extension': 'mp4', 'filename': 'Pexels Videos 2126081.mp4', 'mimetype': 'video/mp4', 'filesize': 34826154, 'checksum': 942296040}, 'metrics': {'pipeline': [{'processor': 'zmlp_core.core.PreCacheSourceFileProcessor', 'module': 'standard', 'checksum': 2178814325, 'executionTime': 3.77, 'executionDate': '2020-06-02T18:19:26.965708'}, {'processor': 'zmlp_core.core.FileImportProcessor', 'module': 'standard', 'checksum': 117837444, 'executionTime': 5.12, 'executionDate': '2020-06-02T18:19:45.446754'}, {'processor': 'zmlp_core.proxy.ImageProxyProcessor', 'module': 'standard', 'checksum': 457707303, 'executionTime': 4.95, 'executionDate': '2020-06-02T18:20:19.617235'}, {'processor': 'zmlp_core.proxy.VideoProxyProcessor', 'module': 'standard', 'checksum': 482873147, 'executionTime': 16.35, 'executionDate': '2020-06-02T18:21:10.303020'}, {'processor': 'zmlp_analysis.zvi.ZviSimilarityProcessor', 'module': 'standard', 'checksum': 1879445844, 'executionTime': 0.47, 'executionDate': '2020-06-02T18:24:05.710085'}, {'processor': 'zmlp_analysis.zvi.ZviObjectDetectionProcessor', 'module': 'zvi-object-detection', 'checksum': 3329037091, 'executionTime': 0.74, 'executionDate': '2020-06-02T18:24:20.839937'}, {'processor': 'zmlp_analysis.zvi.ZviFaceDetectionProcessor', 'module': 'zvi-face-detection', 'checksum': 2666795579, 'executionTime': 1.11, 'executionDate': '2020-06-02T18:24:40.541844'}, {'processor': 'zmlp_analysis.zvi.ZviLabelDetectionProcessor', 'module': 'zvi-label-detection', 'checksum': 2989691564, 'executionTime': 0.48, 'executionDate': '2020-06-02T18:24:55.012309'}]}, 'media': {'width': 3840, 'height': 2160, 'length': 15.015, 'timeCreated': '2019-04-11T03:39:36.000000Z', 'aspect': 1.78, 'orientation': 'landscape', 'type': 'video'}, 'clip': {'type': 'scene', 'start': 0.0, 'stop': 15.015, 'track': 'full', 'length': 15.02, 'pile': 'LrJD1U9VTQjLMySULjvs7DmQ3z0', 'sourceAssetId': 'lehxxRoIXViucqX41IntDzcGdnhzga-i'}, 'files': [{'id': 'assets/lehxxRoIXViucqX41IntDzcGdnhzga-i/proxy/image_1024x576.jpg', 'name': 'image_1024x576.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 256542, 'attrs': {'time_offset': 7.51, 'width': 1024, 'height': 576}}, {'id': 'assets/lehxxRoIXViucqX41IntDzcGdnhzga-i/proxy/image_512x288.jpg', 'name': 'image_512x288.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 84774, 'attrs': {'time_offset': 7.51, 'width': 512, 'height': 288}}, {'id': 'assets/lehxxRoIXViucqX41IntDzcGdnhzga-i/web-proxy/web-proxy.jpg', 'name': 'web-proxy.jpg', 'category': 'web-proxy', 'mimetype': 'image/jpeg', 'size': 60570, 'attrs': {'width': 1024, 'height': 576}}, {'id': 'assets/lehxxRoIXViucqX41IntDzcGdnhzga-i/proxy/video_1024x576.mp4', 'name': 'video_1024x576.mp4', 'category': 'proxy', 'mimetype': 'video/mp4', 'size': 6350201, 'attrs': {'width': 1024, 'height': 576, 'frames': 360, 'frameRate': 23.98}}], 'analysis': {'zvi-image-similarity': {'type': 'similarity', 'simhash': 'JGPAPPPFGMDIPPGHPANCHAJPEPNHFPPMJDCPPDPIPBPOPPGPJCPIEJNMPIPPAPPFDPPJPPMPFBPMCBPPBFDJBJMPADPCFAGJPPPBEBPIAPPPPPLDMDPFACGPPPPPHHDOBPOBAKIBPLPPONMBPDPBPNPDJDAJHDPOALPFPPBMECGHMDDFPKLEHPGPGPDIIOHJAADPEJELPDAPPPGPGBBPKGEPGFLPPBBIAFPAPNKPCEEPPPBBLEECAAPPPPPFPMIPDIHNPPPKBDPPPONLBFMDGAPEDABJPMPDPELAGPLEPPLCPFCEKIJOCBDMPPEGOPPGDPAAEPOPPPPDPGEPBBOPPIGPHIHOHNPEFCPPPPAKPPPPAEBIMDEPBGPLGBENLKAPJCANPPJAHGPMJPAPPJHPKPPFPPPCELMPFGPAPAFPBHBCFHMGFEPJCFKNCPPEKFPEPEOACNPPPHPMPHPPGPPCDHAPPPMOPPPPLJKKFIPEBADLPPPPNPPPKPPPPKAFFDNBAGPFCAPNPPHPPPCOGIGIPPLFCBAPFFIPOPCJPPNIBPFPPNBPNIIOGPPNIGHJPPIDPEPNPGPBPLKPGGOPDGDPNBIIKPAPPMPBPFJPPJCAJDPPPGFPMPIPFNIHHPPPAAAPEPBFCEHDBPKCOOFCPAGPMGPAPKCPPEBGBPAPPIGFPGPHMPLIJKJMPAIPPFCLAAJLJNPPBIIPBHGAAEPAAPKBPPCPDLIIBJIPPGCEGPPPPPOHAPLPPKAPPPMPPOCCEPGNNPDCGGPPKEHGGPPDOFPGIPPFPEEPHMPFPMGFPPPPJGPPPNPPPNPPPPMBMNEOAPPHFBNPFPACPOFGIPPPLPPGBMDPGCPCIPBAMCJFFCODCOPPKDPPJPCPDPEJPFGAPPFAPOPHIHLPBPPPPBCGBBKAJCAPGPPEDDPKPPGGPPPIGEPDFIJPCIBPIBPPAKPMBCABEBEAMPHPAPIBMIAPBAPGBLPCFPNHBHLPFPPPDAGPKBPGPPLFHCCLFPLJPPKBBNAIPIDPPHMPPPFGPPEPPPGPPCPEPMHJJPCGPPIPBCGPKPPPKPGPDKALPNEPPPPFAKLBCIMPPPEPELPCPPPBPJPPPPBGNKPCPGAPPMBMMEAIHMHDPMHCBNPPBNPPADDBPAPFLDPPPCBGPCGPPPFPNKPPPGHBGNFHCEFPFPOPPDAFKEPBPPPPPHCADNEJAPAACPPEPCEBGGPJFPPIPMPPDDADCFEACKPPFPFPLPPPICADECGHFPHECIPPOPDLHLPPPAPIIPDEAOBPMBFFFFJPPPLPGCPPHKOPJPPBPPABGPLPJEPPGPPBDEFCKMGBPPAHAPCKHKGPKINPGIEPEPIPHIIAEPDPLIPPIEPPPPKPGGCFPDJPPBAIEPPPPAJPPIPEALPPECKHBDBMDIJHPPGDPMPGPAFPHPPOAPCJPHDGAFIPPPPPLCPOGPPCAEPBPEAELKECGBCDPDKPPGPGPFLPDJKKPEBPPJJPEEPBHBPPPOPEPOPPPPPGPMEPEDPCPGPCPPAPEFBFPPPGPCKPAHHMHPPCPPLGPPBDEBLHEKPOPCPKPNPBNPPJPLPCBPPHPPLPBPHGPPMAOKAIPMEHFPDAPJPAAOPPPDHPAINPDALMAPMFPDIFACADFHKAPPPPPAAPPGAMGPCOEOOPPPCHKGJPFDPJPPNDDELPOILEPPBAGPBPPHFEPPPEPOPEOMPCPPAFAAPCPBAPCPHPPOGNLAPJJPPMEPDJDPKPHAAJGPPPCAFOBPPCICFCAABLMPPPPNCGIBPKCPPEPPLPPPGPPHPPPBMPMACPGPBHBMPFMPDGGNPGEFGAPPPGIBMBJPCPIBPKPKDDNAEOPPPEGAACPMJAFPADCAIOBPALPMPCFPHPKPPPLNFPPPABIMAKKHOIPCENAPDPAJJJLPIHPPPPPFDPLPPFPEHPPPFIPNEPBPPPGPPKNBKCGECPPGKAHPGPPHAPPEPPJOMBPDPPPFPPNDHJAECGLBHGGFBCPNFBAPDPPAPDFDFPHPADBPBFICKHOAAEGCDP'}, 'zvi-face-detection': {'type': 'labels', 'count': 1, 'predictions': [{'label': 'face0', 'score': 0.77, 'bbox': [0.083, 0.554, 0.324, 1.082], 'simhash': 'MKIPMQNOKMMPPMLSPNLMMLLMOMONOMOLLOLMMIKNPIOKNONNMOPOQKIKLMMNPPLNHPNMOLLLLQMPKLKLMKKLNJQNLMLLMNRONIIRNNPMKLLLMMOKJJNJLLLOQQKJKKMONKNLLJILMLPRPKNLPKLLPJONNNPINPOPMNOPQOLLMMLSNLRMLMKOFNHLLNMLJLMLQILKNMLMOMMKNLKLHNJMMOOPKNNJMJKLNMNJMOOLPJMONKNLMNOKSNPOMMHMNNPLQKMSLMLHLHKLMKKNNMOLLKJLMKNMKJKMHLLNNJNNLONOSHQLOMKKMKMLKLMMKNOKQJHKMPLNMONNKNNOPKLONISLNKPNLMKMMNPMRNKLNLKIKOKOKJONMNNNJPKLHPLLNJLLKLNQJMIKMPPPNOMMGLRMLIOJPMKJMNPIPNKOMMPOOJLNLKPLONMIJMLQHNNMPLLJOMMLNOKQJNNOPNMJOLQMMOIPOMPLMKNKLMJLMJLOMLRLMKOPNLMKLPQNLONM'}]}, 'zvi-label-detection': {'type': 'labels', 'count': 2, 'predictions': [{'label': 'lakeside', 'score': 0.676}, {'label': 'valley', 'score': 0.158}]}}}}, {'_index': 'imx29yosuvoa7grd', '_type': '_doc', '_id': '_rOyVOtsK5Fu1hyjjcOj9fXCtvWdPNT5', '_score': 0.0, '_source': {'system': {'jobId': '13b61d1e-5f71-12a6-9398-7eb3d1ce02c4', 'dataSourceId': 'ba231dbf-dd21-12aa-9c3a-765f5c657424', 'timeCreated': '2020-06-02T18:00:01.382686Z', 'state': 'Analyzed', 'projectId': '6a72b6c1-aa96-415b-8c2d-df355ba9c8f5', 'taskId': '13b61d1f-5f71-12a6-9398-7eb3d1ce02c4', 'timeModified': '2020-06-02T18:24:58.175661Z'}, 'source': {'path': 'gs://zvi-qa-private-test-data/zmlp/video/Pexels Videos 2334654.mp4', 'extension': 'mp4', 'filename': 'Pexels Videos 2334654.mp4', 'mimetype': 'video/mp4', 'filesize': 23821205, 'checksum': 2661581411}, 'metrics': {'pipeline': [{'processor': 'zmlp_core.core.PreCacheSourceFileProcessor', 'module': 'standard', 'checksum': 2178814325, 'executionTime': 1.94, 'executionDate': '2020-06-02T18:19:34.165947'}, {'processor': 'zmlp_core.core.FileImportProcessor', 'module': 'standard', 'checksum': 117837444, 'executionTime': 3.62, 'executionDate': '2020-06-02T18:19:48.949229'}, {'processor': 'zmlp_core.proxy.ImageProxyProcessor', 'module': 'standard', 'checksum': 457707303, 'executionTime': 5.01, 'executionDate': '2020-06-02T18:20:24.157034'}, {'processor': 'zmlp_core.proxy.VideoProxyProcessor', 'module': 'standard', 'checksum': 482873147, 'executionTime': 22.1, 'executionDate': '2020-06-02T18:21:32.415066'}, {'processor': 'zmlp_analysis.zvi.ZviSimilarityProcessor', 'module': 'standard', 'checksum': 1879445844, 'executionTime': 0.53, 'executionDate': '2020-06-02T18:24:08.800948'}, {'processor': 'zmlp_analysis.zvi.ZviObjectDetectionProcessor', 'module': 'zvi-object-detection', 'checksum': 3329037091, 'executionTime': 0.72, 'executionDate': '2020-06-02T18:24:25.187493'}, {'processor': 'zmlp_analysis.zvi.ZviFaceDetectionProcessor', 'module': 'zvi-face-detection', 'checksum': 2666795579, 'executionTime': 0.43, 'executionDate': '2020-06-02T18:24:40.754787'}, {'processor': 'zmlp_analysis.zvi.ZviLabelDetectionProcessor', 'module': 'zvi-label-detection', 'checksum': 2989691564, 'executionTime': 0.67, 'executionDate': '2020-06-02T18:24:55.294124'}]}, 'media': {'width': 3840, 'height': 2160, 'length': 18.026667, 'timeCreated': '2019-05-18T19:07:53.000000Z', 'aspect': 1.78, 'orientation': 'landscape', 'type': 'video'}, 'clip': {'type': 'scene', 'start': 0.0, 'stop': 18.026667, 'track': 'full', 'length': 18.03, 'pile': 'yWHo2dck7U5D6C7z5F1fDQ_0kUc', 'sourceAssetId': '_rOyVOtsK5Fu1hyjjcOj9fXCtvWdPNT5'}, 'files': [{'id': 'assets/_rOyVOtsK5Fu1hyjjcOj9fXCtvWdPNT5/proxy/image_1024x576.jpg', 'name': 'image_1024x576.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 292453, 'attrs': {'time_offset': 9.01, 'width': 1024, 'height': 576}}, {'id': 'assets/_rOyVOtsK5Fu1hyjjcOj9fXCtvWdPNT5/proxy/image_512x288.jpg', 'name': 'image_512x288.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 93527, 'attrs': {'time_offset': 9.01, 'width': 512, 'height': 288}}, {'id': 'assets/_rOyVOtsK5Fu1hyjjcOj9fXCtvWdPNT5/web-proxy/web-proxy.jpg', 'name': 'web-proxy.jpg', 'category': 'web-proxy', 'mimetype': 'image/jpeg', 'size': 73288, 'attrs': {'width': 1024, 'height': 576}}, {'id': 'assets/_rOyVOtsK5Fu1hyjjcOj9fXCtvWdPNT5/proxy/video_1024x576.mp4', 'name': 'video_1024x576.mp4', 'category': 'proxy', 'mimetype': 'video/mp4', 'size': 3112586, 'attrs': {'width': 1024, 'height': 576, 'frames': 540, 'frameRate': 29.97}}], 'analysis': {'zvi-image-similarity': {'type': 'similarity', 'simhash': 'BPIGPPPKCIAAPPPODDJMPAAMPPPPFPPPKEKPFDICPLOMCPLPIPOMPDNPLIOEKLCNPPEPPPGPPEPJHJPPCNDANPPPPPPBICPPGPGPNEPHPPPPPKFPLPPAOPLPPFHCPMCDGPCPFBFDMNPPPIBPPFPEPKPBPDDMPKMBPPPCKJEJOBDFFPGKPLJLGPGHLPBHPPNEINCPBPGOKMDJPPAPPJAPPCBPPPJBIFPLFAFAHBPPCPFPPPBKPIPIEPPPPPAOPEJKABKPPPPLPLPPPMPHBBFHAKBPGPHFBPFPPBCALPPBDPKLLHMPPBFKACLGBNPGPJMABPJHCBPPNJOBIKPPJBPNPIMPIHPOEPMAADPPIPBPPPHPABIELBIECFPLEGGPPKNPCBPFPBDOCPPNDEIMLPJLPGPANPGAPFFNMKHJPDEHAEBBPCBDCDPPFJCBJOPBFAPDPGFPEPPEPIPDPHPMPPMPJKGPPFONPPBMPIPBJDPPPHIPPOPPPPJPMEPPGPFHMIPAMGPPPAPPPMGFFPKPPPMFPNPPPPONHHOOPNPPLIPPIDDPPDNPPELPPMBPHDCDPGCKMIPPBPFCPPCGCMDPPPJPIGJDAPIPPBPJIBFPHPDPEAMGPIHCFPMPEPCCHMJGFJCOBPPEFPAIKKCPPPGFDHOKPDPAPBPPPAIDDMFGPPPPLPPANIPLPPGFMPAJPEANCPPGHEPHPLPPAEJIHPPEAPECPPCPIEAFPJPPPPAHFPKBPPCLPPGPPPBPMIPDNCCPPMPPPMFLDCPPEPGLLHPAPAPBPPGPPDPNGPPAPPHFJPPPMEPMPMEDPDPFJBJBPPGLBPPGPNPOPPMPPPBJPFAPPGPMPPPPDMCHPPPPDBGBPNPCOPPPBKLDJHPNPFFIPFFFPKHEPPCFAPPCHPMNBCDGHLFPPPEMPPPBMPHLPPPBEPPALCPPPJNPILPKPKCPGLDCMGABNALAAPBHAAPPPJAEEJPNFPFFFCPOPFCPPIMPIBDGPPPHPPPADCPPPPNPMECPEFGHPLKJPPEPPGPOGPPPPPPPPMPFPFDPPHCPPLCPDADBLPPPDGMPEPAENPCPPPPJCLPMCLKPMPNPCNHPPBICGHFMOPJLIPPFLPDPOEIGLODHIGJLCADKMELBFPPHDLPIPDPOPKPNINEEPDPPPPPPPPGPPIHPAIPPPBJBDJAPHEIBPJPOPNPEPBFPEBFPGPIABPBIPBKCKIPBPPLPPJDLCEAPNCPPILLIPPPNLPPPOBLMMHKECMGDPPCBHPBPAPDAPBPAGPEDBIHNDPPNEPCPPPPPAKPPBCPBFNPMPJPBLMPDJBEPPNPEGOPPLBDIPDEMPPPPPPBMEPPPAIAPJGOPPPLBEPGPJPPPAPPPIFLPAFPPFPPPPBHJKMPNPCMBPCPLCMIPNJECCHAGBFHIPPINEOPNJEAPPPHPPPAMPBPJCPDPPPJPHANCHGPMFLPHEFPAPPHDCCPMPEEJPKLPPCPPBPLPPLCPBEPMGHEPFAPPPCPIPLMPPPHLFEJPAPBGPGJCEPPPNLKJCLDPPGINJLIMHPPPPPCHEPDDMBPLBPPPPCPGPHPFPPPGBPPGOFPIPPPEPPPPPPJIMGJIMPLPJOEDPKEACPPMPGAPABFKBAJPBDPPPKFPGPDAGFNBPPPPDDEKPCAPJPBNIJPPAPPPBCBPFPNHPGLPLAEKPLNFPCBKPPKPJPACKKPLPKPLBPMGPPOPGLNJPAFPEFPCPJFPNCPPCODPFPPPPPJJGLLGFPPPPCEPBPPMDDCBKFNGPPPKPEAKPDPPAPPDPHEPPPBKJCPJPIMPEJPGKIDEEPJMBMPCPPPCMFIAPPPPBPOAPPHPAGPPPPPPLFPPNHMPPAMIPPIPPPECPDIHIPMPPPPCFPDPPPPFLPOBPFAPAPCPPBGNDBDCJHIMGFHPDPIPPPOPPPPFPPBDBBCPMPAFPPPMAPPPHGPBPPGAMPKIPKPGJEPPPPFJMIPPPBPEPPPEPPPHBIFNABAJPOPOFCAPFBHABJPPGOPBPFEFAPAMDPHAEIPJPPJPPPDK'}, 'zvi-label-detection': {'type': 'labels', 'count': 1, 'predictions': [{'label': 'alp', 'score': 0.765}]}}}}, {'_index': 'imx29yosuvoa7grd', '_type': '_doc', '_id': '2fVl8JAbp2RO8RgiuOHuZ5XWB3hObgTv', '_score': 0.0, '_source': {'system': {'jobId': '13b61d1e-5f71-12a6-9398-7eb3d1ce02c4', 'dataSourceId': 'ba231dbf-dd21-12aa-9c3a-765f5c657424', 'timeCreated': '2020-06-02T18:00:01.380879Z', 'state': 'Analyzed', 'projectId': '6a72b6c1-aa96-415b-8c2d-df355ba9c8f5', 'taskId': '13b61d1f-5f71-12a6-9398-7eb3d1ce02c4', 'timeModified': '2020-06-02T18:24:58.177020Z'}, 'source': {'path': 'gs://zvi-qa-private-test-data/zmlp/video/Pexels Videos 2156021.mp4', 'extension': 'mp4', 'filename': 'Pexels Videos 2156021.mp4', 'mimetype': 'video/mp4', 'filesize': 107399235, 'checksum': 2836408201}, 'metrics': {'pipeline': [{'processor': 'zmlp_core.core.PreCacheSourceFileProcessor', 'module': 'standard', 'checksum': 2178814325, 'executionTime': 7.86, 'executionDate': '2020-06-02T18:19:31.059244'}, {'processor': 'zmlp_core.core.FileImportProcessor', 'module': 'standard', 'checksum': 117837444, 'executionTime': 7.53, 'executionDate': '2020-06-02T18:19:49.206011'}, {'processor': 'zmlp_core.proxy.ImageProxyProcessor', 'module': 'standard', 'checksum': 457707303, 'executionTime': 4.31, 'executionDate': '2020-06-02T18:20:23.927677'}, {'processor': 'zmlp_core.proxy.VideoProxyProcessor', 'module': 'standard', 'checksum': 482873147, 'executionTime': 83.45, 'executionDate': '2020-06-02T18:22:28.389222'}, {'processor': 'zmlp_analysis.zvi.ZviSimilarityProcessor', 'module': 'standard', 'checksum': 1879445844, 'executionTime': 0.54, 'executionDate': '2020-06-02T18:24:12.420758'}, {'processor': 'zmlp_analysis.zvi.ZviObjectDetectionProcessor', 'module': 'zvi-object-detection', 'checksum': 3329037091, 'executionTime': 0.76, 'executionDate': '2020-06-02T18:24:30.300923'}, {'processor': 'zmlp_analysis.zvi.ZviFaceDetectionProcessor', 'module': 'zvi-face-detection', 'checksum': 2666795579, 'executionTime': 0.53, 'executionDate': '2020-06-02T18:24:41.962645'}, {'processor': 'zmlp_analysis.zvi.ZviLabelDetectionProcessor', 'module': 'zvi-label-detection', 'checksum': 2989691564, 'executionTime': 0.58, 'executionDate': '2020-06-02T18:24:56.088320'}]}, 'media': {'width': 1920, 'height': 1080, 'length': 189.063875, 'timeCreated': '2019-04-13T14:09:00.000000Z', 'aspect': 1.78, 'orientation': 'landscape', 'type': 'video'}, 'clip': {'type': 'scene', 'start': 0.0, 'stop': 189.063875, 'track': 'full', 'length': 189.06, 'pile': '-DPu2LoYC_U7wdHE69giEhwRnyc', 'sourceAssetId': '2fVl8JAbp2RO8RgiuOHuZ5XWB3hObgTv'}, 'files': [{'id': 'assets/2fVl8JAbp2RO8RgiuOHuZ5XWB3hObgTv/proxy/image_1024x576.jpg', 'name': 'image_1024x576.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 358244, 'attrs': {'time_offset': 94.53, 'width': 1024, 'height': 576}}, {'id': 'assets/2fVl8JAbp2RO8RgiuOHuZ5XWB3hObgTv/proxy/image_512x288.jpg', 'name': 'image_512x288.jpg', 'category': 'proxy', 'mimetype': 'image/jpeg', 'size': 114223, 'attrs': {'time_offset': 94.53, 'width': 512, 'height': 288}}, {'id': 'assets/2fVl8JAbp2RO8RgiuOHuZ5XWB3hObgTv/web-proxy/web-proxy.jpg', 'name': 'web-proxy.jpg', 'category': 'web-proxy', 'mimetype': 'image/jpeg', 'size': 94246, 'attrs': {'width': 1024, 'height': 576}}, {'id': 'assets/2fVl8JAbp2RO8RgiuOHuZ5XWB3hObgTv/proxy/video_1024x576.mp4', 'name': 'video_1024x576.mp4', 'category': 'proxy', 'mimetype': 'video/mp4', 'size': 94222851, 'attrs': {'width': 1024, 'height': 576, 'frames': 4533, 'frameRate': 23.98}}], 'analysis': {'zvi-image-similarity': {'type': 'similarity', 'simhash': 'GPJAPPPOMGDBPNPPMEPPMCPHMDPLCPPPPCEPHCHPPEPPPPBPPIPINBNPPEEPPFEPPKJICPBPNBMLABCPBHPFAKPODIKCPJGPPOPAPDPDPBJAHAGPFHJDCEBBPBPPGMBFHIPBELMKEPAPDIPKPDPEPPCIBPGAPDMAHPEIDPLPFPDBGBKPPDPPIPDPOCAAFPJGENFAAECPPINEPPPKPCNPGLNPMPHPHCPPHPPPPOELADEPPPPEKPLJEPBPPHKOLPNPDGHAPPNPPPPPPPLAAGPPCKGLGPAPCNAOGKPGPFLNMPADNKAEPGCPPCPDJPHDMPBOPPAPBPEIPPPPNCAPBAGNPIHPPPMLCIPPEEPFPKEPPLPEBMBPOAPPAIICIEAPDJDPEIDFLBBIEJGPBADICFAEFIPBPPJEMPBPPEMFPPKPBGHPFEHBCAPCPIEPAMJHNCKPPPPPLPPPEJPPECPCCHEEPBJPMILGMIPOAPPGLIPBLNMPPAPPPHPOGPIPPPEOHHDMHAJHKCPDPPPAPPKCAEHPJOGMHPFBPIHAPDPICODOBPHPKFPFCPOMOPDHBOIPPKBIGHBPFAPAPJAGEFBMMBJPFLOBNPEPPHPNPFPCPJMPIOPPPPCEPPPIMPAMPFNPBPMPPHGNBODGFJCOPKBDPPPCPNPDPPPEPGEHLLFPPPPPPLPPLDIPLPPPELALPPBDBIPEPKPEFBPPNPIJDDDGPPPPFPMAIPPJJNPPPPGAHFAPIPIPCLHLPKPBPPPMPLPENPJHFPPANDAPLIKPBGPPAHPPPPPPPPBAMPPPFEOOFPPPKAAPEPHPEONJPPPKAPIOBHGHNMOACPPPPJEPAGIKPPPPGAEHCJFPNPPJPEJPFPPCDPMEEAFPOPMLIPIMDNPCKPPMPMMNBPOPCPPLPDPFEKIEDHJEBPNEAPBAPLOIPKNPCJPLPMPNFIJGPHPHAEIIOODCKPJAEPPHAPEPPEIMGKMPPIPAMNPONDCEIJPPFPDPKMPIGPMPAILJGKHDPPPNJPHCPJAPGHPPPPEEPBLFPEPMKLPAAIPPGGLPPPGPFAECDFPPEFHPLPHBCHJPDPOBCDPPPPAKPGHNBPLAFPIABEIMBPBPOEPDPAFEHBFPMLLNBPPNKPPABAALIJOPANGBMJPIGDLHGAGIGJPPCMJDDEJEICHPCPFOGFPJNIPKGBPODAPHFPLIJOAHPLEPAPJPDPMJPLCACPFPACCFDFCPCPIEDPAGBPPPBPPAPHBPLNBBPBAPOPJPOBPDPJPPAPOPPGPHPPAPCIHPBDAPKCDPPFEPOCKOIPMJMIPIPPNCEPHPPKMJJKNMLOFPAEFDPLPIOPJBAPJDPDPDHGPPCPOPPGPEPKALHFLCMEPDPPPDCGPNPIHPKOCKHPDBPPFMPOGPADPPAPPPGGKPCPHCJPAFPLJCJBDLDPCBIPLPBDBPFGKPGPJPPDEPPPIIGOPEDPJGPDBAKPAPJFPCPMPHPPPFEPKPAMKPPIPAPAMIPFCPPGPAPPDPKOHFPPDPDCNOEOPEBPPLPJPFHFPLFBPPADPIEPDLPHJEBBGELAPKLOPPPGMNPGPDBPFBHLPDDGPPNLPPGPBPPPPNBCPPPFCPPLBACCPNAPPPPCPNPPPPFPDPDHKEPDBCFJJCHEFFNIPPCAGPPLIMDBNGAEAPEPPFOFHPPPEDPDOODCFNPAPIPIAEPPPNPPPDIPBLPFPFMCIPPICBIFDDOIFPHJPBPDPFPGPBDPBHFPDDOPOPEPFGKFPPBJEPFMGIEPBHEDAEPGPPCPOHHOHACPDPNHDPPPHHOBBJELFPPELEEJPAGPPPJIIGKPPKDPENJPPFDPIOFPCPEBPPMPPPPDIKKAPCPFNLBEEFMMHCPPLDBPNLNPPGCNAPFPKPPPBGPPACIPDPPIDADAMGBJPPHJBPFAFBPPNPLPGDOPFCDPPDLCEABDOBEPLPHPMGINBIOIPEPJBPODEPPINKPPHPIIDBPPIHGDNPHOANBCFMPHFLHPCPJBPDPMKPFMBIFPEPAJBHPANLGPPFBPPPDJCP'}, 'zvi-label-detection': {'type': 'labels', 'count': 2, 'predictions': [{'label': 'coral_fungus', 'score': 0.258}, {'label': 'fountain', 'score': 0.168}]}}}}]}}  # noqa

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        task_id = '5cd3eedf-ab69-1a12-8017-3e6d2d97ef02'
        url = reverse('task-assets', kwargs={'project_pk': project.id, 'pk': task_id})
        response = api_client.get(url)
        assert response.status_code == 200
        assert response.json()['count'] == 357
        assert response.json()['next'] == 'http://testserver/api/v1/projects/6abc33f0-4acf-4196-95ff-4cbb7f640a06/tasks/5cd3eedf-ab69-1a12-8017-3e6d2d97ef02/assets/?from=50&size=50'  # noqa


class TestTaskErrorViewSet:
    def test_list(self, monkeypatch, api_client, zmlp_project_user, project):
        def mock_post_response(*args, **kwargs):
            return {'list': [{"id": "d5ffb9ba-5822-11ea-b3c8-0242ac120004", "taskId": "d4fffcf9-5822-11ea-b3c8-0242ac120004", "jobId": "ce4df7e7-5822-11ea-b3c8-0242ac120004", "dataSourceId": "ce46f306-5822-11ea-b3c8-0242ac120004", "assetId": "oh4g6WGPFqlQOShzVdmpr2hugGu1WuEh", "path": "gs://zmlp-public-test-data/corrupt.jpg", "message": "ZmlpFatalProcessorException: ('Failed to pre-cache source file', ValueError('Anonymous credentials cannot be refreshed.'))", "processor": "zmlp_core.core.processors.PreCacheSourceFileProcessor", "fatal": True, "analyst": "not-implemented", "phase": "execute", "timeCreated": 1582671723066, "stackTrace": [{ "file": "/usr/local/lib/python3.7/dist-packages/zmlpcd/process.py", "lineNumber": 263, "className": "process", "methodName": "retval = self.instance.process(frame)" }, { "file": "/zps/pylib/zmlp_core/core/processors.py", "lineNumber": 39, "className": "process", "methodName": "raise ZmlpFatalProcessorException('Failed to pre-cache source file', e)" } ] }], 'page': {'from': 0, 'size': 50, 'disabled': False, 'totalCount': 1}}  # noqa

        def mock_get_response(*args, **kwargs):
            return {"id": "ce4df7e7-5822-11ea-b3c8-0242ac120004", "projectId": "00000000-0000-0000-0000-000000000000", "dataSourceId": "ce46f306-5822-11ea-b3c8-0242ac120004", "name": "Applying modules:  to gs://zmlp-public-test-data", "type": "Import", "state": "Success", "assetCounts": {"assetCreatedCount": 1, "assetReplacedCount": 0, "assetWarningCount": 0, "assetErrorCount": 1}, "priority": 100, "paused": False, "timePauseExpired": -1, "maxRunningTasks": 1024, "jobId": "ce4df7e7-5822-11ea-b3c8-0242ac120004"}  # noqa

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('taskerror-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        _json = response.json()
        assert _json['count'] == 1

    def test_list_stacktrace_empty_methodname(self, monkeypatch, api_client,
                                              zmlp_project_user, project):
        def mock_post_response(*args, **kwargs):
            return {'list': [{"id": "d5ffb9ba-5822-11ea-b3c8-0242ac120004", "taskId": "d4fffcf9-5822-11ea-b3c8-0242ac120004", "jobId": "ce4df7e7-5822-11ea-b3c8-0242ac120004", "dataSourceId": "ce46f306-5822-11ea-b3c8-0242ac120004", "assetId": "oh4g6WGPFqlQOShzVdmpr2hugGu1WuEh", "path": "gs://zmlp-public-test-data/corrupt.jpg", "message": "ZmlpFatalProcessorException: ('Failed to pre-cache source file', ValueError('Anonymous credentials cannot be refreshed.'))", "processor": "zmlp_core.core.processors.PreCacheSourceFileProcessor", "fatal": True, "analyst": "not-implemented", "phase": "execute", "timeCreated": 1582671723066, "stackTrace": [{ "file": "/usr/local/lib/python3.7/dist-packages/zmlpcd/process.py", "lineNumber": 263, "className": "process", "methodName": "retval = self.instance.process(frame)" }, { "file": "/zps/pylib/zmlp_core/core/processors.py", "lineNumber": 39, "className": "process", "methodName": ""} ] }], 'page': {'from': 0, 'size': 50, 'disabled': False, 'totalCount': 1}}  # noqa

        def mock_get_response(*args, **kwargs):
            return {"id": "ce4df7e7-5822-11ea-b3c8-0242ac120004", "projectId": "00000000-0000-0000-0000-000000000000", "dataSourceId": "ce46f306-5822-11ea-b3c8-0242ac120004", "name": "Applying modules:  to gs://zmlp-public-test-data", "type": "Import", "state": "Success", "assetCounts": {"assetCreatedCount": 1, "assetReplacedCount": 0, "assetWarningCount": 0, "assetErrorCount": 1}, "priority": 100, "paused": False, "timePauseExpired": -1, "maxRunningTasks": 1024, "jobId": "ce4df7e7-5822-11ea-b3c8-0242ac120004"}  # noqa

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('taskerror-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        _json = response.json()
        assert _json['results'][0]['stackTrace'][1]['methodName'] == ''

    def test_list_blank_mehtodname(self, monkeypatch, api_client, zmlp_project_user, project):
        def mock_post_response(*args, **kwargs):
            return {'list': [{"id": "d5ffb9ba-5822-11ea-b3c8-0242ac120004", "taskId": "d4fffcf9-5822-11ea-b3c8-0242ac120004", "jobId": "ce4df7e7-5822-11ea-b3c8-0242ac120004", "dataSourceId": "ce46f306-5822-11ea-b3c8-0242ac120004", "assetId": "oh4g6WGPFqlQOShzVdmpr2hugGu1WuEh", "path": "gs://zmlp-public-test-data/corrupt.jpg", "message": "ZmlpFatalProcessorException: ('Failed to pre-cache source file', ValueError('Anonymous credentials cannot be refreshed.'))", "processor": "zmlp_core.core.processors.PreCacheSourceFileProcessor", "fatal": True, "analyst": "not-implemented", "phase": "execute", "timeCreated": 1582671723066}], 'page': {'from': 0, 'size': 50, 'disabled': False, 'totalCount': 1}}  # noqa

        def mock_get_response(*args, **kwargs):
            return {"id": "ce4df7e7-5822-11ea-b3c8-0242ac120004", "projectId": "00000000-0000-0000-0000-000000000000", "dataSourceId": "ce46f306-5822-11ea-b3c8-0242ac120004", "name": "Applying modules:  to gs://zmlp-public-test-data", "type": "Import", "state": "Success", "assetCounts": {"assetCreatedCount": 1, "assetReplacedCount": 0, "assetWarningCount": 0, "assetErrorCount": 1}, "priority": 100, "paused": False, "timePauseExpired": -1, "maxRunningTasks": 1024, "jobId": "ce4df7e7-5822-11ea-b3c8-0242ac120004"}  # noqa

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('taskerror-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        _json = response.json()
        assert _json['count'] == 1
        assert response.json()['results'][0]['stackTrace'] == []

    def test_retrieve(self, monkeypatch, api_client, zmlp_project_user, project):
        def mock_post_response(*args, **kwargs):
            return {"id": "d5ffb9ba-5822-11ea-b3c8-0242ac120004", "taskId": "d4fffcf9-5822-11ea-b3c8-0242ac120004", "jobId": "ce4df7e7-5822-11ea-b3c8-0242ac120004", "dataSourceId": "ce46f306-5822-11ea-b3c8-0242ac120004", "assetId": "oh4g6WGPFqlQOShzVdmpr2hugGu1WuEh", "path": "gs://zmlp-public-test-data/corrupt.jpg", "message": "ZmlpFatalProcessorException: ('Failed to pre-cache source file', ValueError('Anonymous credentials cannot be refreshed.'))", "processor": "zmlp_core.core.processors.PreCacheSourceFileProcessor", "fatal": True, "analyst": "not-implemented", "phase": "execute", "timeCreated": 1582671723066}  # noqa

        def mock_get_response(*args, **kwargs):
            return {"id": "ce4df7e7-5822-11ea-b3c8-0242ac120004", "projectId": "00000000-0000-0000-0000-000000000000", "dataSourceId": "ce46f306-5822-11ea-b3c8-0242ac120004", "name": "Applying modules:  to gs://zmlp-public-test-data", "type": "Import", "state": "Success", "assetCounts": {"assetCreatedCount": 1, "assetReplacedCount": 0, "assetWarningCount": 0, "assetErrorCount": 1}, "priority": 100, "paused": False, "timePauseExpired": -1, "maxRunningTasks": 1024, "jobId": "ce4df7e7-5822-11ea-b3c8-0242ac120004"}  # noqa

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('taskerror-detail', kwargs={'project_pk': project.id, 'pk': 'd5ffb9ba-5822-11ea-b3c8-0242ac120004'}))  # noqa
        assert response.status_code == 200
        assert response.json()['jobName'] == 'Applying modules:  to gs://zmlp-public-test-data'

    def test_retrieve_no_stack_trace(self, monkeypatch, api_client, zmlp_project_user, project):
        def mock_post_response(*args, **kwargs):
            return {"id": "d5ffb9ba-5822-11ea-b3c8-0242ac120004", "taskId": "d4fffcf9-5822-11ea-b3c8-0242ac120004", "jobId": "ce4df7e7-5822-11ea-b3c8-0242ac120004", "dataSourceId": "ce46f306-5822-11ea-b3c8-0242ac120004", "assetId": "oh4g6WGPFqlQOShzVdmpr2hugGu1WuEh", "path": "gs://zmlp-public-test-data/corrupt.jpg", "message": "ZmlpFatalProcessorException: ('Failed to pre-cache source file', ValueError('Anonymous credentials cannot be refreshed.'))", "processor": "zmlp_core.core.processors.PreCacheSourceFileProcessor", "fatal": True, "analyst": "not-implemented", "phase": "execute", "timeCreated": 1582671723066}  # noqa

        def mock_get_response(*args, **kwargs):
            return {"id": "ce4df7e7-5822-11ea-b3c8-0242ac120004", "projectId": "00000000-0000-0000-0000-000000000000", "dataSourceId": "ce46f306-5822-11ea-b3c8-0242ac120004", "name": "Applying modules:  to gs://zmlp-public-test-data", "type": "Import", "state": "Success", "assetCounts": {"assetCreatedCount": 1, "assetReplacedCount": 0, "assetWarningCount": 0, "assetErrorCount": 1}, "priority": 100, "paused": False, "timePauseExpired": -1, "maxRunningTasks": 1024, "jobId": "ce4df7e7-5822-11ea-b3c8-0242ac120004"}  # noqa

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('taskerror-detail', kwargs={'project_pk': project.id, 'pk': 'd5ffb9ba-5822-11ea-b3c8-0242ac120004'}))  # noqa
        assert response.status_code == 200
        assert response.json()['jobName'] == 'Applying modules:  to gs://zmlp-public-test-data'
        assert response.json()['stackTrace'] == []


class TestJobTaskViewset:

    def test_list(self, monkeypatch, api_client, zmlp_project_user, project):
        def mock_post_response(*args, **kwargs):
            return {'list': [{'id': '59527630-57f2-11ea-b3c8-0242ac120004', 'jobId': '5950534f-57f2-11ea-b3c8-0242ac120004', 'projectId': 'f7411da2-6573-4b1a-8e18-15af9bded45b', 'dataSourceId': '593689be-57f2-11ea-b3c8-0242ac120004', 'name': "Crawling files in 'gs://zorroa-dev-data'", 'state': 'Success', 'host': 'http://0945d0cfea37:5000', 'timeStarted': 1582652666857, 'timeStopped': 1582652672906, 'timeCreated': 1582650898050, 'timePing': 1582650898050, 'assetCounts': {'assetCreatedCount': 0, 'assetReplacedCount': 0, 'assetWarningCount': 0, 'assetErrorCount': 2}, 'taskId': '59527630-57f2-11ea-b3c8-0242ac120004'}, {'id': '63bf1241-57f2-11ea-b3c8-0242ac120004', 'jobId': '5950534f-57f2-11ea-b3c8-0242ac120004', 'projectId': 'f7411da2-6573-4b1a-8e18-15af9bded45b', 'dataSourceId': '593689be-57f2-11ea-b3c8-0242ac120004', 'name': 'Expand with 7 assets, 8 processors.', 'state': 'Success', 'host': 'http://0945d0cfea37:5000', 'timeStarted': 1582650916053, 'timeStopped': 1582650958777, 'timeCreated': 1582650915539, 'timePing': 1582650930794, 'assetCounts': {'assetCreatedCount': 0, 'assetReplacedCount': 7, 'assetWarningCount': 0, 'assetErrorCount': 0, 'assetTotalCount': 7}, 'taskId': '63bf1241-57f2-11ea-b3c8-0242ac120004'}], 'page': {'from': 0, 'size': 50, 'disabled': False, 'totalCount': 2}}  # noqa

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('job-detail-task-list', kwargs={'project_pk': project.id, 'job_pk': '950534f-57f2-11ea-b3c8-0242ac120004'}))  # noqa
        assert response.status_code == 200
        _json = response.json()
        assert _json['count'] == 2
        assert _json['results'][0]['assetCounts']['assetTotalCount'] == 2
