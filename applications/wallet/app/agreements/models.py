import uuid

from django.contrib.auth import get_user_model
from django.db import models

from wallet.mixins import UUIDMixin, TimeStampMixin

User = get_user_model()


class Agreement(UUIDMixin, TimeStampMixin):
    """Represents a User Agreeing to our Privacy Policy"""

    user = models.ForeignKey(User, related_name='agreements', on_delete=models.CASCADE)
    policiesDate = models.CharField(max_length=8, default='00000000')
    ipAddress = models.GenericIPAddressField(null=True, blank=True)

    def __str__(self):
        return f'{self.user} - {self.policiesDate}'
