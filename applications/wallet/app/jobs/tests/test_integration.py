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

    def test_list_stacktrace_no_methodname(self, monkeypatch, api_client, zmlp_project_user,
                                           project):
        def mock_post_response(*args, **kwargs):
            return {'list': [{"id": "d5ffb9ba-5822-11ea-b3c8-0242ac120004", "taskId": "d4fffcf9-5822-11ea-b3c8-0242ac120004", "jobId": "ce4df7e7-5822-11ea-b3c8-0242ac120004", "dataSourceId": "ce46f306-5822-11ea-b3c8-0242ac120004", "assetId": "oh4g6WGPFqlQOShzVdmpr2hugGu1WuEh", "path": "gs://zmlp-public-test-data/corrupt.jpg", "message": "ZmlpFatalProcessorException: ('Failed to pre-cache source file', ValueError('Anonymous credentials cannot be refreshed.'))", "processor": "zmlp_core.core.processors.PreCacheSourceFileProcessor", "fatal": True, "analyst": "not-implemented", "phase": "execute", "timeCreated": 1582671723066, "stackTrace": [{ "file": "/usr/local/lib/python3.7/dist-packages/zmlpcd/process.py", "lineNumber": 263, "className": "process", "methodName": "retval = self.instance.process(frame)" }, { "file": "/zps/pylib/zmlp_core/core/processors.py", "lineNumber": 39, "className": "process"} ] }], 'page': {'from': 0, 'size': 50, 'disabled': False, 'totalCount': 1}}  # noqa

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

    def test_list_no_stacktrace(self, monkeypatch, api_client, zmlp_project_user, project):
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
        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('job-detail-task-list', kwargs={'project_pk': project.id, 'job_pk': '950534f-57f2-11ea-b3c8-0242ac120004'}))  # noqa
        assert response.status_code == 200
        _json = response.json()
        assert _json['count'] == 2
        assert _json['results'][0]['assetCounts']['assetTotalCount'] == 2
