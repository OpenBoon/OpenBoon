import pytest
from django.urls import reverse
from django.test import override_settings
from requests import Response
from zmlp import ZmlpClient
from jobs.views import JobsViewSet
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

    @override_settings(PLATFORM='zmlp')
    def test_get_list_zmlp(self, zmlp_project_user, project, api_client, monkeypatch):

        def mock_api_response(*args, **kwargs):
            response = {"list": [{"id": "82d53089-67c2-1433-8fef-0a580a000955", "organizationId": "00000000-9998-8888-7777-666666666666", "name": "test-whitespace.json", "type": "Import", "state": "Active", "assetCounts": {"assetCreatedCount": 0, "assetReplacedCount": 0, "assetWarningCount": 0, "assetErrorCount": 4}, "taskCounts": {"tasksTotal": 1, "tasksWaiting": 0, "tasksRunning": 0, "tasksSuccess": 0, "tasksFailure": 1, "tasksSkipped": 0, "tasksQueued": 0}, "createdUser": {"id": "00000000-7b0b-480e-8c36-f06f04aed2f1", "username": "admin", "email": "admin@zorroa.com", "permissionId": "00000000-fc08-4e4a-aa7a-a183f42c9fa0", "homeFolderId": "00000000-2395-4e71-9e4c-dacceef6ad53", "organizationId": "00000000-9998-8888-7777-666666666666"}, "timeStarted": 1573090540886, "timeUpdated": 1573090536003, "timeCreated": 1573090536003, "priority": 100, "paused": False, "timePauseExpired": -1, "maxRunningTasks": 1024, "jobId": "82d53089-67c2-1433-8fef-0a580a000955"}], "page": {"from": 0, "size": 50, "totalCount": 0}}  # noqa
            return response

        monkeypatch.setattr(ZmlpClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('job-list', kwargs={'project_pk': project.id}))
        assert response.status_code == 200
        content = response.json()
        assert len(content['results']) == 1
        assert len(content['results'][0]) > 0

    @override_settings(PLATFORM='zmlp')
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

        monkeypatch.setattr(JobsViewSet, '_get_updated_info', get_updated_info_mock)
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

        monkeypatch.setattr(JobsViewSet, '_get_updated_info', get_updated_info_mock)
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

        monkeypatch.setattr(JobsViewSet, '_get_updated_info', get_updated_info_mock)
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

        monkeypatch.setattr(JobsViewSet, '_get_updated_info', get_updated_info_mock)
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
            'arbitraryKey': 'NewValue'
        }
        viewset = JobsViewSet()
        new_job_spec = viewset._get_updated_info(api_client, job_pk, new_values)
        assert new_job_spec['name'] == new_values['name']
        assert new_job_spec['priority'] == new_values['priority']
        assert new_job_spec['paused'] == new_values['paused']
        assert new_job_spec['arbitraryKey'] == new_values['arbitraryKey']
