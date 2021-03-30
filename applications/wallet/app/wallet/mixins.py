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


class ActiveManager(models.Manager):
    """Model manager that only returns objects that are active."""
    def get_queryset(self):
        return super(ActiveManager, self).get_queryset().filter(isActive=True)


class ActiveMixin(models.Model):
    """Model mixin that adds the isActive field. Also overrides the default model manager
    to only show active objects.

    """
    all_objects = models.Manager()
    objects = ActiveManager()
    isActive = models.BooleanField(default=True)

    class Meta:
        abstract = True


class BoonAISortArgsMixin():
    """Converts Django style ordering arguments into BoonAI sort query args."""

    def get_boonai_sort_args(self, request):
        sort = request.query_params.get('ordering')
        if sort:
            fields = sort.split(',')
            sort_args = []
            for field in fields:
                cleaned_field = field.lstrip('-')
                if field == cleaned_field:
                    sort_args.append(f'{field}:a')
                else:
                    sort_args.append(f'{cleaned_field}:d')
            return sort_args
        return None
