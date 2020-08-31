from django.utils.translation import gettext_lazy
from rest_framework import exceptions, status
from rest_framework.exceptions import APIException
from rest_framework.views import exception_handler
from zmlp.client import ZmlpSecurityException, ZmlpInvalidRequestException, \
    ZmlpNotFoundException, ZmlpDuplicateException


class InvalidRequestError(APIException):
    status_code = status.HTTP_400_BAD_REQUEST
    default_detail = gettext_lazy('Invalid request.')
    default_code = 'invalid_request'


class DuplicateError(APIException):
    status_code = status.HTTP_409_CONFLICT
    default_detail = gettext_lazy('Resource already exists.')
    default_code = 'already_exists'


class InvalidZmlpDataError(APIException):
    status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
    default_detail = gettext_lazy('Invalid data was returned from ZMLP.')
    default_code = 'invalid_zmlp_data'


def zmlp_exception_handler(exc, context):
    """Custom DRF exception handler that converts ZMLP exceptions to built-in DRF exceptions."""
    exception_mapping = {ZmlpSecurityException: exceptions.PermissionDenied,
                         ZmlpInvalidRequestException: InvalidRequestError,
                         ZmlpNotFoundException: exceptions.NotFound,
                         ZmlpDuplicateException: DuplicateError}
    exc_type = type(exc)
    if exc_type in exception_mapping:
        exc = exception_mapping[exc_type]()
    return exception_handler(exc, context)
