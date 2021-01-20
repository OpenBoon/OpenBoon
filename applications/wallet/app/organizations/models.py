from django.contrib.auth import get_user_model
from django.db import models, transaction

from organizations.utils import random_organization_name
from wallet.mixins import TimeStampMixin, UUIDMixin, ActiveMixin

User = get_user_model()


class Plan(models.TextChoices):
    """Choices for the tier field in the Subscription model."""
    ACCESS = 'access'
    BUILD = 'build'


class Organization(UUIDMixin, TimeStampMixin, ActiveMixin):
    """An organization is a collection of projects with an owner. Currently this is only
    used for billing purposes."""
    name = models.CharField(max_length=144, unique=True, default=random_organization_name)
    owner = models.ForeignKey(User, on_delete=models.DO_NOTHING, null=True, blank=True)
    plan = models.CharField(max_length=6, choices=Plan.choices, default=Plan.ACCESS)

    def __str__(self):
        return self.name

    def __repr__(self):
        return f"Organization(name='{self.name}', owner_id={self.owner_id})"

    def save(self, *args, **kwargs):
        with transaction.atomic():
            super(Organization, self).save(*args, **kwargs)
            if not self.isActive:
                for project in self.projects.all():
                    project.isActive = False
                    project.save()

    def get_ml_usage_last_hour(self):
        # TODO: Merge in metrics code and return real numbers
        return {'tier_1_image_count': 1000,
                'tier_1_video_hours': 100,
                'tier_2_image_count': 2000,
                'tier_2_video_hours': 200}
