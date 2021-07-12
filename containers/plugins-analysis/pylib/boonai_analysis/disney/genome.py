import os
import requests

from boonflow import AssetProcessor, FileTypes
from boonflow.proxy import get_proxy_level
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

    file_types = FileTypes.images | FileTypes.video
    analysis_name = None
    base_url = 'https://automation-api-preprod.md.aws.seabc.go.com'
    algorithm = None

    def __init__(self):
        super(AbstractGenomeProcessor, self).__init__()
        self.proxy_level = 1
        self.username = os.environ.get("GENOME_USERNAME")
        self.password = os.environ.get("GENOME_PASSWORD")
        self.file_uri = None
        self.auth_token = None
        self.media = os.environ.get('GENOME_MEDIA', 'Marvel')

    def process(self, frame):
        asset = frame.asset
        proxy = get_proxy_level(asset, self.proxy_level)
        if not proxy:
            return
        self.set_auth_token()
        task_data = self.submit_genome_task(proxy)
        results = self.get_results_when_complete(task_data)
        self.add_genome_analysis(results)
        # Do some more stuff
        # get the token
        # submit the request
        #

    def set_auth_token(self):
        """Gets an authorization token from the Genome service.

        This token is valid for an hour and will be used for all requests to the service.
        """
        payload = {'user': self.username,
                   'password': self.password}
        response = requests.post(f'{self.base_url}/auth/token')
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
            'algorithm_name': self.algorithm,
            'url': file_uri
        }
        if self.media:
            payload['media'] = self.media

        headers = {'Authorization': f'Bearer {self.auth_token}'}
        response = requests.post(f'{self.base_url}/process/url',
                                 headers=headers, json=payload)
        if not response.ok:
            raise BoonSdkException("Unable to submit file for processing to Genome.")
        content = response.json()
        return content['task']
