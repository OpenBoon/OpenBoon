from ..entity import Job, Task, TaskError
from ..util import as_collection, as_id_collection, as_id


class JobApp:
    """
    An App instance for managing Jobs. Jobs are containers for async processes
    such as data import or training.
    """
    def __init__(self, app):
        self.app = app

    def get_job(self, id):
        """
        Get a Job by its unique Id.
        Args:
            id (str): The Job id or Job object.

        Returns:
            Job: The Job
        """
        return Job(self.app.client.get('/api/v1/jobs/{}'.format(as_id(id))))

    def refresh_job(self, job):
        """
        Refreshes the internals of the given job.

        Args:
            job (Job): The job to refresh.

        """
        job._data = self.app.client.get('/api/v1/jobs/{}'.format(job.id))

    def find_jobs(self, id=None, state=None, name=None, limit=None, sort=None):
        """
        Find jobs matching the given criteria.

        Args:
            id (mixed): A job ID or IDs to filter on.
            state (mixed): A Job state or list of states to filter on.
            name (mixed): A Job name or list of names to filter on.
            limit (int): The maximum number of jobs to return, None  is no limit.
            sort (list): A list of sort ordering phrases, like ["name:d", "time_created:a"]

        Returns:
            generator: A generator which will return matching jobs when iterated.

        """
        body = {
            'ids': as_collection(id),
            'states': as_collection(state),
            'names': as_collection(name),
            'sort': sort
        }
        return self.app.client.iter_paged_results('/api/v1/jobs/_search', body, limit, Job)

    def find_one_job(self, id=None, state=None, name=None):
        """
        Find single Job matching the given criteria.  Raises exception if more
        than one result is found.

        Args:
            id (mixed): A job ID or IDs to filter on.
            state (mixed): A Job state or list of states to filter on.
            name (mixed): A Job name or list of names to filter on.
            sort (list): A list of sort ordering phrases, like ["name:d", "time_created:a"]

        Returns:
            Job: The job.
        """
        body = {
            'ids': as_collection(id),
            'states': as_collection(state),
            'names': as_collection(name)
        }
        return Job(self.app.client.post('/api/v1/jobs/_findOne', body))

    def find_task_errors(self, query=None, job=None, task=None,
                         asset=None, path=None, processor=None, limit=None, sort=None):
        """
        Find TaskErrors based on the supplied criterion.

        Args:
            query (str): keyword query to match various error properties.
            job (mixed): A single Job, job id or list of either type.
            task (mixed): A single Task, task id or list of either type.
            asset (mixed): A single Asset, asset id or list of either type.
            path (mixed): A file path or list of file path.
            processor (mixed): A processor name or list of processors.
            limit (int): Limit the number of results or None for all results.
            sort (list): A list of sort ordering phrases, like ["name:d", "time_created:a"]

        Returns:
            generator: A generator which returns results when iterated.

        """
        body = {
            'keywords': query,
            'jobIds': as_id_collection(job),
            'taskIds': as_id_collection(task),
            'assetIds': as_id_collection(asset),
            'paths': as_collection(path),
            'processor': as_collection(processor),
            'sort': sort
        }
        return self.app.client.iter_paged_results(
            '/api/v1/taskerrors/_search', body, limit, TaskError)

    def pause_job(self, job):
        """
        Pause scheduling for the given Job.  Pausing a job simply removes the
        job from scheduler consideration.  All existing tasks will continue to run
        and Analysts will move to new jobs as tasks complete.

        Args:
            job (Job): The Job to pause

        Returns:
            bool: True if the job was actually paused.

        """
        # Resolve the job if we need to.
        if isinstance(job, str):
            job = self.get_job(job)
        if self.app.client.put('/api/v1/jobs/{}'.format(job.id), job._data)['success']:
            job._data['paused'] = True
            return True
        return False

    def resume_job(self, job):
        """
        Resume scheduling for the given Job.

        Args:
            job (Job): The Job to resume

        Returns:
            bool: True of the job was actually resumed.
        """
        if isinstance(job, str):
            job = self.get_job(job)
        if self.app.client.put('/api/v1/jobs/{}'.format(job.id), job._data)['success']:
            job._data['paused'] = False
            return True
        return False

    def cancel_job(self, job):
        """
        Cancel the given Job.  Canceling a job immediately kills all running Tasks
        and removes the job from scheduler consideration.

        Args:
            job (Job): The Job to cancel, or the job's unique Id.

        Returns:
            bool: True if the job was actually canceled, False if the job was already cancelled.

        """
        if isinstance(job, str):
            job = self.get_job(job)
        if self.app.client.put('/api/v1/jobs/{}/_cancel'.format(job.id)).get('success'):
            self.refresh_job(job)
            return True
        return False

    def restart_job(self, job):
        """
        Restart a canceled job.

        Args:
            job (Job): The Job to restart

        Returns:
            bool: True if the job was actually restarted, false if the job was not cancelled.

        """
        if isinstance(job, str):
            job = self.get_job(job)
        if self.app.client.put('/api/v1/jobs/{}/_restart'.format(job.id)).get('success'):
            self.refresh_job(job)
            return True
        return False

    def retry_all_failed_tasks(self, job):
        """
        Retry all failed Tasks in the Job.

        Args:
            job (Job): The Job with failed tasks.

        Returns:
            bool: True if the some failed tasks were restarted.
        """
        if isinstance(job, str):
            job = self.get_job(job)
        if self.app.client.put(
                '/api/v1/jobs/{}/_retryAllFailures'.format(job.id)).get('success'):
            self.refresh_job(job)
            return True
        return False

    def find_tasks(self, job=None, id=None, name=None, state=None, limit=None, sort=None):
        """
        Find Tasks matching the given criteria.

        Args:
            job: (mixed): A single Job, job id or list of either type.
            id (mixed): A single Task, task id or list of either type.
            name (mixed): A task name or list of tasks names.
            state (mixed): A take state or list of task states.
            limit (int): Limit the number of results, None for no limit.
            sort (list): A list of sort ordering phrases, like ["name:d", "time_created:a"]

        Returns:
            generator: A Generator that returns matching Tasks when iterated.
        """
        body = {
            'ids': as_collection(id),
            'states': as_collection(state),
            'names': as_collection(name),
            'jobIds': as_id_collection(job),
            'sort': sort
        }
        return self.app.client.iter_paged_results('/api/v1/tasks/_search', body, limit, Task)

    def find_one_task(self, job=None, id=None, name=None, state=None):
        """
        Find a single task matching the criterion.

        Args:
            job: (mixed): A single Job, job id or list of either type.
            id (mixed): A single Task, task id or list of either type.
            name (mixed): A task name or list of tasks names.
            state (mixed): A take state or list of task states.

        Returns:
            Task A single matching task.

        """
        body = {
            'ids': as_collection(id),
            'states': as_collection(state),
            'names': as_collection(name),
            'jobIds': as_id_collection(job)
        }
        res = Task(self.app.client.post('/api/v1/tasks/_findOne', body))
        return res

    def get_task(self, task):
        """
        Get a Task by its unique id.

        Args:
            task (str): The Task or task id.

        Returns:
            Task: The Task

        """
        return Task(self.app.client.get('/api/v1/tasks/{}'.format(as_id(task))))

    def refresh_task(self, task):
        """
        Refreshes the internals of the given job.

        Args:
            task (Task): The Task

        """
        task._data = self.app.client.get('/api/v1/tasks/{}'.format(task.id))

    def skip_task(self, task):
        """
        Skip the given task.  A skipped task wilk not run.

        Args:
            task (str): The Task or task id.

        Returns:
            bool: True if the Task changed to the Skipped state.
        """
        if isinstance(task, str):
            task = self.get_task(task)
        if self.app.client.put('/api/v1/tasks/{}/_skip'.format(task.id))['success']:
            self.refresh_task(task)
            return True
        return False

    def retry_task(self, task):
        """
        Retry the given task.  Retried tasks are set back to the waiting state.

        Args:
            task (str): The Task or task id.

        Returns:
           bool: True if the Task changed to the Waiting state.
        """
        if isinstance(task, str):
            task = self.get_task(task)
        if self.app.client.put('/api/v1/tasks/{}/_retry'.format(task.id))['success']:
            self.refresh_task(task)
            return True
        return False

    def get_task_script(self, task):
        """
        Return the given task's ZPS script.

        Args:
            task: (str): The Task or task id.

        Returns:
            dict: The script in dictionary form.
        """
        return self.app.client.get('/api/v1/tasks/{}/_script'.format(as_id(task)))

    def download_task_log(self, task, dst_path):
        """
        Download the task log file to the given file path.

        Args:
            task: (str): The Task or task id.
            dst_path (str): The path to the destination file.
        Returns:
            dict: The script in dictionary form.
        """
        return self.app.client.stream('/api/v1/tasks/{}/_log'.format(as_id(task)), dst_path)

    def iterate_task_log(self, task):
        """
        Return a generator that can be used to iterate a task log file.

        Args:
            task: (str): The Task or task id.

        Returns:
            generator: A generator which yields each line of a log file.

        """
        return self.app.client.stream_text('/api/v1/tasks/{}/_log'.format(as_id(task)))
