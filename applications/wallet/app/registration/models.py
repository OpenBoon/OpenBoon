import uuid

from django.conf import settings
from django.db import models


class UserRegistrationToken(models.Model):
    """Ties a random token to a user to use for verifying new accounts via email."""
    token = models.UUIDField(default=uuid.uuid4, primary_key=True, unique=True)
    createdAt = models.DateTimeField(auto_now_add=True)
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)

    def __str__(self):
        return f'{self.user.username}: {self.token}'
