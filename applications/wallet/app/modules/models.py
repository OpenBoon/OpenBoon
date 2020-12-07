from django.db import models


class Provider(models.Model):
    """Provides display details for pipeline module providers."""
    name = models.CharField(max_length=255, unique=True)
    description = models.TextField()
    logo_data_uri = models.TextField(
        help_text='Paste in the base64 data uri text string of an image. '
                  'You can use https://www.base64-image.de/ to convert '
                  'images, use the "copy image" button after converting '
                  'and paste results above. If you have an svg you can use '
                  'https://base64.guru/converter/encode/image/svg')

    def __str__(self):
        return self.name
