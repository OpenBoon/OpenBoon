from django.http import Http404
from rest_framework import exceptions, status
from rest_framework.exceptions import APIException
from rest_framework.views import exception_handler
from boonsdk.client import ZmlpSecurityException, ZmlpInvalidRequestException, \
    BoonSdkNotFoundException, BoonSdkDuplicateException


class InvalidRequestError(APIException):
    status_code = status.HTTP_400_BAD_REQUEST
    default_detail = {'detail': ['Invalid request.']}
    default_code = 'invalid_request'


class DuplicateError(APIException):
    status_code = status.HTTP_409_CONFLICT
    default_detail = {'detail': ['Resource already exists.']}
    default_code = 'already_exists'


class InvalidZmlpDataError(APIException):
    status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
    default_detail = {'detail': ['Invalid data was returned from boonsdk.']}
    default_code = 'invalid_zmlp_data'


class NotFoundError(APIException):
    status_code = status.HTTP_404_NOT_FOUND
    default_detail = {'detail': ['Not found.']}
    default_code = 'not_found'


def zmlp_exception_handler(exc, context):
    """Custom DRF exception handler that converts ZMLP exceptions to built-in DRF exceptions."""
    exception_mapping = {ZmlpSecurityException: exceptions.PermissionDenied,
                         ZmlpInvalidRequestException: InvalidRequestError,
                         BoonSdkNotFoundException: NotFoundError,
                         Http404: NotFoundError,
                         BoonSdkDuplicateException: DuplicateError}
    exc_type = type(exc)
    if exc_type in exception_mapping:
        exc = exception_mapping[exc_type]()
    return exception_handler(exc, context)
