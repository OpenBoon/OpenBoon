import math
import uuid

from django.contrib.auth import get_user_model
from django.db import models

from projects.models import Project
from wallet.mixins import UUIDMixin, TimeStampMixin

User = get_user_model()


class Tier(models.TextChoices):
    """Choices for the tier field in the Subscription model."""
    ESSENTIALS = 'essentials'
    PREMIER = 'premier'


class Subscription(UUIDMixin, TimeStampMixin):
    """Represents the purchased plan for a given Project"""
    project = models.OneToOneField(Project, on_delete=models.CASCADE)
    tier = models.CharField(max_length=20, choices=Tier.choices, default=Tier.ESSENTIALS)

    def __str__(self):
        return f'{self.project}'

    def usage(self):
        """Returns the all time usage information for the project."""
        client = self.project.get_zmlp_super_client()
        quotas = client.get('api/v1/project/_quotas')
        video_hours = self._get_usage_hours_from_seconds(quotas['videoSecondsCount'])
        image_count = quotas['pageCount']
        return {'video_hours': video_hours, 'image_count': image_count}

    def usage_last_hour(self):
        """Returns usage information from the last hour for the project."""
        client = self.project.get_zmlp_super_client()
        all_usage = client.get('/api/v1/project/_quotas_time_series')
        if not all_usage:
            return None
        usage = all_usage[-1]
        return {'end_time': usage['timestamp']/1000,
                'video_hours': self._get_usage_hours_from_seconds(usage['videoSecondsCount']),
                'image_count': usage['pageCount']}

    def _get_usage_hours_from_seconds(self, seconds):
        """Converts seconds to hours and always rounds up."""
        return math.ceil(seconds/60/60)
