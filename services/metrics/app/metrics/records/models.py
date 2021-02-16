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

    # List of module pricing tiers. Any new module added to the platform needs to be
    # added to one of these lists.
    free_modules = ['zvi-extract-layers',
                    'zvi-extract-pages',
                    'standard']
    tier_1_modules = ['zvi-label-detection',
                      'zvi-object-detection',
                      'zvi-text-detection',
                      'zvi-face-detection']
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
                      'clarifai-face-detection',
                      'clarifai-food-detection',
                      'clarifai-label-detection',
                      'clarifai-logo-detection',
                      'clarifai-nsfw-detection',
                      'clarifai-texture-detection',
                      'clarifai-travel-detection',
                      'clarifai-unsafe-detection',
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
        unique_together = (
            ('service', 'asset_id', 'project')
        )

    def save(self, *args, **kwargs):
        super(ApiCall, self).save(*args, **kwargs)
