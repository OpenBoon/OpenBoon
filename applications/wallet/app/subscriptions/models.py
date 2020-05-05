import math
import uuid

from django.db import models
from django.conf import settings
from django.contrib.auth import get_user_model
from multiselectfield import MultiSelectField

from projects.models import Project
from wallet.utils import get_zmlp_superuser_client

User = get_user_model()

MODULES = (('zmlp-classification', 'Label Detection'),
           ('zmlp-objects', 'Object Detection'),
           ('zmlp-face-recognition', 'Facial Recognition'),
           ('zmlp-ocr', 'OCR (Optical Character Recognition)'),
           ('zmlp-deep-document', 'Page Analysis'),
           ('shot-detection', 'Shot Detection'),
           ('gcp-vision-crop-hints', 'Crop Hints (Vision)'),
           ('gcp-document-text-detection', 'OCR Documents (Vision)'),
           ('gcp-vision-text-detection', 'OCR Images (Vision)'),
           ('gcp-vision-label-detection', 'Label Detection (Vision)'),
           ('gcp-video-label-detection', 'Label Detection (Video)'),
           ('gcp-shot-detection', 'Shot Change (Video)'),
           ('gcp-explicit-content-detection', 'Explicit Content Detection (Video)'))


class Subscription(models.Model):
    """Represents the purchased plan for a given Project"""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    project = models.OneToOneField(Project, on_delete=models.CASCADE)
    video_hours_limit = models.IntegerField(default=0)
    image_count_limit = models.IntegerField(default=0)
    modules = MultiSelectField(choices=MODULES, blank=True)
    created_date = models.DateTimeField(auto_now_add=True)
    modified_date = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f'{self.project}'

    def limits(self):
        return {'video_hours': self.video_hours_limit,
                'image_count': self.image_count_limit}

    def usage_all_time(self):
        """Returns the all time usage information for the project."""
        client = self.project.get_zmlp_super_client()
        quotas = client.get(f'api/v1/project/_quotas')
        video_hours = self._get_usage_hours_from_seconds(quotas['videoSecondsCount'])
        image_count = quotas['pageCount']
        return {'video_hours': video_hours, 'image_count': image_count}

    def usage_last_hour(self):
        """Returns usage information from the last hour for the project."""
        client = self.project.get_zmlp_super_client()
        usage = client.get('/api/v1/project/_quotas_time_series')[-1]

        return {'end_time': usage['timestamp']/1000,
                'video_hours': self._get_usage_hours_from_seconds(usage['videoSecondsCount']),
                'image_count': usage['pageCount']}

    def _get_usage_hours_from_seconds(self, seconds):
        """Converts seconds to hours and always rounds up."""
        return math.ceil(seconds/60/60)
