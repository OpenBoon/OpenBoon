from django.db import models
from psqlextra.manager import PostgresManager


class ApiCall(models.Model):
    """Represents a registered API Call for billing purposes"""
    objects = PostgresManager()

    project = models.UUIDField()
    service = models.TextField()
    asset_id = models.CharField(max_length=32)
    asset_path = models.TextField(blank=True, default='')
    image_count = models.IntegerField(blank=True, default=0)
    video_seconds = models.FloatField(blank=True, default=0.0)
    created_date = models.DateTimeField(auto_now_add=True)
    modified_date = models.DateTimeField(auto_now=True)

    # List of module pricing tiers. Any new module added to the platform needs to be
    # added to one of these lists.
    free_modules = ['boonai-extract-layers',
                    'boonai-extract-pages',
                    'standard']
    tier_1_modules = ['boonai-label-detection',
                      'boonai-object-detection',
                      'boonai-text-detection',
                      'boonai-face-detection']
    tier_2_modules = ['gcp-dlp',
                      'gcp-document-text-detection',
                      'gcp-image-text-detection',
                      'gcp-label-detection',
                      'gcp-landmark-detection',
                      'gcp-logo-detection',
                      'gcp-object-detection',
                      'gcp-speech-to-text',
                      'gcp-video-explicit-detection',
                      'gcp-video-label-detection',
                      'gcp-video-logo-detection',
                      'gcp-video-object-detection',
                      'gcp-video-speech-transcription',
                      'gcp-video-text-detection',
                      'clarifai-apparel-detection',
                      'clarifai-celebrity-detection',
                      'clarifai-demographics-detection',
                      'clarifai-face-detection',
                      'clarifai-food-detection',
                      'clarifai-label-detection',
                      'clarifai-logo-detection',
                      'clarifai-nsfw-detection',
                      'clarifai-room-types-detection',
                      'clarifai-texture-detection',
                      'clarifai-travel-detection',
                      'clarifai-unsafe-detection',
                      'clarifai-weapon-detection',
                      'clarifai-wedding-detection',
                      'aws-celebrity-detection',
                      'aws-face-detection',
                      'aws-label-detection',
                      'aws-text-detection',
                      'aws-unsafe-detection',
                      'aws-transcribe',
                      'aws-black-frame-detection',
                      'aws-end-credits-detection',
                      'azure-category-detection',
                      'azure-celebrity-detection',
                      'azure-explicit-detection',
                      'azure-face-detection',
                      'azure-image-description-detection',
                      'azure-label-detection',
                      'azure-landmark-detection',
                      'azure-logo-detection',
                      'azure-object-detection',
                      'azure-text-detection']

    class Meta:
        indexes = [
            models.Index(fields=['service', 'project', 'created_date'])
        ]
