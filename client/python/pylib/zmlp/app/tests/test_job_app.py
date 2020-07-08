# flake8: noqa
import os
import datetime
import logging
import unittest
from unittest.mock import patch

from zmlp import ZmlpClient, ZmlpApp
from zmlp.entity import Job, Task


logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)


class ZmlpJobAppTests(unittest.TestCase):

    def setUp(self):
        # This is not a valid key
        self.key_dict = {
            'projectId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'keyId': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'sharedKey': 'test123test135'
        }
        self.app = ZmlpApp(self.key_dict)

    @patch.object(ZmlpClient, 'get')
    def test_get_job(self, get_patch):
        get_patch.return_value = mock_job_data
        self.assert_job(self.app.jobs.get_job('12345'))

    @patch.object(ZmlpClient, 'get')
    def test_refresh_job(self, get_patch):
        get_patch.return_value = mock_job_data
        job = self.app.jobs.get_job('12345')
        self.app.jobs.refresh_job(job)
        self.assert_job(job)

    @patch.object(ZmlpClient, 'post')
    def test_find_jobs(self, post_patch):
        post_patch.return_value = {
            "list": [mock_job_data]
        }
        jobs = self.app.jobs.find_jobs(limit=1)
        for job in jobs:
            self.assert_job(job)

        jobs = self.app.jobs.find_jobs(id=['1234'],
                                       state=['Success'],
                                       name=['foo'],
                                       limit=1,
                                       sort={'name': 'desc'})
        for job in jobs:
            self.assert_job(job)

    @patch.object(ZmlpClient, 'post')
    def test_find_one_job(self, post_patch):
        post_patch.return_value = mock_job_data
        self.assert_job(self.app.jobs.find_one_job())

    @patch.object(ZmlpClient, 'post')
    def test_find_task_errors(self, post_patch):
        post_patch.return_value = {'list': [mock_task_error_data]}
        errors = list(self.app.jobs.find_task_errors(limit=1))
        assert 1 == len(errors)
        self.assert_task_error(errors[0])

    @patch.object(ZmlpClient, 'put')
    @patch.object(ZmlpClient, 'get')
    def test_pause_job(self, get_patch, put_patch):
        get_patch.return_value = mock_job_data
        put_patch.return_value = {
            'success': True
        }
        assert self.app.jobs.pause_job(Job(mock_job_data))
        assert self.app.jobs.pause_job("12345")

    @patch.object(ZmlpClient, 'put')
    @patch.object(ZmlpClient, 'get')
    def test_resume_job(self, get_patch, put_patch):
        get_patch.return_value = mock_job_data
        put_patch.return_value = {
            'success': True
        }
        assert self.app.jobs.resume_job(Job(mock_job_data))
        assert self.app.jobs.resume_job("12345")

    @patch.object(ZmlpClient, 'put')
    @patch.object(ZmlpClient, 'get')
    def test_cancel_job(self, get_patch, put_patch):
        get_patch.return_value = mock_job_data
        put_patch.return_value = {
            'success': True
        }
        assert self.app.jobs.cancel_job(Job(mock_job_data))
        assert self.app.jobs.cancel_job("12345")

    @patch.object(ZmlpClient, 'put')
    @patch.object(ZmlpClient, 'get')
    def test_restart_job(self, get_patch, put_patch):
        get_patch.return_value = mock_job_data
        put_patch.return_value = {
            'success': True
        }
        assert self.app.jobs.restart_job(Job(mock_job_data))
        assert self.app.jobs.restart_job("12345")

    @patch.object(ZmlpClient, 'put')
    @patch.object(ZmlpClient, 'get')
    def test_retry_all_failed_tasks(self, get_patch, put_patch):
        get_patch.return_value = mock_job_data
        put_patch.return_value = {
            'success': True
        }
        assert self.app.jobs.retry_all_failed_tasks(Job(mock_job_data))
        assert self.app.jobs.retry_all_failed_tasks("12345")

    @patch.object(ZmlpClient, 'post')
    def test_find_tasks(self, post_patch):
        post_patch.return_value = {'list': [mock_task_data]}
        tasks = list(self.app.jobs.find_tasks(limit=1))
        assert 1 == len(tasks)
        self.assert_task(tasks[0])

        tasks = list(self.app.jobs.find_tasks(job=["12345"],
                                              id=["i2345"],
                                              name=["12345"],
                                              state=['12345'],
                                              limit=1,
                                              sort=["name:d"]))
        assert 1 == len(tasks)
        self.assert_task(tasks[0])

    @patch.object(ZmlpClient, 'post')
    def test_find_one_task(self, post_patch):
        post_patch.return_value = mock_task_data
        task = self.app.jobs.find_one_task()
        self.assert_task(task)
        task = self.app.jobs.find_one_task(id=["i2345"],
                                           job=["12345"],
                                           name=["12345"],
                                           state=['12345'])
        self.assert_task(task)

    @patch.object(ZmlpClient, 'get')
    def test_get_task(self, get_patch):
        get_patch.return_value = mock_task_data
        task = self.app.jobs.get_task("12345")
        self.assert_task(task)

    @patch.object(ZmlpClient, 'get')
    def test_refresh_task(self, get_patch):
        get_patch.return_value = mock_task_data
        task = self.app.jobs.get_task("12345")
        self.assert_task(task)

    @patch.object(ZmlpClient, 'get')
    @patch.object(ZmlpClient, 'put')
    def test_skip_task(self, put_patch, get_patch):
        get_patch.return_value = mock_task_data
        put_patch.return_value = {
            'success': True
        }
        assert self.app.jobs.skip_task("12345")
        assert self.app.jobs.skip_task(Task(mock_task_data))

    @patch.object(ZmlpClient, 'get')
    @patch.object(ZmlpClient, 'put')
    def test_retry_task(self, put_patch, get_patch):
        get_patch.return_value = mock_task_data
        put_patch.return_value = {
            'success': True
        }
        assert self.app.jobs.retry_task("12345")
        assert self.app.jobs.retry_task(Task(mock_task_data))

    @patch.object(ZmlpClient, 'get')
    def test_get_task_script(self, get_patch):
        get_patch.return_value = {"script": "foo"}
        script = self.app.jobs.get_task_script("12345")
        assert script['script'] == 'foo'
        script = self.app.jobs.get_task_script(Task(mock_task_data))
        assert script['script'] == 'foo'

    @patch.object(ZmlpClient, 'stream')
    def test_download_task_log(self, stream_patch):
        log_file = "/tmp/task.log"
        try:
            os.unlink(log_file)
        except FileNotFoundError:
            pass

        stream_patch.return_value = log_file
        self.assertEquals(log_file, self.app.jobs.download_task_log("12345", log_file))

    @patch.object(ZmlpClient, 'stream_text')
    def test_iterate_task_log(self, stream_patch):
        stream_patch.return_value = (a for a in [b"test", b"test",  b"test"])
        count = 0
        for line in self.app.jobs.iterate_task_log("12345"):
            self.assertEqual(b"test", line)
            count += 1
        self.assertEqual(3, count)

    def assert_task(self, task):
        assert mock_task_data['id'] == task.id
        assert mock_task_data['name'] == task.name
        assert mock_task_data['jobId'] == task.job_id
        assert mock_task_data['state'] == task.state
        assert isinstance(task.time_created, datetime.datetime)
        assert isinstance(task.time_modified, datetime.datetime)
        assert isinstance(task.time_started, datetime.datetime)
        assert isinstance(task.time_stopped, datetime.datetime)
        assert isinstance(task.time_pinged, datetime.datetime)

    def assert_task_error(self, error):
        assert mock_task_error_data['id'] == error.id
        assert mock_task_error_data['assetId'] == error.asset_id
        assert mock_task_error_data['fatal'] is True
        assert mock_task_error_data['jobId'] is error.job_id
        assert mock_task_error_data['message'] is error.message
        assert mock_task_error_data['path'] is error.path
        assert mock_task_error_data['phase'] is error.phase
        assert mock_task_error_data['processor'] is error.processor
        assert mock_task_error_data['stackTrace'] is error.stack_trace

    def assert_job(self, job):
        assert mock_job_data['id'] == job.id
        assert mock_job_data['state'] == job.state
        assert mock_job_data['priority'] == job.priority
        assert isinstance(job.time_created, datetime.datetime)
        assert isinstance(job.time_modified, datetime.datetime)
        assert isinstance(job.time_started, datetime.datetime)
        assert isinstance(job.time_stopped, datetime.datetime)

        assert 0 == job.asset_counts.asset_created_count
        assert 0 == job.asset_counts.asset_error_count
        assert 0 == job.asset_counts.asset_replaced_count
        assert 0 == job.asset_counts.asset_warning_count
        assert 0 == job.task_counts.tasks_failure
        assert 0 == job.task_counts.tasks_queued
        assert 0 == job.task_counts.tasks_running
        assert 0 == job.task_counts.tasks_skipped
        assert 1 == job.task_counts.tasks_success
        assert 1 == job.task_counts.tasks_total
        assert 0 == job.task_counts.tasks_waiting


