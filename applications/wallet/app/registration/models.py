import uuid

from django.conf import settings
from django.contrib.auth.models import AbstractUser
from django.db import models


class UserRegistrationToken(models.Model):
    """Ties a random token to a user to use for verifying new accounts via email."""
    token = models.UUIDField(default=uuid.uuid4, primary_key=True, unique=True)
    created_at = models.DateTimeField(auto_now_add=True)
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)

    def __str__(self):
        return f'{self.user.username}: {self.token}'


class WalletUser(AbstractUser):
    """Extends the built-in Django user and is intended to be used as the default user model."""
    id = models.UUIDField(primary_key=True)
    data = models.TextField(help_text='Arbitrary JSON used by the frontend app.', default='{}')

    def save(self, *args, **kwargs):

        if self.email:
            self.email = self.username
        super(WalletUser, self).save(*args, **kwargs)
