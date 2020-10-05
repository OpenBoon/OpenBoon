import os
import time

import requests
from google.cloud import monitoring_v3
from requests.auth import HTTPBasicAuth


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

    NOTE: This implementation is currently limited to supporting int64 and double gauge metrics with
    a resource type of "global". As needed this should be expanded to support more varied metrics.
    More information can be found at
    https://cloud.google.com/monitoring/custom-metrics/creating-metrics.

    """
    def __init__(self, monitoring_client, zmlp_client, k8s_client, project_id):
        self.monitoring_client = monitoring_client
        self.zmlp_client = zmlp_client
        self.project_id = project_id
        self.k8s_client = k8s_client

    def publish(self):
        for metric_value in list(self.get_metric_values()):
            series = monitoring_v3.types.TimeSeries()
            series.metric.type = metric_value.metric_type
            series.resource.type = 'global'
            point = series.points.add()
            now = time.time()
            point.interval.end_time.seconds = int(now)
            point.interval.end_time.nanos = int((now - point.interval.end_time.seconds) * 10 ** 9)
            if isinstance(metric_value.value, int):
                point.value.int64_value = metric_value.value
            elif isinstance(metric_value.value, float):
                point.value.double_value = metric_value.value
            else:
                raise TypeError('Metric values must be ints or floats.')
            print(f'Submitting Time Series: {metric_value.metric_type} = {point.value}')
            project_path = self.monitoring_client.project_path(self.project_id)
            self.monitoring_client.create_time_series(project_path, [series])

    def get_metric_values(self):
        """Returns a list of MetricValue objects to publish."""
        return NotImplemented


class JobQueueMetrics(BaseMetric):

    def get_metric_values(self):
        api_gateway_url = os.environ['ZMLP_API_URL']
        basic_auth = HTTPBasicAuth('monitor', os.environ['MONITOR_PASSWORD'])

        # Get max running tasks.
        response = requests.get(os.path.join(api_gateway_url, 'monitor/metrics/tasks.max_running'),
                                auth=basic_auth)
        max_running_tasks = int(response.json()['measurements'][0]['value'])

        # Get total pending tasks.
        response = requests.get(os.path.join(api_gateway_url, 'monitor/metrics/tasks.active'),
                                auth=basic_auth)
        total_pending_tasks = int(response.json()['measurements'][0]['value'])

        desired_analyst_count = int(min(total_pending_tasks, max_running_tasks))
        current_analyst_count = self.get_analyst_count()
        analyst_scale_ratio = desired_analyst_count / current_analyst_count

        return [MetricValue('custom.googleapis.com/zmlp/total-pending-tasks', total_pending_tasks),
                MetricValue('custom.googleapis.com/zmlp/max-running-tasks', max_running_tasks),
                MetricValue('custom.googleapis.com/zmlp/current-analyst-count', current_analyst_count),
                MetricValue('custom.googleapis.com/zmlp/desired-analyst-count', desired_analyst_count),
                MetricValue('custom.googleapis.com/zmlp/analyst-scale-ratio', analyst_scale_ratio)]

    def get_analyst_count(self):
        scale = self.k8s_client.AppsV1Api().read_namespaced_deployment_scale('analyst', 'default')
        return scale.status.replicas