mock_job_data = {
    'assetCounts': {'assetCreatedCount': 0,
                    'assetErrorCount': 0,
                    'assetReplacedCount': 0,
                    'assetWarningCount': 0},
    'id': '85f90f4e-fb60-103a-98bb-0242c0a8100a',
    'jobId': '85f90f4e-fb60-103a-98bb-0242c0a8100a',
    'maxRunningTasks': 1024,
    'name': 'Analyze 10 created assets, 0 existing files.',
    'paused': False,
    'priority': 100,
    'projectId': '00000000-0000-0000-0000-000000000000',
    'state': 'Success',
    'taskCounts': {'tasksFailure': 0,
                   'tasksQueued': 0,
                   'tasksRunning': 0,
                   'tasksSkipped': 0,
                   'tasksSuccess': 1,
                   'tasksTotal': 1,
                   'tasksWaiting': 0},
    'timeCreated': 1587065716234,
    'timePauseExpired': -1,
    'timeStarted': 1587065721450,
    'timeStopped': 1587065758872,
    'timeUpdated': 1587065758872,
    'type': 'Import'}

mock_task_error_data = {
    'analyst': 'not-implemented',
    'assetId': 'YF6q_R-4TWVnWwOWKnKBeZtlBnRJo8nF',
    'fatal': True,
    'id': '85f90f5a-fb60-103a-98bb-0242c0a8100a',
    'jobId': '85f90f58-fb60-103a-98bb-0242c0a8100a',
    'message': "CalledProcessError: Command '['oiiotool', '-q', '--wildcardoff', "
               "'--info:format=xml:verbose=1', "
               "'/tmp/85f90f59-fb60-103a-98bb-0242c0a8100a/0fab77f78893dbdf2c42b600ecdd2d55e9d680bf.jpg']' "
               'returned non-zero exit status 255.',
    'path': '/Users/chambers/src/zmlp/test-data/images/cow.jpg',
    'phase': 'execute',
    'processor': 'zmlp_core.core.FileImportProcessor',
    'stackTrace': [{'className': 'process',
                    'file': '/usr/local/lib/python3.7/dist-packages/zmlpcd/process.py',
                    'lineNumber': 293,
                    'methodName': 'retval = self.instance.process(frame)'},
                   {'className': 'process',
                    'file': '/zvi/pylib/zmlp_core/core/importers.py',
                    'lineNumber': 49,
                    'methodName': 'proc.process(frame)'},
                   {'className': 'process',
                    'file': '/zvi/pylib/zmlp_core/image/importers.py',
                    'lineNumber': 42,
                    'methodName': 'metadata = get_image_metadata(path)'},
                   {'className': 'get_image_metadata',
                    'file': '/zvi/pylib/zmlp_core/util/media.py',
                    'lineNumber': 80,
                    'methodName': 'output = check_output(cmd, shell=False, '
                                  'stderr=DEVNULL)'},
                   {'className': 'check_output',
                    'file': '/usr/lib/python3.7/subprocess.py',
                    'lineNumber': 411,
                    'methodName': '**kwargs).stdout'},
                   {'className': 'run',
                    'file': '/usr/lib/python3.7/subprocess.py',
                    'lineNumber': 512,
                    'methodName': 'output=stdout, stderr=stderr)'}],
    'taskId': '85f90f59-fb60-103a-98bb-0242c0a8100a',
    'timeCreated': 1587134193431}

mock_task_data = {
    'assetCounts': {'assetCreatedCount': 0,
                    'assetErrorCount': 1,
                    'assetReplacedCount': 0,
                    'assetTotalCount': 1,
                    'assetWarningCount': 0},
    'host': 'http://11ac7b4d5b32:5000',
    'id': '85f90f59-fb60-103a-98bb-0242c0a8100a',
    'jobId': '85f90f58-fb60-103a-98bb-0242c0a8100a',
    'name': 'Analyze 1 created assets, 0 existing files.',
    'projectId': '00000000-0000-0000-0000-000000000000',
    'state': 'Failure',
    'taskId': '85f90f59-fb60-103a-98bb-0242c0a8100a',
    'timeCreated': 1587134186053,
    'timePing': 1587134194866,
    'timeStarted': 1587134190370,
    'timeStopped': 1587134198415}
