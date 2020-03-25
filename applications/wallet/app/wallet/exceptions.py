from django.utils.translation import gettext_lazy
from rest_framework import exceptions, status
from rest_framework.exceptions import APIException
from rest_framework.views import exception_handler
from zmlp.client import ZmlpSecurityException, ZmlpInvalidRequestException


class InvalidRequestError(APIException):
    status_code = status.HTTP_400_BAD_REQUEST
    default_detail = gettext_lazy('Invalid request.')
    default_code = 'invalid_request'


def zmlp_exception_handler(exc, context):
    """Custom DRF exception handler that converts ZMLP exceptions to built-in DRF exceptions."""
    exception_mapping = {ZmlpSecurityException: exceptions.PermissionDenied,
                         ZmlpInvalidRequestException: InvalidRequestError}
    exc_type = type(exc)
    if exc_type in exception_mapping:
        exc = exception_mapping[exc_type]()
    return exception_handler(exc, context)
