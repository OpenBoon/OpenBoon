import os
import time
import requests

from boonflow import AssetProcessor, FileTypes
from boonflow.proxy import get_proxy_level, calculate_normalized_bbox
from boonflow.analysis import LabelDetectionAnalysis
from boonsdk import BoonSdkException

__all__ = [
    'GenomeVfxAnimatedFaces',
    'GenomeObjectDetector',
    'GenomeFullFrameClassifier'
]


class AbstractGenomeProcessor(AssetProcessor):
    """
    This base class is used for all Disney Genome Project processors. Subclasses only
    have to implement the "detect(asset, image) method.
    """

    file_types = FileTypes.images | FileTypes.videos
    analysis_name = None
    base_url = os.environ.get('GENOME_BASE_URL',
                              'https://automation-api-preprod.md.aws.seabc.go.com')
    namespace = None

    def __init__(self):
        super(AbstractGenomeProcessor, self).__init__()
        self.proxy_level = 1
        self._genome_username = None
        self._genome_password = None
        self._asset_proxy = None
        self.file_uri = None
        self.auth_token = None
        self.media = os.environ.get('GENOME_MEDIA', 'Marvel')
        self.sleep_seconds = int(os.environ.get('GENOME_SLEEP_SECONDS', 20))

    @property
    def genome_username(self):
        if self._genome_username is None:
            self._genome_username = os.environ.get('GENOME_USERNAME')
            if self._genome_username is None:
                raise BoonSdkException('GENOME_USERNAME unavailable. Please set in ENV variables.')
        return self._genome_username

    @property
    def genome_password(self):
        if self._genome_password is None:
            self._genome_password = os.environ.get('GENOME_PASSWORD')
            if self._genome_password is None:
                raise BoonSdkException('GENOME_PASSWORD unavailable. Please set in ENV variables.')

    @property
    def auth_headers(self):
        if self.auth_token is None:
            self.set_auth_token()
        return {'Authorization': f'Bearer {self.auth_token}'}

    def process(self, frame):
        asset = frame.asset
        proxy = get_proxy_level(asset, self.proxy_level)
        if not proxy:
            return
        task_data = self.submit_genome_task(proxy)
        result_data = self.get_results_when_complete(task_data)
        self.add_genome_analysis(asset, result_data)

    def set_auth_token(self):
        """Gets an authorization token from the Genome service.

        This token is valid for an hour and will be used for all requests to the service.
        """
        payload = {'user': self.genome_username,
                   'password': self.genome_password}
        response = requests.post(f'{self.base_url}/auth/token', json=payload)
        if not response.ok:
            raise BoonSdkException("Unable to get authorization token from Genome.")
        content = response.json()
        self.auth_token = content['token']

    def submit_genome_task(self, proxy):
        """Submits the task to Genome for the specified algorithm.

        Args:
            Object: Proxy file for the asset being processed.
        """
        file_uri = self.get_proxy_signed_url(proxy)
        payload = {
            'algorithm_name': self.namespace,
            'url': file_uri
        }
        if self.media:
            payload['media'] = self.media

        response = requests.post(f'{self.base_url}/process/url',
                                 headers=self.auth_headers, json=payload)
        if not response.ok:
            raise BoonSdkException("Unable to submit file for processing to Genome.")
        content = response.json()
        return content['task']

    def get_proxy_signed_url(self, proxy):
        """Retrieves the signed url for the proxy to analyze."""
        signed_url_path = f'api/v3/files/_sign/{proxy.id}'
        response = self.app.client.get(signed_url_path)
        return response['uri']

    def get_results_when_complete(self, task_data):
        """Polls and checks for when a Genome task has completed.

        Args:
            task_data (dict): Info describing task submitted to Genome.

        Returns:
            (dict): The results from the completed Genome task.
        """
        loopback_id = task_data.get('loopback_id')
        if not loopback_id:
            raise BoonSdkException('Unable to get loopback_id from Genome task data.')
        status_endpoint = f"{self.base_url}/process/check/{task_data['loopback_id']}"
        current_status = 'queue'
        total_sleep_time = 0
        while current_status not in ('done', 'error'):
            print(f'Waiting {self.sleep_seconds} seconds for task to complete. '
                  f'Current status: {current_status}, slept for {total_sleep_time} second so far.')
            total_sleep_time += self.sleep_seconds
            time.sleep(self.sleep_seconds)
            response = requests.get(status_endpoint, headers=self.auth_headers)
            if not response.ok:
                raise BoonSdkException(f'Error retrieving status of task '
                                       f'for loopback_id: {loopback_id}.')
            content = response.json()
            current_status = content['task']['status']
            if current_status == 'error':
                raise BoonSdkException(f'Genome task errored. Please report to Genome '
                                       f'team with loopback_id: {loopback_id}')

        return content['task']['result']

    def add_genome_analysis(self, asset, result_data):
        """Adds Genome task result to the asset's metadata."""
        raise NotImplementedError()


class GenomeVfxAnimatedFaces(AbstractGenomeProcessor):
    analysis_name = 'genome-vfx-animated-faces'
    namespace = 'VfxAnimatedFaces'

    def add_genome_analysis(self, asset, result_data):
        """Adds Genome task result to the asset's metadata."""
        analysis = LabelDetectionAnalysis()
        for detection in result_data['detections']:
            coords = detection['coord']
            proxy = get_proxy_level(asset, self.proxy_level)
            bbox = calculate_normalized_bbox(proxy.attrs['width'],
                                             proxy.attrs['height'],
                                             coords)
            for instance in detection['instances']:
                analysis.add_label_and_score(instance['label'],
                                             instance['prob'],
                                             bbox=bbox)
        asset.add_analysis(self.namespace, analysis)


class GenomeObjectDetector(AbstractGenomeProcessor):
    analysis_name = 'genome-object-detection'
    namespace = 'PyObjectDetector2'

    def add_genome_analysis(self, asset, result_data):
        """Adds Genome task result to the asset's metadata."""
        analysis = LabelDetectionAnalysis()
        for detection in result_data['detections']:
            for instance in detection['instances']:
                analysis.add_label_and_score(instance['label'],
                                             instance['prob'])
        asset.add_analysis(self.namespace, analysis)


class GenomeFullFrameClassifier(AbstractGenomeProcessor):
    analysis_name = 'genome-full-frame-classifier'
    namespace = 'FullFrameClassifier'

    def add_genome_analysis(self, asset, result_data):
        """Adds Genome task result to the asset's metadata."""
        analysis = LabelDetectionAnalysis()
        for detection in result_data['detections']:
            for instance in detection['instances']:
                analysis.add_label_and_score(instance['label'],
                                             instance['prob'])
        asset.add_analysis(self.namespace, analysis)
