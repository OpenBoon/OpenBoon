__all__ = [
    "ZmlpClientException",
    "ZmlpSecurityException",
    "ZmlpConnectionException",
    "ZmlpWriteException",
    "ZmlpDuplicateException",
    "ZmlpNotFoundException",
    "ZmlpInvalidRequestException"
]


class ZmlpClientException(Exception):
    """The base exception class for all ZmlpClient related Exceptions."""
    pass


class ZmlpRequestException(ZmlpClientException):
    """
    The base exception class for all exceptions thrown from ZMLP.
    """
    def __init__(self, data):
        super(ZmlpClientException, self).__init__(
            data.get("message", "Unknown request exception"))
        self.__data = data

    @property
    def type(self):
        return self.__data["exception"]

    @property
    def cause(self):
        return self.__data["cause"]

    @property
    def endpoint(self):
        return self.__data["path"]

    @property
    def status(self):
        return self.__data["status"]

    def __str__(self):
        return "<ZmlpRequestException msg=%s>" % self.__data["message"]


class ZmlpConnectionException(ZmlpClientException):
    """
    This exception is thrown if the client encounters a connectivity issue
    with the ZMLP API servers..
    """
    pass


class ZmlpWriteException(ZmlpRequestException):
    """
    This exception is thrown the ZMLP fails a write operation.
    """

    def __init__(self, data):
        super(ZmlpWriteException, self).__init__(data)


class ZmlpSecurityException(ZmlpRequestException):
    """
    This exception is thrown if ZMLP fails a security check on the request.
    """

    def __init__(self, data):
        super(ZmlpSecurityException, self).__init__(data)


class ZmlpNotFoundException(ZmlpRequestException):
    """
    This exception is thrown if the ZMLP fails a read operation because
    a piece of named data cannot be found.
    """

    def __init__(self, data):
        super(ZmlpNotFoundException, self).__init__(data)


class ZmlpDuplicateException(ZmlpWriteException):
    """
    This exception is thrown if the ZMLP fails a write operation because
    the newly created element would be a duplicate.
    """

    def __init__(self, data):
        super(ZmlpDuplicateException, self).__init__(data)


class ZmlpInvalidRequestException(ZmlpRequestException):
    """
    This exception is thrown if the request sent to ZMLP is invalid in
    some way, similar to an IllegalArgumentException.
    """

    def __init__(self, data):
        super(ZmlpInvalidRequestException, self).__init__(data)


"""
A map of HTTP response codes to local exception types.
"""
EXCEPTION_MAP = {
    404: ZmlpNotFoundException,
    409: ZmlpDuplicateException,
    500: ZmlpInvalidRequestException,
    400: ZmlpInvalidRequestException,
    401: ZmlpSecurityException,
    403: ZmlpSecurityException
}


def translate(status_code):
    """
    Translate the HTTP status code into one of the exceptions.

    Args:
        status_code (int): the HTTP status code

    Returns:
        Exception: the exception to throw for the given status code
    """
    return EXCEPTION_MAP.get(status_code, ZmlpRequestException)
