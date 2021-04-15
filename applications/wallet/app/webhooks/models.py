from django.db import models


class Trigger(models.Model):
    name = models.CharField(max_length=64, unique=True)
    displayName = models.CharField(max_length=64, unique=True)
    description = models.CharField(max_length=255)

    def __str__(self):
        return self.displayName
