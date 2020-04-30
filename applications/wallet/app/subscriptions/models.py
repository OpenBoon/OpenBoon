import uuid

from django.db import models
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

    def usage(self):
        user = User.objects.get(email='software@zorroa.com')
        client = get_zmlp_superuser_client(user, project_id=str(self.project.id))
        quotas = client.get(f'api/v1/project/_quotas')
        video_hours = quotas['videoSecondsCount'] * 60 * 60
        image_count = quotas['pageCount']
        return {'video_hours': video_hours, 'image_count': image_count}
