import time
from google.cloud import monitoring_v3
from zmlp import ZmlpClient


class MetricValue(object):
    """Describes a metric to publish to GCP Monitoring.

    Args:
        metric_type(str): Metric type to pass to GCP. This is the name of your custom metric
         and must start with "custom.googleapis.com/" and has a path-style format. For more
         information see https://cloud.google.com/monitoring/custom-metrics/creating-metrics#custom_metric_names
        value(int): Integer value to publish for the metric.

    """
    def __init__(self, metric_type, value):
        if not metric_type.startswith('custom.googleapis.com/'):
            raise ValueError('Metric types must start with custom.googleapis.com/.')
        self.metric_type = metric_type
        self.value = value


class BaseMetric(object):
    """Base class all Metrics inherit from. Each concrete Metric must override the get_metric_values
    function and have it return a list of MetricValue objects to publish.

    NOTE: This implementation is currently limited to supporting int64 gauge metrics with a
    resource type of "global". As needed this should be expanded to support more varied metrics.
    More information can be found at
    https://cloud.google.com/monitoring/custom-metrics/creating-metrics.

    """
    def __init__(self, monitoring_client, zmlp_client, project_id):
        self.monitoring_client = monitoring_client
        self.zmlp_client = zmlp_client
        self.project_id = project_id

    def publish(self):
        for metric_value in list(self.get_metric_values()):
            series = monitoring_v3.types.TimeSeries()
            series.metric.type = metric_value.metric_type
            series.resource.type = 'global'
            point = series.points.add()
            now = time.time()
            point.interval.end_time.seconds = int(now)
            point.interval.end_time.nanos = int((now - point.interval.end_time.seconds) * 10 ** 9)
            point.value.int64_value = metric_value.value
            print(f'Submitting Time Series: {metric_value.metric_type} = {point.value.int64_value}')
            project_path = self.monitoring_client.project_path(self.project_id)
            self.monitoring_client.create_time_series(project_path, [series])

    def get_metric_values(self):
        """Returns a list of MetricValue objects to publish."""
        return NotImplemented


class JobQueueMetrics(BaseMetric):

    def get_metric_values(self):
        total_pending_jobs = 0
        total_pending_tasks = 0
        projects = self.zmlp_client.post('/api/v1/projects/_search', {})['list']
        for project in projects:
            client = ZmlpClient(apikey=self.zmlp_client.apikey, server=self.zmlp_client.server,
                                project_id=project['id'])
            jobs_response = client.post('/api/v1/jobs/_search',
                                       {'states': ['InProgress'], 'paused': False})
            pending_job_count = jobs_response['page']['totalCount']
            pending_task_count = 0
            for job in jobs_response['list']:
                task_counts = job['taskCounts']
                pending_task_count += task_counts['tasksWaiting']
                pending_task_count += task_counts['tasksRunning']
                pending_task_count += task_counts['tasksQueued']
            if pending_job_count:
                print(f'Project {project["name"]} has {pending_task_count} pending task(s) in '
                      f'{pending_job_count} pending job(s).')
            total_pending_jobs += pending_job_count
            total_pending_tasks += pending_task_count

        return [MetricValue('custom.googleapis.com/zmlp/total-pending-jobs', total_pending_jobs),
                MetricValue('custom.googleapis.com/zmlp/total-pending-tasks', total_pending_tasks)]

