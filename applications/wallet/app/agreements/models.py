import uuid

from django.contrib.auth import get_user_model
from django.db import models

User = get_user_model()


class Agreement(models.Model):
    """Represents a User Agreeing to our Privacy Policy"""

    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    user = models.ForeignKey(User, related_name='agreements', on_delete=models.CASCADE)
    policies_date = models.CharField(max_length=8, default='00000000')
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    created_date = models.DateTimeField(auto_now_add=True)
    modified_date = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f'{self.user} - {self.policies_date}'
