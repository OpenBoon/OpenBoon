from datetime import datetime
from.util import ObjectView
from .entity import BaseEntity


class Job(BaseEntity):
    """
    A Job represents a background file import process.
    """

    def __init__(self, data):
        super(Job, self).__init__(data)

    @property
    def name(self):
        """The name of the Job"""
        return self._data['name']

    @property
    def state(self):
        """The state of the Job"""
        return self._data['state']

    @property
    def paused(self):
        """True if the job is paused."""
        return self._data['paused']

    @property
    def priority(self):
        """The priority of the Job"""
        return self._data['priority']

    @property
    def time_started(self):
        """The datetime the job got the first analyst."""
        if self._data['timeStarted'] == -1:
            return None
        else:
            return datetime.fromtimestamp(self._data['timeStarted'] / 1000.0)

    @property
    def time_stopped(self):
        """The datetime the job finished."""
        if self._data['timeStopped'] == -1:
            return None
        else:
            return datetime.fromtimestamp(self._data['timeStopped'] / 1000.0)

    @property
    def asset_counts(self):
        return ObjectView(self._data['assetCounts'])

    @property
    def task_counts(self):
        return ObjectView(self._data['taskCounts'])

    @property
    def time_modified(self):
        """The date/time the entity was modified."""
        return datetime.fromtimestamp(self._data['timeUpdated'] / 1000.0)


class Task(BaseEntity):
    """
    Jobs contain Tasks and each Task handles the processing for 1 or more files/assets.
    """

    def __init__(self, data):
        super(Task, self).__init__(data)

    @property
    def job_id(self):
        """The DataSource Id if any """
        return self._data.get('datasourceId')

    @property
    def name(self):
        """The name of the Task"""
        return self._data['name']

    @property
    def time_started(self):
        """The datetime the job got the first analyst."""
        if self._data['timeStarted'] == -1:
            return None
        else:
            return datetime.fromtimestamp(self._data['timeStarted'] / 1000.0)

    @property
    def time_stopped(self):
        """The datetime the job finished."""
        if self._data['timeStopped'] == -1:
            return None
        else:
            return datetime.fromtimestamp(self._data['timeStopped'] / 1000.0)

    @property
    def time_pinged(self):
        """The datetime the running task sent a watch dog ping."""
        if self._data['timePing'] == -1:
            return None
        else:
            return datetime.fromtimestamp(self._data['timePing'] / 1000.0)

    @property
    def asset_counts(self):
        return ObjectView(self._data['assetCounts'])


class TaskError:
    """
    A TaskError contains information regarding a failed Task or Asset.
    """

    def __init__(self, data):
        self._data = data

    @property
    def id(self):
        """ID of the TaskError"""
        return self._data['id']

    @property
    def task_id(self):
        """UUID of the Task that encountered an error."""
        return self._data['taskId']

    @property
    def job_id(self):
        """UUID of the Job that encountered an error."""
        return self._data['jobId']

    @property
    def datasource_id(self):
        """UUID of the DataSource that encountered an error."""
        return self._data['dataSourceId']

    @property
    def asset_id(self):
        """ID of the Asset that encountered an error."""
        return self._data['assetId']

    @property
    def path(self):
        """File path or URI that was being processed."""
        return self._data['path']

    @property
    def message(self):
        """Error message from the exception that generated the error."""
        return self._data['message']

    @property
    def processor(self):
        """Processor in which the error occurred."""
        return self._data['processor']

    @property
    def fatal(self):
        """True if the error was fatal and the Asset was not processed."""
        return self._data['fatal']

    @property
    def phase(self):
        """Phase at which the error occurred: generate, execute, teardown."""
        return self._data['phase']

    @property
    def time_created(self):
        """The date/time the entity was created."""
        return datetime.fromtimestamp(self._data['timeCreated'] / 1000.0)

    @property
    def stack_trace(self):
        """Full stack trace from the error, if any."""
        return self._data['stackTrace']

