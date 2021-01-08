from django.db import models


class ApiCall(models.Model):
    """Represents a registered API Call for billing purposes"""

    project = models.UUIDField()
    service = models.CharField(max_length=64)
    asset_id = models.CharField(max_length=32)
    asset_path = models.CharField(max_length=255, blank=True, default='')
    image_count = models.IntegerField(blank=True, default=0)
    video_minutes = models.FloatField(blank=True, default=0.0)
    created_date = models.DateTimeField(auto_now_add=True)
    modified_date = models.DateTimeField(auto_now=True)

    class Meta:
        unique_together = (
            ('service', 'asset_id')
        )
