import uuid

from django.db import models
from djangorestframework_camel_case.render import CamelCaseBrowsableAPIRenderer, \
    CamelCaseJSONRenderer


class CamelCaseRendererMixin(object):
    """Mixin for DRF viewsets that overrides the parsers and renders so that all camelCase
    parameters sent to the view are converted to snake_case. Helpful for views that interact
    with Wallet models that use snake case.

    NOTE: This mixin needs to come first when defining a class.

    """
    renderer_classes = [CamelCaseJSONRenderer, CamelCaseBrowsableAPIRenderer]


class UUIDMixin(models.Model):
    """Base model mixin that all models should inherit from."""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)

    class Meta:
        abstract = True


class TimeStampMixin(models.Model):
    """Model mixin that add timestamp information."""
    createdDate = models.DateTimeField(auto_now_add=True)
    modifiedDate = models.DateTimeField(auto_now=True)

    class Meta:
        abstract = True
